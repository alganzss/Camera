package my.pikrew.rideablecamera.camera;

import my.pikrew.rideablecamera.RideableCameraPlugin;
import my.pikrew.rideablecamera.camera.impl.ArmorStandCamera;
import my.pikrew.rideablecamera.camera.impl.FakePlayerCamera;
import my.pikrew.rideablecamera.config.ConfigManager;
import my.pikrew.rideablecamera.models.CameraSession;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages all camera sessions for players
 */
public class CameraManager {

    private final RideableCameraPlugin plugin;
    private final Map<UUID, CameraSession> activeSessions;
    private final ICameraImplementation cameraImpl;

    public CameraManager(RideableCameraPlugin plugin) {
        this.plugin = plugin;
        this.activeSessions = new HashMap<>();

        // Choose implementation based on config
        ConfigManager config = plugin.getConfigManager();
        String implType = config.getCameraImplementation();

        if (implType.equalsIgnoreCase("fakeplayer")) {
            this.cameraImpl = new FakePlayerCamera(plugin);
        } else {
            this.cameraImpl = new ArmorStandCamera(plugin);
        }

        plugin.getLogger().info("Using camera implementation: " + cameraImpl.getClass().getSimpleName());
    }

    /**
     * Enable camera for a player
     * @param player The player
     * @return true if successful
     */
    public boolean enableCamera(Player player) {
        if (isActive(player)) {
            return false;
        }

        try {
            CameraSession session = cameraImpl.createSession(player);

            if (session != null) {
                activeSessions.put(player.getUniqueId(), session);
                return true;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to enable camera for " + player.getName());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Disable camera for a player
     * @param player The player
     * @return true if successful
     */
    public boolean disableCamera(Player player) {
        CameraSession session = activeSessions.get(player.getUniqueId());

        if (session == null) {
            return false;
        }

        try {
            cameraImpl.destroySession(session);
            activeSessions.remove(player.getUniqueId());
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to disable camera for " + player.getName());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Toggle camera for a player
     * @param player The player
     * @return true if camera is now enabled, false if disabled
     */
    public boolean toggleCamera(Player player) {
        if (isActive(player)) {
            disableCamera(player);
            return false;
        } else {
            enableCamera(player);
            return true;
        }
    }

    /**
     * Check if camera is active for a player
     * @param player The player
     * @return true if active
     */
    public boolean isActive(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }

    /**
     * Get camera session for a player
     * @param player The player
     * @return CameraSession or null
     */
    public CameraSession getSession(Player player) {
        return activeSessions.get(player.getUniqueId());
    }

    /**
     * Get all active sessions
     * @return Map of active sessions
     */
    public Map<UUID, CameraSession> getActiveSessions() {
        return new HashMap<>(activeSessions);
    }

    /**
     * Disable all active cameras
     */
    public void disableAllCameras() {
        for (UUID uuid : new HashMap<>(activeSessions).keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                disableCamera(player);
            } else {
                // Player offline, force cleanup
                CameraSession session = activeSessions.get(uuid);
                if (session != null) {
                    try {
                        cameraImpl.destroySession(session);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to cleanup session for UUID: " + uuid);
                    }
                }
                activeSessions.remove(uuid);
            }
        }
    }

    /**
     * Start the camera update task
     */
    public void startUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                updateAllCameras();
            }
        }.runTaskTimer(plugin, 0L, 1L); // Update every tick
    }

    /**
     * Update all active cameras
     */
    private void updateAllCameras() {
        for (Map.Entry<UUID, CameraSession> entry : activeSessions.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());

            if (player == null || !player.isOnline()) {
                // Player left, cleanup
                try {
                    cameraImpl.destroySession(entry.getValue());
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to cleanup session for offline player");
                }
                activeSessions.remove(entry.getKey());
                continue;
            }

            try {
                cameraImpl.updateSession(entry.getValue(), player);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to update camera for " + player.getName());
            }
        }
    }
}