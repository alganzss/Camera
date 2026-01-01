package my.pikrew.rideablecamera.listeners;

import my.pikrew.rideablecamera.RideableCameraPlugin;
import my.pikrew.rideablecamera.camera.CameraManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles player-related events
 */
public class PlayerListener implements Listener {

    private final RideableCameraPlugin plugin;
    private final CameraManager cameraManager;

    public PlayerListener(RideableCameraPlugin plugin) {
        this.plugin = plugin;
        this.cameraManager = plugin.getCameraManager();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Auto-enable if configured
        if (plugin.getConfigManager().isAutoEnableOnJoin()) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    cameraManager.enableCamera(player);
                }
            }, 40L); // 2 second delay
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Disable camera if active
        if (cameraManager.isActive(player)) {
            cameraManager.disableCamera(player);
        }
    }
}