package com.chagui68.singleip.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class GeoIPChecker {

    private static final String API_URL = "http://ip-api.com/json/";
    private final Logger logger;
    private final Map<String, CachedCountry> cache;
    private final int cacheDuration;

    public GeoIPChecker(Logger logger, int cacheDurationMinutes) {
        this.logger = logger;
        this.cache = new HashMap<>();
        this.cacheDuration = cacheDurationMinutes * 60 * 1000;
    }

    public CompletableFuture<String> getCountryCode(String ip) {
        return CompletableFuture.supplyAsync(() -> {
            if (isLocalIP(ip)) {
                return "LOCAL";
            }

            CachedCountry cached = cache.get(ip);
            if (cached != null && !cached.isExpired()) {
                logger.fine("Country for " + ip + " retrieved from cache: " + cached.countryCode);
                return cached.countryCode;
            }

            try {
                URL url = new URL(API_URL + ip + "?fields=status,message,countryCode");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("User-Agent", "MinecraftServer/SingleIPPlugin-Chagui68");

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();

                    JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();

                    if (json.get("status").getAsString().equals("success")) {
                        String countryCode = json.get("countryCode").getAsString();

                        cache.put(ip, new CachedCountry(countryCode, System.currentTimeMillis()));

                        logger.info("Country detected for " + ip + ": " + countryCode);
                        return countryCode;
                    } else {
                        logger.warning("API error for " + ip + ": " + json.get("message").getAsString());
                        return null;
                    }
                } else {
                    logger.warning("HTTP error " + responseCode + " for IP: " + ip);
                    return null;
                }
            } catch (Exception e) {
                logger.severe("Error checking geolocation for " + ip + ": " + e.getMessage());
                return null;
            }
        });
    }

    private boolean isLocalIP(String ip) {
        return ip.equals("127.0.0.1") ||
                ip.equals("localhost") ||
                ip.startsWith("192.168.") ||
                ip.startsWith("10.") ||
                ip.startsWith("172.16.") ||
                ip.startsWith("172.17.") ||
                ip.startsWith("172.18.") ||
                ip.startsWith("172.19.") ||
                ip.startsWith("172.20.") ||
                ip.startsWith("172.21.") ||
                ip.startsWith("172.22.") ||
                ip.startsWith("172.23.") ||
                ip.startsWith("172.24.") ||
                ip.startsWith("172.25.") ||
                ip.startsWith("172.26.") ||
                ip.startsWith("172.27.") ||
                ip.startsWith("172.28.") ||
                ip.startsWith("172.29.") ||
                ip.startsWith("172.30.") ||
                ip.startsWith("172.31.");
    }

    public void cleanCache() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private class CachedCountry {
        String countryCode;
        long timestamp;

        CachedCountry(String countryCode, long timestamp) {
            this.countryCode = countryCode;
            this.timestamp = timestamp;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > cacheDuration;
        }
    }
}
