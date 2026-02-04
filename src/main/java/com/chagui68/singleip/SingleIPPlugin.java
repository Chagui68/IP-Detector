package com.chagui68.singleip;

import com.chagui68.singleip.commands.IPCommand;
import com.chagui68.singleip.listeners.PlayerConnectionListener;
import com.chagui68.singleip.storage.IPDataManager;
import com.chagui68.singleip.utils.AccessLogger;
import org.bukkit.plugin.java.JavaPlugin;

public class SingleIPPlugin extends JavaPlugin {

    private IPDataManager dataManager;
    private AccessLogger accessLogger;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getDataFolder().mkdirs();

        dataManager = new IPDataManager(this);
        dataManager.loadData();

        accessLogger = new AccessLogger(this);

        getServer().getPluginManager().registerEvents(
                new PlayerConnectionListener(this, dataManager),
                this
        );

        getCommand("ipmanager").setExecutor(new IPCommand(this, dataManager));
        getLogger().info("SingleIPPlugin v2.0.0 enabled successfully");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveData();
        }
        getLogger().info("SingleIPPlugin disabled. Data saved.");
    }

    public IPDataManager getDataManager() {
        return dataManager;
    }

    public AccessLogger getAccessLogger() {
        return accessLogger;
    }
}
