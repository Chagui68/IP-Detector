package com.chagui68.singleip.storage;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class IPDataManager {

    private static final Pattern IP_PATTERN = Pattern.compile(
            "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$|" +
                    "^(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4})$"
    );

    private final com.chagui68.singleip.SingleIPPlugin plugin;
    private final File dataFile;
    private final File failedLoginsFile;
    private final Map<UUID, List<String>> playerIPs;
    private final Logger logger;
    private final DateTimeFormatter dateFormatter;

    public IPDataManager(com.chagui68.singleip.SingleIPPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.dataFile = new File(plugin.getDataFolder(), "player-ips.yml");
        this.failedLoginsFile = new File(plugin.getDataFolder(), "failed-logins.yml");
        this.playerIPs = new ConcurrentHashMap<>();
        this.dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    }

    public void loadData() {
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
                logger.info("Data file created: player-ips.yml");
            } catch (IOException e) {
                logger.severe("Error creating data file: " + e.getMessage());
                return;
            }
        }

        if (!failedLoginsFile.exists()) {
            try {
                failedLoginsFile.createNewFile();
                logger.info("Failed logins file created: failed-logins.yml");
            } catch (IOException e) {
                logger.severe("Error creating failed logins file: " + e.getMessage());
            }
        }

        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);

        if (data.contains("players")) {
            for (String uuidString : data.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    List<String> ips = data.getStringList("players." + uuidString);
                    playerIPs.put(uuid, new ArrayList<>(ips));
                } catch (IllegalArgumentException e) {
                    logger.warning("Invalid UUID ignored: " + uuidString);
                }
            }
        }

        logger.info("Loaded " + playerIPs.size() + " players with their IPs");
    }

    public void saveData() {
        FileConfiguration data = new YamlConfiguration();

        for (Map.Entry<UUID, List<String>> entry : playerIPs.entrySet()) {
            data.set("players." + entry.getKey().toString(), entry.getValue());
        }

        try {
            data.save(dataFile);
            if (plugin.getConfig().getBoolean("debug", false)) {
                logger.info("Data saved: " + playerIPs.size() + " players");
            }
        } catch (IOException e) {
            logger.severe("Error saving data: " + e.getMessage());
        }
    }

    public void logFailedLogin(String playerName, UUID uuid, String attemptedIP, String reason) {
        FileConfiguration failedLogins = YamlConfiguration.loadConfiguration(failedLoginsFile);

        String timestamp = LocalDateTime.now().format(dateFormatter);
        String logKey = "failed-logins." + timestamp.replace(" ", "_").replace(":", "-");

        failedLogins.set(logKey + ".player", playerName);
        failedLogins.set(logKey + ".uuid", uuid.toString());
        failedLogins.set(logKey + ".attempted-ip", attemptedIP);
        failedLogins.set(logKey + ".reason", reason);
        failedLogins.set(logKey + ".timestamp", timestamp);

        try {
            failedLogins.save(failedLoginsFile);
            if (plugin.getConfig().getBoolean("log-failed-attempts", true)) {
                logger.warning("Failed login attempt - Player: " + playerName + " | IP: " + attemptedIP + " | Reason: " + reason);
            }
        } catch (IOException e) {
            logger.severe("Error saving failed login: " + e.getMessage());
        }
    }

    public List<Map<String, String>> getFailedLogins(int limit) {
        FileConfiguration failedLogins = YamlConfiguration.loadConfiguration(failedLoginsFile);
        List<Map<String, String>> attempts = new ArrayList<>();

        if (!failedLogins.contains("failed-logins")) {
            return attempts;
        }

        Set<String> keys = failedLogins.getConfigurationSection("failed-logins").getKeys(false);
        List<String> sortedKeys = new ArrayList<>(keys);
        Collections.sort(sortedKeys, Collections.reverseOrder());

        int count = 0;
        for (String key : sortedKeys) {
            if (count >= limit) break;

            Map<String, String> attempt = new HashMap<>();
            attempt.put("player", failedLogins.getString("failed-logins." + key + ".player"));
            attempt.put("uuid", failedLogins.getString("failed-logins." + key + ".uuid"));
            attempt.put("attempted-ip", failedLogins.getString("failed-logins." + key + ".attempted-ip"));
            attempt.put("reason", failedLogins.getString("failed-logins." + key + ".reason"));
            attempt.put("timestamp", failedLogins.getString("failed-logins." + key + ".timestamp"));
            attempts.add(attempt);
            count++;
        }

        return attempts;
    }

    public List<Map<String, String>> getFailedLoginsByPlayer(UUID uuid, int limit) {
        FileConfiguration failedLogins = YamlConfiguration.loadConfiguration(failedLoginsFile);
        List<Map<String, String>> attempts = new ArrayList<>();

        if (!failedLogins.contains("failed-logins")) {
            return attempts;
        }

        Set<String> keys = failedLogins.getConfigurationSection("failed-logins").getKeys(false);
        List<String> sortedKeys = new ArrayList<>(keys);
        Collections.sort(sortedKeys, Collections.reverseOrder());

        int count = 0;
        for (String key : sortedKeys) {
            String storedUUID = failedLogins.getString("failed-logins." + key + ".uuid");
            if (storedUUID != null && storedUUID.equals(uuid.toString())) {
                if (count >= limit) break;

                Map<String, String> attempt = new HashMap<>();
                attempt.put("player", failedLogins.getString("failed-logins." + key + ".player"));
                attempt.put("uuid", storedUUID);
                attempt.put("attempted-ip", failedLogins.getString("failed-logins." + key + ".attempted-ip"));
                attempt.put("reason", failedLogins.getString("failed-logins." + key + ".reason"));
                attempt.put("timestamp", failedLogins.getString("failed-logins." + key + ".timestamp"));
                attempts.add(attempt);
                count++;
            }
        }

        return attempts;
    }

    public List<String> getRegisteredIPs(UUID uuid) {
        return playerIPs.getOrDefault(uuid, new ArrayList<>());
    }

    public boolean registerFirstIP(UUID uuid, String ip) {
        if (!playerIPs.containsKey(uuid)) {
            List<String> ips = new ArrayList<>();
            ips.add(ip);
            playerIPs.put(uuid, ips);
            saveData();

            if (plugin.getConfig().getBoolean("debug", false)) {
                logger.info("Primary IP registered - UUID: " + uuid + " | IP: " + ip);
            }
            return true;
        }
        return false;
    }

    public boolean addSecondIP(UUID uuid, String ip) {
        if (!playerIPs.containsKey(uuid)) {
            return false;
        }

        List<String> ips = playerIPs.get(uuid);

        if (ips.contains(ip)) {
            return false;
        }

        ips.add(ip);
        saveData();

        logger.info("IP added - UUID: " + uuid + " | IP: " + ip);
        return true;
    }

    public boolean removeIP(UUID uuid, String ip) {
        if (!playerIPs.containsKey(uuid)) {
            return false;
        }

        List<String> ips = playerIPs.get(uuid);
        boolean removed = ips.remove(ip);

        if (removed) {
            if (ips.isEmpty()) {
                playerIPs.remove(uuid);
            }
            saveData();
            logger.info("IP removed - UUID: " + uuid + " | IP: " + ip);
        }

        return removed;
    }

    public boolean isIPAllowed(UUID uuid, String ip) {
        if (!playerIPs.containsKey(uuid)) {
            return false;
        }
        return playerIPs.get(uuid).contains(ip);
    }

    public boolean hasRegisteredIP(UUID uuid) {
        return playerIPs.containsKey(uuid);
    }

    public int getIPCount(UUID uuid) {
        return playerIPs.getOrDefault(uuid, new ArrayList<>()).size();
    }

    public int getStoredPlayerCount() {
        return playerIPs.size();
    }

    public boolean isValidIP(String ip) {
        return IP_PATTERN.matcher(ip).matches();
    }

    public Set<UUID> getAllPlayers() {
        return playerIPs.keySet();
    }

    public void removeAllIPs(UUID uuid) {
        playerIPs.remove(uuid);
        saveData();
    }
}
