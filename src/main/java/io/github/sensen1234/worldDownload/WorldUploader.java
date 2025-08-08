package io.github.sensen1234.worldDownload;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class WorldUploader {

    public static void uploadWorld(Player player, String worldName, File worldFolder) {
        WorldDownloadPlugin plugin = WorldDownloadPlugin.getInstance();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String startMsg = plugin.getMessage("start-download").replace("{world}", worldName);
                player.sendMessage(startMsg);

                // 生成UUID
                String uuid = UUID.randomUUID().toString();

                // 创建临时ZIP文件
                File tempZip = new File(plugin.getTempFolder(), uuid + ".zip");

                player.sendMessage(plugin.getMessage("compressing"));
                // 压缩世界文件夹（只包含level.dat、region和entities文件夹）
                zipWorldFolder(worldFolder, tempZip, player);

                // 检查文件大小
                long maxSize = plugin.getMaxFileSize();
                if (tempZip.length() > maxSize) {
                    String size = String.valueOf(maxSize / 1024 / 1024);
                    String tooLargeMsg = plugin.getMessage("file-too-large").replace("{size}", size);
                    player.sendMessage(tooLargeMsg);
                    tempZip.delete();
                    return;
                }

                String sizeMB = String.valueOf(tempZip.length() / 1024 / 1024);
                String uploadingMsg = plugin.getMessage("uploading").replace("{size}", sizeMB);
                player.sendMessage(uploadingMsg);

                // 检查是否启用限速
                long speedLimit = plugin.getUploadSpeedLimit();
                if (speedLimit > 0) {
                    String speedMsg = plugin.getMessage("upload-speed-limit").replace("{speed}", String.valueOf(speedLimit / 1024));
                    player.sendMessage(speedMsg);
                }

                // 上传文件
                String downloadUrl = uploadFileToLegacyFawe(tempZip, uuid, player);

                if (downloadUrl != null && !downloadUrl.isEmpty()) {
                    player.sendMessage(plugin.getMessage("upload-success"));
                    String linkMsg = plugin.getMessage("download-link").replace("{url}", downloadUrl);
                    player.sendMessage(linkMsg);
                } else {
                    player.sendMessage(plugin.getMessage("upload-failed"));
                }

                // 清理临时文件
                tempZip.delete();
                player.sendMessage(plugin.getMessage("temp-file-cleanup"));

            } catch (Exception e) {
                String errorMsg = plugin.getMessage("upload-error").replace("{error}", e.getMessage());
                player.sendMessage(errorMsg);
                e.printStackTrace();
            }
        });
    }

    private static void zipWorldFolder(File sourceFolder, File zipFile, Player player) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(fos)) {

            // 添加指定的文件和文件夹
            addSpecificFilesToZip(sourceFolder, sourceFolder.getName(), zos, player);
        }
    }

    private static void addSpecificFilesToZip(File folder, String parentFolder, java.util.zip.ZipOutputStream zos, Player player) throws IOException {
        // 添加level.dat文件（如果存在）
        File levelDat = new File(folder, "level.dat");
        if (levelDat.exists()) {
            String zipEntryName = parentFolder + "/level.dat";
            try (FileInputStream fis = new FileInputStream(levelDat)) {
                java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry(zipEntryName);
                zos.putNextEntry(zipEntry);

                byte[] buffer = new byte[1024];
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                }
                zos.closeEntry();
                player.sendMessage("§7已添加: level.dat");
            }
        }

        // 添加region文件夹（如果存在）
        File regionFolder = new File(folder, "region");
        if (regionFolder.exists() && regionFolder.isDirectory()) {
            addRegionFolderToZip(regionFolder, parentFolder + "/region", zos, player);
        }

        // 添加entities文件夹（如果存在）
        File entitiesFolder = new File(folder, "entities");
        if (entitiesFolder.exists() && entitiesFolder.isDirectory()) {
            addEntitiesFolderToZip(entitiesFolder, parentFolder + "/entities", zos, player);
        }
    }

    private static void addRegionFolderToZip(File regionFolder, String parentFolder, java.util.zip.ZipOutputStream zos, Player player) throws IOException {
        File[] files = regionFolder.listFiles();
        if (files == null) return;

        player.sendMessage("§7正在添加region文件夹...");
        int fileCount = 0;

        for (File file : files) {
            if (file.isFile()) {
                // 只添加.mca和.mcr文件（region文件）
                String fileName = file.getName();
                if (fileName.endsWith(".mca") || fileName.endsWith(".mcr")) {
                    String zipEntryName = parentFolder + "/" + fileName;
                    try (FileInputStream fis = new FileInputStream(file)) {
                        java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry(zipEntryName);
                        zos.putNextEntry(zipEntry);

                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = fis.read(buffer)) > 0) {
                            zos.write(buffer, 0, length);
                        }
                        zos.closeEntry();
                        fileCount++;
                    }
                }
            }
        }
        player.sendMessage("§7已添加 " + fileCount + " 个region文件");
    }

    private static void addEntitiesFolderToZip(File entitiesFolder, String parentFolder, java.util.zip.ZipOutputStream zos, Player player) throws IOException {
        File[] files = entitiesFolder.listFiles();
        if (files == null) return;

        player.sendMessage("§7正在添加entities文件夹...");
        int fileCount = 0;

        for (File file : files) {
            if (file.isFile()) {
                // 只添加.mca和.mcr文件（entities文件）
                String fileName = file.getName();
                if (fileName.endsWith(".mca") || fileName.endsWith(".mcr")) {
                    String zipEntryName = parentFolder + "/" + fileName;
                    try (FileInputStream fis = new FileInputStream(file)) {
                        java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry(zipEntryName);
                        zos.putNextEntry(zipEntry);

                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = fis.read(buffer)) > 0) {
                            zos.write(buffer, 0, length);
                        }
                        zos.closeEntry();
                        fileCount++;
                    }
                }
            }
        }
        player.sendMessage("§7已添加 " + fileCount + " 个entities文件");
    }

    private static String uploadFileToLegacyFawe(File file, String uuid, Player player) {
        try {
            WorldDownloadPlugin plugin = WorldDownloadPlugin.getInstance();
            String baseUrl = plugin.getUploadBaseUrl();
            String uploadUrl = baseUrl.endsWith("/") ? baseUrl + "upload.php" : baseUrl + "/upload.php";
            String fullUrl = uploadUrl + "?" + uuid;

            String progressMsg = plugin.getMessage("upload-progress").replace("{url}", fullUrl);
            player.sendMessage(progressMsg);

            URL url = new URL(fullUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // 设置连接和读取超时
            int timeout = plugin.getUploadTimeout();
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);

            // 设置请求头
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setInstanceFollowRedirects(true);

            // 设置User-Agent和其他必要的头部
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
            connection.setRequestProperty("Connection", "keep-alive");
            connection.setRequestProperty("Upgrade-Insecure-Requests", "1");

            // 生成boundary
            String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            // 获取限速设置
            long speedLimit = plugin.getUploadSpeedLimit(); // 字节/秒
            long chunkSize = 8192; // 默认块大小
            long delayMs = 0; // 延迟毫秒数

            if (speedLimit > 0) {
                // 计算每块数据需要的延迟时间
                delayMs = (chunkSize * 1000) / speedLimit;
                if (delayMs < 1) delayMs = 1;
            }

            // 构建multipart/form-data内容
            try (DataOutputStream dos = new DataOutputStream(connection.getOutputStream())) {
                // 写入multipart头部
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"schematicFile\"; filename=\"" + uuid + ".zip\"\r\n");
                dos.writeBytes("Content-Type: application/zip\r\n\r\n");

                // 读取文件并写入（带限速）
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[(int) chunkSize];
                    int bytesRead;
                    long totalBytes = file.length();
                    long uploadedBytes = 0;
                    long lastProgressUpdate = System.currentTimeMillis();

                    while ((bytesRead = fis.read(buffer)) != -1) {
                        dos.write(buffer, 0, bytesRead);
                        uploadedBytes += bytesRead;

                        // 限速控制
                        if (speedLimit > 0 && delayMs > 0) {
                            try {
                                Thread.sleep(delayMs);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }

                        // 每2秒更新一次进度
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastProgressUpdate > 2000) {
                            int progress = (int) ((uploadedBytes * 100) / totalBytes);
                            String progressMsgDetail = plugin.getMessage("upload-progress-detail")
                                    .replace("{progress}", String.valueOf(progress))
                                    .replace("{uploaded}", String.valueOf(uploadedBytes / 1024 / 1024))
                                    .replace("{total}", String.valueOf(totalBytes / 1024 / 1024));
                            player.sendMessage(progressMsgDetail);
                            lastProgressUpdate = currentTime;
                        }
                    }
                }

                dos.writeBytes("\r\n--" + boundary + "--\r\n");
            }

            // 获取响应
            int responseCode = connection.getResponseCode();
            String responseMsg = plugin.getMessage("response-code").replace("{code}", String.valueOf(responseCode));
            player.sendMessage(responseMsg);

            String finalUrl = null;

            // 处理响应
            if (responseCode == 302) {
                // 获取重定向URL
                String redirectUrl = connection.getHeaderField("Location");
                String redirectMsg = plugin.getMessage("redirect-url").replace("{url}", redirectUrl);
                player.sendMessage(redirectMsg);
                finalUrl = redirectUrl;
            } else if (responseCode == 200) {
                // 读取响应内容
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line).append("\n");
                    }
                }

                // 根据响应判断是否成功
                if (response.toString().contains("Success") || response.toString().contains("success")) {
                    String baseUrlClean = plugin.getUploadBaseUrl();
                    if (baseUrlClean.endsWith("/")) {
                        baseUrlClean = baseUrlClean.substring(0, baseUrlClean.length() - 1);
                    }
                    finalUrl = baseUrlClean + "/?key=" + uuid + "&type=zip";
                }
            } else {
                // 读取错误响应
                StringBuilder errorResponse = new StringBuilder();
                try {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            errorResponse.append(line).append("\n");
                        }
                    }
                } catch (Exception ignored) {}
            }

            // 如果没有获取到URL，构造默认URL
            if (finalUrl == null || finalUrl.isEmpty()) {
                String baseUrlClean = plugin.getUploadBaseUrl();
                if (baseUrlClean.endsWith("/")) {
                    baseUrlClean = baseUrlClean.substring(0, baseUrlClean.length() - 1);
                }
                finalUrl = baseUrlClean + "/?key=" + uuid + "&type=zip";
                String defaultUrlMsg = plugin.getMessage("default-download-url").replace("{url}", finalUrl);
                player.sendMessage(defaultUrlMsg);
            }

            return finalUrl;

        } catch (Exception e) {
            WorldDownloadPlugin plugin = WorldDownloadPlugin.getInstance();
            String errorMsg = plugin.getMessage("upload-error").replace("{error}", e.getMessage());
            player.sendMessage(errorMsg);
            e.printStackTrace();
            return null;
        }
    }
}
