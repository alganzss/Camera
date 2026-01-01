package my.pikrew.rideablecamera.models;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Represents a camera session for a player
 * Contains all data needed for camera operation
 */
public class CameraSession {

    private final UUID playerUUID;
    private final Player player;

    // Original player data (for restoration)
    private final GameMode originalGameMode;
    private final Location originalLocation;
    private final boolean originalFlying;

    // Camera entity (ArmorStand, FakePlayer, etc)
    private Entity cameraEntity;

    // Camera settings
    private CameraSettings settings;

    // Session metadata
    private final long startTime;
    private boolean active;

    public CameraSession(Player player, CameraSettings settings) {
        this.playerUUID = player.getUniqueId();
        this.player = player;
        this.settings = settings;

        // Save original state
        this.originalGameMode = player.getGameMode();
        this.originalLocation = player.getLocation().clone();
        this.originalFlying = player.isFlying();

        this.startTime = System.currentTimeMillis();
        this.active = true;
    }

    // Getters
    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public Player getPlayer() {
        return player;
    }

    public GameMode getOriginalGameMode() {
        return originalGameMode;
    }

    public Location getOriginalLocation() {
        return originalLocation;
    }

    public boolean wasFlying() {
        return originalFlying;
    }

    public Entity getCameraEntity() {
        return cameraEntity;
    }

    public CameraSettings getSettings() {
        return settings;
    }

    public long getStartTime() {
        return startTime;
    }

    public boolean isActive() {
        return active;
    }

    // Setters
    public void setCameraEntity(Entity entity) {
        this.cameraEntity = entity;
    }

    public void setSettings(CameraSettings settings) {
        this.settings = settings;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Get session duration in milliseconds
     * @return Duration
     */
    public long getDuration() {
        return System.currentTimeMillis() - startTime;
    }

    /**
     * Check if session is valid
     * @return true if valid
     */
    public boolean isValid() {
        return active && player != null && player.isOnline() && cameraEntity != null && cameraEntity.isValid();
    }
}