package my.pikrew.rideablecamera.config;

import my.pikrew.rideablecamera.RideableCameraPlugin;
import my.pikrew.rideablecamera.models.CameraSettings;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Manages plugin configuration
 */
public class ConfigManager {

    private final RideableCameraPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(RideableCameraPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Load configuration from file
     */
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        plugin.getLogger().info("Configuration loaded successfully!");
    }

    /**
     * Reload configuration
     */
    public void reloadConfiguration() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        plugin.getLogger().info("Configuration reloaded!");
    }

    /**
     * Get camera implementation type
     * @return Implementation type (armorstand or fakeplayer)
     */
    public String getCameraImplementation() {
        return config.getString("camera.implementation", "armorstand");
    }

    /**
     * Check if auto-enable on join is enabled
     * @return true if enabled
     */
    public boolean isAutoEnableOnJoin() {
        return config.getBoolean("camera.auto-enable-on-join", false);
    }

    /**
     * Get default camera settings from config
     * @return CameraSettings
     */
    public CameraSettings getDefaultCameraSettings() {
        CameraSettings settings = new CameraSettings();

        settings.setDistance(config.getDouble("camera.distance", 3.5));
        settings.setHeight(config.getDouble("camera.height", 1.5));
        settings.setSideOffset(config.getDouble("camera.side-offset", 0.0));
        settings.setSmoothness(config.getDouble("camera.smoothness", 0.15));
        settings.setCopyEquipment(config.getBoolean("npc.copy-equipment", true));
        settings.setShowNpcName(config.getBoolean("npc.show-name", false));

        return settings;
    }

    /**
     * Get message with prefix
     * @param key Message key
     * @return Formatted message
     */
    public String getMessage(String key) {
        String prefix = config.getString("messages.prefix", "§a[RideCam]§f");
        String message = config.getString("messages." + key, key);
        return prefix + " " + message;
    }

    /**
     * Get raw message without prefix
     * @param key Message key
     * @return Raw message
     */
    public String getRawMessage(String key) {
        return config.getString("messages." + key, key);
    }

    /**
     * Check if double sneak toggle is enabled
     * @return true if enabled
     */
    public boolean isDoubleSneakToggle() {
        return config.getBoolean("camera.double-sneak-toggle", false);
    }

    /**
     * Get update interval in ticks
     * @return Update interval
     */
    public long getUpdateInterval() {
        return config.getLong("camera.update-interval", 1L);
    }

    /**
     * Check if sync equipment is enabled
     * @return true if enabled
     */
    public boolean isSyncEquipment() {
        return config.getBoolean("npc.sync-equipment", true);
    }
}
