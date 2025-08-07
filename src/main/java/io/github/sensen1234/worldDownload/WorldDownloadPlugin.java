package io.github.sensen1234.worldDownload;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;

public class WorldDownloadPlugin extends JavaPlugin {
    private static WorldDownloadPlugin instance;
    private File tempFolder;

    @Override
    public void onEnable() {
        instance = this;

        // 保存默认配置
        saveDefaultConfig();

        // 创建临时文件夹
        tempFolder = new File(getDataFolder(), getConfig().getString("temp-folder", "temp_world_downloads"));
        if (!tempFolder.exists()) {
            tempFolder.mkdirs();
        }

        // 注册命令
        getCommand("worlddownload").setExecutor(new DownloadCommand());

        getLogger().info(getMessage("plugin-enabled"));
    }

    @Override
    public void onDisable() {
        // 清理临时文件
        if (tempFolder != null && tempFolder.exists()) {
            deleteDirectory(tempFolder);
        }
        getLogger().info(getMessage("plugin-disabled"));
    }

    public static WorldDownloadPlugin getInstance() {
        return instance;
    }

    public File getTempFolder() {
        return tempFolder;
    }

    private boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    public String getUploadBaseUrl() {
        return getConfig().getString("upload-base-url", "https://www.sencraft.top/fawe");
    }

    public int getUploadTimeout() {
        return getConfig().getInt("upload-timeout", 300000); // 默认5分钟
    }

    public long getMaxFileSize() {
        return getConfig().getLong("max-file-size", 1073741824L); // 默认1GB
    }

    // 获取上传限速设置 (字节/秒)
    public long getUploadSpeedLimit() {
        return getConfig().getLong("upload-speed-limit", 0); // 0表示不限速
    }

    // 获取配置中的消息文本
    public String getMessage(String key) {
        return getConfig().getString("messages." + key, "§c消息未配置: " + key);
    }
}
