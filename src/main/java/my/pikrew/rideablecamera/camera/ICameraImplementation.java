package my.pikrew.rideablecamera.camera;

import my.pikrew.rideablecamera.models.CameraSession;
import org.bukkit.entity.Player;

/**
 * Interface for different camera implementations
 * Allows easy switching between ArmorStand, FakePlayer, or custom implementations
 */
public interface ICameraImplementation {

    /**
     * Create a new camera session for a player
     * @param player The player
     * @return CameraSession or null if failed
     */
    CameraSession createSession(Player player);

    /**
     * Update an existing camera session
     * @param session The session to update
     * @param player The player
     */
    void updateSession(CameraSession session, Player player);

    /**
     * Destroy a camera session and cleanup
     * @param session The session to destroy
     */
    void destroySession(CameraSession session);

    /**
     * Get the name of this implementation
     * @return Implementation name
     */
    String getName();
}
