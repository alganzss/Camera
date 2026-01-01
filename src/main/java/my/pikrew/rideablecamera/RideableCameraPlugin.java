package my.pikrew.rideablecamera;

import my.pikrew.rideablecamera.camera.CameraManager;
import my.pikrew.rideablecamera.commands.CameraCommandExecutor;
import my.pikrew.rideablecamera.config.ConfigManager;
import my.pikrew.rideablecamera.listeners.PlayerListener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * RideableCamera Plugin
 * Main entry point for the plugin
 *
 * @author Pikrew
 * @version 1.0.0
 */
public class RideableCameraPlugin extends JavaPlugin {

    private static RideableCameraPlugin instance;

    // Managers
    private ConfigManager configManager;
    private CameraManager cameraManager;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize managers
        initializeManagers();

        // Register commands
        registerCommands();

        // Register listeners
        registerListeners();

        getLogger().info("RideableCamera has been enabled!");
        getLogger().info("Version: " + getDescription().getVersion());
    }

    @Override
    public void onDisable() {
        // Cleanup all active cameras
        if (cameraManager != null) {
            cameraManager.disableAllCameras();
        }

        getLogger().info("RideableCamera has been disabled!");
    }

    /**
     * Initialize all managers
     */
    private void initializeManagers() {
        // Config manager
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        // Camera manager
        cameraManager = new CameraManager(this);
        cameraManager.startUpdateTask();
    }

    /**
     * Register all commands
     */
    private void registerCommands() {
        CameraCommandExecutor commandExecutor = new CameraCommandExecutor(this);
        getCommand("ridecam").setExecutor(commandExecutor);
        getCommand("ridecam").setTabCompleter(commandExecutor);
    }

    /**
     * Register all event listeners
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(
                new PlayerListener(this),
                this
        );
    }

    /**
     * Get plugin instance
     * @return Plugin instance
     */
    public static RideableCameraPlugin getInstance() {
        return instance;
    }

    /**
     * Get config manager
     * @return ConfigManager instance
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Get camera manager
     * @return CameraManager instance
     */
    public CameraManager getCameraManager() {
        return cameraManager;
    }
}