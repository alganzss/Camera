package my.pikrew.rideablecamera.camera.impl;

import my.pikrew.rideablecamera.RideableCameraPlugin;
import my.pikrew.rideablecamera.camera.ICameraImplementation;
import my.pikrew.rideablecamera.models.CameraSession;
import org.bukkit.entity.Player;

/**
 * Camera implementation using Fake Player entity (NMS)
 * More realistic but requires version-specific code
 *
 * TODO: Implement NMS-based fake player
 * For now, falls back to ArmorStand implementation
 */
public class FakePlayerCamera implements ICameraImplementation {

    private final RideableCameraPlugin plugin;
    private final ArmorStandCamera fallback;

    public FakePlayerCamera(RideableCameraPlugin plugin) {
        this.plugin = plugin;
        this.fallback = new ArmorStandCamera(plugin);

        plugin.getLogger().warning("FakePlayer implementation not yet available!");
        plugin.getLogger().warning("Falling back to ArmorStand implementation.");
    }

    @Override
    public CameraSession createSession(Player player) {
        // TODO: Implement NMS fake player creation
        // For now, use ArmorStand fallback
        return fallback.createSession(player);
    }

    @Override
    public void updateSession(CameraSession session, Player player) {
        // TODO: Implement NMS fake player update
        fallback.updateSession(session, player);
    }

    @Override
    public void destroySession(CameraSession session) {
        // TODO: Implement NMS fake player cleanup
        fallback.destroySession(session);
    }

    @Override
    public String getName() {
        return "FakePlayer (Fallback to ArmorStand)";
    }
}