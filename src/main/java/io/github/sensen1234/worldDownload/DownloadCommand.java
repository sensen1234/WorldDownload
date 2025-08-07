package io.github.sensen1234.worldDownload;

import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DownloadCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        WorldDownloadPlugin plugin = WorldDownloadPlugin.getInstance();

        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length != 1) {
            player.sendMessage(plugin.getMessage("usage"));
            player.sendMessage(plugin.getMessage("permission-hint"));
            showAvailableWorlds(player);
            return true;
        }

        String worldName = args[0];

        // 权限检查
        if (!hasDownloadPermission(player, worldName)) {
            String noPermMsg = plugin.getMessage("no-permission").replace("{world}", worldName);
            String permReqMsg = plugin.getMessage("permission-required").replace("{world}", worldName);

            player.sendMessage(noPermMsg);
            player.sendMessage(permReqMsg);
            player.sendMessage(plugin.getMessage("wildcard-permission"));
            return true;
        }

        // 检查世界是否存在
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            String notExistMsg = plugin.getMessage("world-not-exist").replace("{world}", worldName);
            player.sendMessage(notExistMsg);
            return true;
        }

        String startMsg = plugin.getMessage("start-download").replace("{world}", worldName);
        player.sendMessage(startMsg);

        // 异步处理上传
        WorldUploader.uploadWorld(player, worldName, world.getWorldFolder());

        return true;
    }

    private boolean hasDownloadPermission(Player player, String worldName) {
        String specificPermission = "worlddownload.download." + worldName;
        String wildcardPermission = "worlddownload.download.*";

        try {
            return player.hasPermission(specificPermission) ||
                    player.hasPermission(wildcardPermission) ||
                    player.isOp();
        } catch (Exception e) {
            return player.isOp();
        }
    }

    private void showAvailableWorlds(Player player) {
        WorldDownloadPlugin plugin = WorldDownloadPlugin.getInstance();
        player.sendMessage(plugin.getMessage("available-worlds"));

        for (World world : plugin.getServer().getWorlds()) {
            String worldName = world.getName();
            if (hasDownloadPermission(player, worldName)) {
                String itemMsg = plugin.getMessage("world-list-item").replace("{world}", worldName);
                player.sendMessage(itemMsg);
            } else {
                String itemMsg = plugin.getMessage("world-list-item-no-perm").replace("{world}", worldName);
                player.sendMessage(itemMsg);
            }
        }
    }
}
