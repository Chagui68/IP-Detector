package com.chagui68.singleip.listeners;

import com.chagui68.singleip.SingleIPPlugin;
import com.chagui68.singleip.storage.IPDataManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.net.InetAddress;
import java.util.List;
import java.util.UUID;

public class PlayerConnectionListener implements Listener {

    private final SingleIPPlugin plugin;
    private final IPDataManager dataManager;

    public PlayerConnectionListener(SingleIPPlugin plugin, IPDataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        InetAddress address = event.getAddress();
        String playerIP = address.getHostAddress();
        String playerName = event.getName();

        if (!dataManager.hasRegisteredIP(uuid)) {
            dataManager.registerFirstIP(uuid, playerIP);
            plugin.getLogger().info("First IP registered for " + playerName + ": " + playerIP);
            return;
        }

        if (!dataManager.isIPAllowed(uuid, playerIP)) {
            String kickMessage = plugin.getConfig().getString("kick-message",
                    ChatColor.RED + "✖ Unauthorized IP address\n" +
                            ChatColor.GRAY + "This IP is not registered for your account\n" +
                            ChatColor.YELLOW + "Contact an administrator if you need help");

            int maxIPs = plugin.getConfig().getInt("max-ips-per-player", 2);
            List<String> registeredIPs = dataManager.getRegisteredIPs(uuid);

            String reason = "IP not registered (" + registeredIPs.size() + "/" + maxIPs + " IPs used)";
            dataManager.logFailedLogin(playerName, uuid, playerIP, reason);

            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, kickMessage);
            plugin.getLogger().warning("Connection denied - Player: " + playerName + " | IP: " + playerIP);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!dataManager.hasRegisteredIP(uuid)) {
            String ip = player.getAddress().getAddress().getHostAddress();
            dataManager.registerFirstIP(uuid, ip);
            plugin.getLogger().info("First IP registered for " + player.getName() + ": " + ip);
        }
    }
}
