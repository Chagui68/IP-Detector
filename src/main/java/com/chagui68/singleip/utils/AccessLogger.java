package com.chagui68.singleip.utils;

import com.chagui68.singleip.SingleIPPlugin;
import com.chagui68.singleip.utils.GeoIPChecker;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class AccessLogger {

    private final SingleIPPlugin plugin;
    private final File logFile;
    private final FileConfiguration logConfig;
    private final GeoIPChecker geoIPChecker;

    public AccessLogger(SingleIPPlugin plugin) {
        this.plugin = plugin;
        this.logFile = new File(plugin.getDataFolder(), "access-logs.yml");
        this.geoIPChecker = new GeoIPChecker(plugin.getLogger(), 60);

        if (!logFile.exists()) {
            try {
                logFile.getParentFile().mkdirs();
                logFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Error creating log file: " + e.getMessage());
            }
        }

        this.logConfig = YamlConfiguration.loadConfiguration(logFile);
    }

    public void logAccessAttempt(UUID uuid, String playerName, String ip, String deviceName) {
        if (!plugin.getConfig().getBoolean("access-logs.enabled", true)) {
            return;
        }

        String key = "attempts." + uuid.toString();
        List<String> attempts = logConfig.getStringList(key);

        int maxLogs = plugin.getConfig().getInt("access-logs.max-logs-per-player", 10);
        if (attempts.size() >= maxLogs) {
            attempts.remove(0);
        }

        String country = "Unknown";
        if (plugin.getConfig().getBoolean("access-logs.save-country", true)) {
            try {
                CompletableFuture<String> countryFuture = geoIPChecker.getCountryCode(ip);
                country = countryFuture.get();
                if (country == null) country = "Unknown";
            } catch (Exception e) {
                plugin.getLogger().warning("Could not get country for IP " + ip + ": " + e.getMessage());
            }
        }

        String timestamp = java.time.LocalDateTime.now().toString();
        String logEntry = String.format("[%s] IP: %s | Device: %s | Country: %s",
                timestamp, ip, deviceName, country);
        attempts.add(logEntry);

        logConfig.set(key, attempts);

        try {
            logConfig.save(logFile);

            if (plugin.getConfig().getBoolean("access-logs.show-in-console", true)) {
                plugin.getLogger().info("Access attempt logged - Player: " + playerName +
                        " | IP: " + ip + " | Device: " + deviceName + " | Country: " + country);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Error saving access log: " + e.getMessage());
        }
    }

    public List<String> getAccessLogs(UUID uuid) {
        String key = "attempts." + uuid.toString();
        return logConfig.getStringList(key);
    }

    public void clearAccessLogs(UUID uuid) {
        String key = "attempts." + uuid.toString();
        logConfig.set(key, null);

        try {
            logConfig.save(logFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error clearing access logs: " + e.getMessage());
        }
    }
}
