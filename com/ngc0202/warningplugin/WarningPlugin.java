package com.ngc0202.warningplugin;

import java.io.File;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author ngc0202
 */
public class WarningPlugin extends JavaPlugin {

    final WarningPlugin plugin = this;
    PropertiesFile warns;
    PropertiesFile bans;

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();
        File warnFile = new File(getDataFolder(), "warns.properties");
        File banFile = new File(getDataFolder(), "bans.properties");
        warns = new PropertiesFile(warnFile.getAbsolutePath());
        warns.load();
        bans = new PropertiesFile(banFile.getAbsolutePath());
        bans.load();
        this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {

            public void run() {
                plugin.banCheckAll();
            }
        }, 1L, 6000L);
        System.out.println(getDescription().getFullName() + " by ngc0202 is activated.");
    }

    @Override
    public void onDisable() {
        System.out.println(getDescription().getName() + " is deactivated.");
        warns.save();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        warns.load();
        if (commandLabel.equalsIgnoreCase("warn")) {
            if (args.length == 0) {
                return false;
            }
            if (sender.hasPermission("WarningPlugin.warn")) {
                sender.sendMessage(ChatColor.RED + this.getCommand("warn").getPermissionMessage());
                return true;
            }
            Player ply;
            if (args[0].equalsIgnoreCase("remove")) {
                if (!((args.length == 2) || (args.length == 3))) {
                    return false;
                }
                ply = sender.getServer().getPlayer(args[1]);
                if (ply == null) {
                    sender.sendMessage(ChatColor.RED + "Couldn't find " + args[1] + ".");
                    return false;
                }
                int remCnt;
                if (args.length == 2) {
                    remCnt = 1;
                } else {
                    remCnt = Integer.parseInt(args[2]);
                }
                if (remCnt < 1) {
                    sender.sendMessage(ChatColor.RED + "Invalid number.");
                    return true;
                }
                if (!warns.keyExists(ply.getName())) {
                    sender.sendMessage(ChatColor.BLUE + ply.getName() + " already has no warnings.");
                    return true;
                }
                int newCnt = warns.getInt(ply.getName()) - remCnt;
                if (newCnt < 0) {
                    newCnt = 0;
                }
                warns.setInt(ply.getName(), newCnt);
                warns.save();
                sender.sendMessage(ChatColor.BLUE + ply.getName() + " now has " + newCnt + " warnings.");
                return true;
            } else {
                if (args.length == 0) {
                    return false;
                }
                ply = sender.getServer().getPlayer(args[0]);
                if (ply == null) {
                    sender.sendMessage(ChatColor.RED + "Couldn't find " + args[0] + ".");
                    return false;
                }
                if (args.length == 1) {
                    sender.sendMessage(ChatColor.RED + "Please give a reason.");
                    return false;
                }
                int newCnt = warns.getInt(ply.getName()) + 1;
                warns.setInt(ply.getName(), newCnt);
                warns.save();
                String reason = this.buildReason(args);
                ply.sendMessage(ChatColor.RED + "You've been warned for: \"" + reason + "\"");
                if (newCnt == 4) {
                    bans.setLong(ply.getName(), System.currentTimeMillis() + 3600000L);
                    ply.setBanned(true);
                    ply.kickPlayer("Banned for having too many warnings. (1 hour)");
                } else if (newCnt >= 10) {
                    bans.removeKey(ply.getName());
                    ply.setBanned(true);
                    ply.kickPlayer("Banned for having too many warnings.");
                }
                return true;
            }
        } else if (commandLabel.equalsIgnoreCase("warns")) {
            if (args.length == 0) {
                int warnings = warns.getInt(sender.getName());
                sender.sendMessage(ChatColor.BLUE + "You currently have " + warnings + " warnings.");
                return true;
            } else if (args.length == 1) {
                if (!sender.hasPermission("WarningPlugin.warns")) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission to do that.");
                    return true;
                }
                Player ply = getServer().getPlayer(args[0]);
                int warnings = warns.getInt(ply.getName());
                sender.sendMessage(ChatColor.BLUE + ply.getName() + " now has " + warnings + " warnings.");
            } else {
                return false;
            }
        }
        return false;
    }

    private void banCheckAll() {
        try {
            bans.load();
            Map<String, String> map = bans.returnMap();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String key = entry.getKey();
                String val = entry.getValue();
                if (Long.parseLong(val) <= System.currentTimeMillis()) {
                    OfflinePlayer ply = getServer().getOfflinePlayer(key);
                    ply.setBanned(false);
                    bans.removeKey(key);
                }
            }
        } catch (Exception ex) {
            System.out.println(ChatColor.RED + "Unable to load the WarningPlugin ban list.");
        }
    }

    private String buildReason(String[] args) {
        String reason = "";
        int len = args.length;
        for (int i = 1; i < len; i++) {
            reason += args[i];
            if (i < (len - 1)) {
                reason += " ";
            }
        }
        return reason;
    }
}
