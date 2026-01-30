package com.chagui68.singleip.commands;

import com.chagui68.singleip.SingleIPPlugin;
import com.chagui68.singleip.storage.IPDataManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;

public class IPCommand implements CommandExecutor, TabCompleter {

    private final SingleIPPlugin plugin;
    private final IPDataManager dataManager;

    public IPCommand(SingleIPPlugin plugin, IPDataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            plugin.getLogger().warning("Unauthorized access attempt to ipmanager by: " + sender.getName());
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "add":
                return handleAdd(sender, args);
            case "remove":
                return handleRemove(sender, args);
            case "list":
                return handleList(sender, args);
            case "info":
                return handleInfo(sender, args);
            case "reset":
                return handleReset(sender, args);
            case "reload":
                return handleReload(sender, args);
            case "failed":
                return handleFailedLogins(sender, args);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleAdd(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sender.sendMessage(ChatColor.RED + "Usage: ipmanager add <player> <ip>");
            return true;
        }

        String playerName = args[1];
        String ip = args[2];

        if (!dataManager.isValidIP(ip)) {
            sender.sendMessage(ChatColor.RED + "✖ Invalid IP format: " + ip);
            sender.sendMessage(ChatColor.YELLOW + "Valid formats: IPv4 (192.168.1.1) or IPv6");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        UUID uuid = target.getUniqueId();

        if (!dataManager.hasRegisteredIP(uuid)) {
            sender.sendMessage(ChatColor.RED + "✖ Player not found in database.");
            sender.sendMessage(ChatColor.YELLOW + "The player must connect at least once first.");
            return true;
        }

        int maxIPs = plugin.getConfig().getInt("max-ips-per-player", 2);
        if (dataManager.getIPCount(uuid) >= maxIPs) {
            sender.sendMessage(ChatColor.RED + "✖ This player already has " + maxIPs + " IPs registered.");
            sender.sendMessage(ChatColor.YELLOW + "Use 'ipmanager remove " + playerName + " <ip>' first.");
            return true;
        }

        List<String> currentIPs = dataManager.getRegisteredIPs(uuid);
        if (currentIPs.contains(ip)) {
            sender.sendMessage(ChatColor.RED + "✖ This IP is already registered for " + playerName);
            return true;
        }

        // El nombre se mantiene por vagancia aunque podria colocar que sea Ips en vez de secondIp ( No quise cambiar eso ) :V
        if (dataManager.addSecondIP(uuid, ip)) {
            sender.sendMessage(ChatColor.GREEN + "✓ IP added successfully");
            sender.sendMessage(ChatColor.GRAY + "Player: " + ChatColor.WHITE + playerName);
            sender.sendMessage(ChatColor.GRAY + "IP added: " + ChatColor.WHITE + ip);
            sender.sendMessage(ChatColor.GRAY + "Total IPs: " + ChatColor.WHITE + dataManager.getIPCount(uuid) + "/" + maxIPs);
        } else {
            sender.sendMessage(ChatColor.RED + "✖ Error adding IP. Please try again.");
        }

        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sender.sendMessage(ChatColor.RED + "Usage: ipmanager remove <player> <ip>");
            return true;
        }

        String playerName = args[1];
        String ip = args[2];

        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        UUID uuid = target.getUniqueId();

        if (!dataManager.hasRegisteredIP(uuid)) {
            sender.sendMessage(ChatColor.RED + "✖ Player not found in database.");
            return true;
        }

        List<String> currentIPs = dataManager.getRegisteredIPs(uuid);
        if (currentIPs.size() == 1) {
            sender.sendMessage(ChatColor.RED + "✖ You cannot remove the only IP for this player.");
            sender.sendMessage(ChatColor.YELLOW + "Use 'ipmanager reset " + playerName + "' to remove all IPs.");
            return true;
        }

        if (dataManager.removeIP(uuid, ip)) {
            sender.sendMessage(ChatColor.GREEN + "✓ IP removed successfully");
            sender.sendMessage(ChatColor.GRAY + "Player: " + ChatColor.WHITE + playerName);
            sender.sendMessage(ChatColor.GRAY + "IP removed: " + ChatColor.WHITE + ip);
            sender.sendMessage(ChatColor.GRAY + "IPs remaining: " + ChatColor.WHITE + dataManager.getIPCount(uuid));
        } else {
            sender.sendMessage(ChatColor.RED + "✖ IP not found for this player.");
            sender.sendMessage(ChatColor.YELLOW + "Current IPs:");
            for (String currentIP : currentIPs) {
                sender.sendMessage(ChatColor.GRAY + "  - " + currentIP);
            }
        }

        return true;
    }

    private boolean handleList(CommandSender sender, String[] args) {
        Set<UUID> players = dataManager.getAllPlayers();

        if (players.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "⚠ No players registered in the database.");
            return true;
        }

        sender.sendMessage(ChatColor.GREEN + "═════════════════════════════════════");
        sender.sendMessage(ChatColor.GREEN + "   Player IP List");
        sender.sendMessage(ChatColor.GREEN + "═════════════════════════════════════");

        int count = 0;
        for (UUID uuid : players) {
            count++;
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            List<String> ips = dataManager.getRegisteredIPs(uuid);

            sender.sendMessage("");
            sender.sendMessage(ChatColor.AQUA + "" + count + ". " + player.getName() +
                    ChatColor.GRAY + " (" + ips.size() + " IP" + (ips.size() > 1 ? "s" : "") + ")");
            sender.sendMessage(ChatColor.DARK_GRAY + "   UUID: " + uuid.toString());

            for (int i = 0; i < ips.size(); i++) {
                String type = i == 0 ? ChatColor.YELLOW + "[Primary]" : ChatColor.AQUA + "[Secondary]";
                sender.sendMessage(ChatColor.GRAY + "   " + type + " " + ChatColor.WHITE + ips.get(i));
            }
        }

        sender.sendMessage("");
        sender.sendMessage(ChatColor.GREEN + "═════════════════════════════════════");
        sender.sendMessage(ChatColor.GRAY + "Total players: " + ChatColor.WHITE + count);

        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Usage: ipmanager info <player>");
            return true;
        }

        String playerName = args[1];
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        UUID uuid = target.getUniqueId();

        if (!dataManager.hasRegisteredIP(uuid)) {
            sender.sendMessage(ChatColor.RED + "✖ Player not found in database.");
            sender.sendMessage(ChatColor.YELLOW + "The player must connect at least once.");
            return true;
        }

        List<String> ips = dataManager.getRegisteredIPs(uuid);
        int maxIPs = plugin.getConfig().getInt("max-ips-per-player", 2);

        sender.sendMessage(ChatColor.GREEN + "═════════════════════════════════════");
        sender.sendMessage(ChatColor.GREEN + "   Information for " + playerName);
        sender.sendMessage(ChatColor.GREEN + "═════════════════════════════════════");
        sender.sendMessage(ChatColor.GRAY + "UUID: " + ChatColor.WHITE + uuid.toString());
        sender.sendMessage(ChatColor.GRAY + "Registered IPs: " + ChatColor.WHITE + ips.size() + "/" + maxIPs);
        sender.sendMessage("");

        for (int i = 0; i < ips.size(); i++) {
            String type = i == 0 ? ChatColor.YELLOW + "[Primary]" : ChatColor.AQUA + "[Secondary]";
            sender.sendMessage(type + " " + ChatColor.WHITE + ips.get(i));
        }

        sender.sendMessage(ChatColor.GREEN + "═════════════════════════════════════");

        return true;
    }

    private boolean handleReset(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Usage: ipmanager reset <player>");
            return true;
        }

        String playerName = args[1];
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        UUID uuid = target.getUniqueId();

        if (!dataManager.hasRegisteredIP(uuid)) {
            sender.sendMessage(ChatColor.RED + "✖ Player not found in database.");
            return true;
        }

        List<String> removedIPs = dataManager.getRegisteredIPs(uuid);
        dataManager.removeAllIPs(uuid);

        sender.sendMessage(ChatColor.GREEN + "✓ All IPs for " + playerName + " have been removed.");
        sender.sendMessage(ChatColor.YELLOW + "Removed IPs:");
        for (String ip : removedIPs) {
            sender.sendMessage(ChatColor.GRAY + "  - " + ip);
        }
        sender.sendMessage(ChatColor.GRAY + "The player must reconnect to register a new IP.");

        return true;
    }

    private boolean handleReload(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.YELLOW + "Reloading player IP data...");

        try {
            dataManager.loadData();
            sender.sendMessage(ChatColor.GREEN + "✓ Player IP data reloaded successfully!");
            sender.sendMessage(ChatColor.GRAY + "Total players in database: " +
                    ChatColor.WHITE + dataManager.getStoredPlayerCount());
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "✖ Error reloading data: " + e.getMessage());
            plugin.getLogger().severe("Error reloading player IPs: " + e.getMessage());
        }

        return true;
    }

    private boolean handleFailedLogins(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // Mostrar últimos 10 intentos fallidos globales
            List<Map<String, String>> attempts = dataManager.getFailedLogins(10);

            if (attempts.isEmpty()) {
                sender.sendMessage(ChatColor.GREEN + "✓ No failed login attempts recorded.");
                return true;
            }

            sender.sendMessage(ChatColor.RED + "═════════════════════════════════════");
            sender.sendMessage(ChatColor.RED + "   Failed Login Attempts (Last 10)");
            sender.sendMessage(ChatColor.RED + "═════════════════════════════════════");

            for (Map<String, String> attempt : attempts) {
                sender.sendMessage("");
                sender.sendMessage(ChatColor.YELLOW + "Player: " + ChatColor.WHITE + attempt.get("player"));
                sender.sendMessage(ChatColor.GRAY + "  IP: " + ChatColor.WHITE + attempt.get("attempted-ip"));
                sender.sendMessage(ChatColor.GRAY + "  Reason: " + ChatColor.WHITE + attempt.get("reason"));
                sender.sendMessage(ChatColor.GRAY + "  Time: " + ChatColor.WHITE + attempt.get("timestamp"));
            }

            sender.sendMessage("");
            sender.sendMessage(ChatColor.RED + "═════════════════════════════════════");
            return true;
        }

        if (args.length == 2) {
            // Mostrar intentos fallidos de un jugador específico
            String playerName = args[1];
            OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
            UUID uuid = target.getUniqueId();

            List<Map<String, String>> attempts = dataManager.getFailedLoginsByPlayer(uuid, 10);

            if (attempts.isEmpty()) {
                sender.sendMessage(ChatColor.GREEN + "✓ No failed login attempts for " + playerName);
                return true;
            }

            sender.sendMessage(ChatColor.RED + "═════════════════════════════════════");
            sender.sendMessage(ChatColor.RED + "   Failed Logins: " + playerName);
            sender.sendMessage(ChatColor.RED + "═════════════════════════════════════");

            for (Map<String, String> attempt : attempts) {
                sender.sendMessage("");
                sender.sendMessage(ChatColor.GRAY + "  IP: " + ChatColor.WHITE + attempt.get("attempted-ip"));
                sender.sendMessage(ChatColor.GRAY + "  Reason: " + ChatColor.WHITE + attempt.get("reason"));
                sender.sendMessage(ChatColor.GRAY + "  Time: " + ChatColor.WHITE + attempt.get("timestamp"));
            }

            sender.sendMessage("");
            sender.sendMessage(ChatColor.RED + "═════════════════════════════════════");
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Usage: ipmanager failed [player]");
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "═════════════════════════════════════");
        sender.sendMessage(ChatColor.GREEN + "   SingleIPPlugin - Commands");
        sender.sendMessage(ChatColor.GRAY + "   By Chagui68");
        sender.sendMessage(ChatColor.GREEN + "═════════════════════════════════════");
        sender.sendMessage(ChatColor.YELLOW + "ipmanager add <player> <ip>");
        sender.sendMessage(ChatColor.GRAY + "  → Add second IP for a player");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + "ipmanager remove <player> <ip>");
        sender.sendMessage(ChatColor.GRAY + "  → Remove a specific IP");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + "ipmanager list");
        sender.sendMessage(ChatColor.GRAY + "  → View all registered players");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + "ipmanager info <player>");
        sender.sendMessage(ChatColor.GRAY + "  → View player IP details");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + "ipmanager reset <player>");
        sender.sendMessage(ChatColor.GRAY + "  → Remove all IPs for a player");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + "ipmanager reload");
        sender.sendMessage(ChatColor.GRAY + "  → Reload player IPs from file");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + "ipmanager failed [player]");
        sender.sendMessage(ChatColor.GRAY + "  → View failed login attempts");
        sender.sendMessage(ChatColor.GREEN + "═════════════════════════════════════");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof ConsoleCommandSender)) {
            return new ArrayList<>();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("add", "remove", "list", "info", "reset", "reload", "failed"));
        } else if (args.length == 2 && !args[0].equalsIgnoreCase("list") && !args[0].equalsIgnoreCase("reload")) {
            for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                if (player.getName() != null) {
                    completions.add(player.getName());
                }
            }
        }

        return completions;
    }
}
