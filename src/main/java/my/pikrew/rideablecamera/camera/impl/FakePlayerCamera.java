package my.pikrew.rideablecamera.camera.impl;

import my.pikrew.rideablecamera.RideableCameraPlugin;
import my.pikrew.rideablecamera.camera.ICameraImplementation;
import my.pikrew.rideablecamera.controller.MovementController;
import my.pikrew.rideablecamera.models.CameraSession;
import my.pikrew.rideablecamera.models.CameraSettings;
import my.pikrew.rideablecamera.nms.FakePlayerEntity;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Camera implementation using Fake Player entity (NMS)
 * More realistic with actual player model and animations
 * Now with full movement control!
 */
public class FakePlayerCamera implements ICameraImplementation {

    private final RideableCameraPlugin plugin;
    private final Map<UUID, FakePlayerEntity> fakePlayerMap;
    private final Map<UUID, MovementController> movementControllers;
    private final Map<UUID, Location> lastPlayerLocations;
    private final ArmorStandCamera fallback;

    public FakePlayerCamera(RideableCameraPlugin plugin) {
        this.plugin = plugin;
        this.fakePlayerMap = new HashMap<>();
        this.movementControllers = new HashMap<>();
        this.lastPlayerLocations = new HashMap<>();
        this.fallback = new ArmorStandCamera(plugin);

        // Check if NMS is available
        try {
            Class.forName("net.minecraft.server.level.ServerPlayer");
            plugin.getLogger().info("NMS detected! FakePlayer implementation is available.");
            plugin.getLogger().info("Players can now control their fake player with WASD movement!");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().warning("NMS not available! Falling back to ArmorStand implementation.");
        }
    }

    @Override
    public CameraSession createSession(Player player) {
        // Check if NMS is available
        try {
            Class.forName("net.minecraft.server.level.ServerPlayer");
        } catch (ClassNotFoundException e) {
            return fallback.createSession(player);
        }

        try {
            // Get settings from config
            CameraSettings settings = plugin.getConfigManager().getDefaultCameraSettings();

            // Create session
            CameraSession session = new CameraSession(player, settings);

            // Spawn fake player
            Location spawnLoc = player.getLocation();
            FakePlayerEntity fakePlayer = new FakePlayerEntity(
                    spawnLoc,
                    player.getName(),
                    player.getUniqueId()
            );

            // Store fake player and controller
            fakePlayerMap.put(player.getUniqueId(), fakePlayer);
            movementControllers.put(player.getUniqueId(), new MovementController());
            lastPlayerLocations.put(player.getUniqueId(), player.getLocation().clone());

            // Configure settings
            configureFakePlayer(fakePlayer, player, settings);

            // Spawn for all online players
            spawnForAllPlayers(fakePlayer, player);

            // Copy equipment
            if (settings.shouldCopyEquipment()) {
                fakePlayer.copyEquipment(player, player);
                broadcastEquipmentUpdate(fakePlayer, player);
            }

            // Setup player
            setupPlayer(player, session);

            // Store fake player in session as "marker"
            session.setCameraEntity(null); // We don't use Bukkit entity

            plugin.getLogger().info("Created FakePlayer camera for " + player.getName() + " with movement control!");

            return session;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create FakePlayer camera for " + player.getName());
            e.printStackTrace();
            return fallback.createSession(player);
        }
    }

    @Override
    public void updateSession(CameraSession session, Player player) {
        if (!session.isValid()) {
            return;
        }

        FakePlayerEntity fakePlayer = fakePlayerMap.get(player.getUniqueId());
        if (fakePlayer == null) {
            fallback.updateSession(session, player);
            return;
        }

        try {
            MovementController controller = movementControllers.get(player.getUniqueId());
            Location lastLocation = lastPlayerLocations.get(player.getUniqueId());
            Location currentPlayerLoc = player.getLocation();

            // Detect movement from player
            boolean isMoving = controller.detectMovementFromPlayer(lastLocation, currentPlayerLoc);

            // Update controller states
            controller.setSneak(player.isSneaking());
            controller.setJump(false); // Jump is detected separately

            // Get fake player's current location
            Location fakePlayerLoc = fakePlayer.getLocation();

            // Update rotation to match player's view
            float yaw = currentPlayerLoc.getYaw();
            float pitch = currentPlayerLoc.getPitch();

            // Apply movement if player is moving
            Location newLocation;
            if (isMoving || controller.getVelocity().lengthSquared() > 0.0001) {
                newLocation = controller.applyMovement(fakePlayerLoc, yaw);
                newLocation.setYaw(yaw);
                newLocation.setPitch(pitch);

                // Move fake player with animation
                fakePlayer.move(newLocation, controller.isOnGround(), player);
                broadcastMovement(fakePlayer, newLocation, controller.isOnGround(), player);
            } else {
                // Just update rotation
                if (Math.abs(fakePlayerLoc.getYaw() - yaw) > 0.5 ||
                        Math.abs(fakePlayerLoc.getPitch() - pitch) > 0.5) {
                    fakePlayer.rotate(yaw, pitch, player);
                    broadcastRotation(fakePlayer, yaw, pitch, player);
                }
            }

            // Update visual states
            fakePlayer.setSneaking(controller.isSneak(), player);
            fakePlayer.setSprinting(controller.isSprint(), player);
            broadcastStates(fakePlayer, controller, player);

            // Sync equipment if enabled
            if (session.getSettings().shouldCopyEquipment()) {
                // Check every 10 ticks to reduce packet spam
                if (session.getDuration() % 500 < 50) { // Every ~0.5 seconds
                    fakePlayer.copyEquipment(player, player);
                    broadcastEquipmentUpdate(fakePlayer, player);
                }
            }

            // Keep spectator target locked (player stays invisible at fake player location)
            if (player.getGameMode() != GameMode.SPECTATOR) {
                player.setGameMode(GameMode.SPECTATOR);
            }

            // Update last location
            lastPlayerLocations.put(player.getUniqueId(), currentPlayerLoc.clone());

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to update FakePlayer camera for " + player.getName());
            e.printStackTrace();
        }
    }

    @Override
    public void destroySession(CameraSession session) {
        Player player = session.getPlayer();
        FakePlayerEntity fakePlayer = fakePlayerMap.get(player.getUniqueId());

        if (fakePlayer == null) {
            fallback.destroySession(session);
            return;
        }

        try {
            // Remove player effects
            if (player != null && player.isOnline()) {
                player.removePotionEffect(PotionEffectType.INVISIBILITY);

                // Restore gamemode
                player.setGameMode(session.getOriginalGameMode());

                // Teleport to fake player location
                player.teleport(fakePlayer.getLocation());

                // Restore flying state
                if (session.wasFlying() && player.getAllowFlight()) {
                    player.setFlying(true);
                }
            }

            // Despawn fake player for all players
            despawnForAllPlayers(fakePlayer, player);

            // Cleanup
            fakePlayerMap.remove(player.getUniqueId());
            movementControllers.remove(player.getUniqueId());
            lastPlayerLocations.remove(player.getUniqueId());

            session.setActive(false);

            plugin.getLogger().info("Destroyed FakePlayer camera for " + player.getName());

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to destroy FakePlayer camera for " + player.getName());
            e.printStackTrace();
        }
    }

    @Override
    public String getName() {
        return "FakePlayer (NMS)";
    }

    /**
     * Configure fake player properties
     */
    private void configureFakePlayer(FakePlayerEntity fakePlayer, Player player, CameraSettings settings) {
        // Fake player is already configured in its constructor
        // Additional configuration can be added here if needed
    }

    /**
     * Setup player for camera mode
     */
    private void setupPlayer(Player player, CameraSession session) {
        // Make player invisible
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.INVISIBILITY,
                Integer.MAX_VALUE,
                1,
                false,
                false
        ));

        // Set spectator mode
        player.setGameMode(GameMode.SPECTATOR);

        // Allow flying for smooth movement
        player.setAllowFlight(true);
        player.setFlying(true);

        // Set fly speed for better control
        player.setFlySpeed(0.05f);
    }

    /**
     * Spawn fake player for all online players
     */
    private void spawnForAllPlayers(FakePlayerEntity fakePlayer, Player owner) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(owner)) {
                fakePlayer.spawn(viewer);
            }
        }
    }

    /**
     * Despawn fake player for all online players
     */
    private void despawnForAllPlayers(FakePlayerEntity fakePlayer, Player owner) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(owner)) {
                try {
                    fakePlayer.despawn(viewer);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to despawn fake player for " + viewer.getName());
                }
            }
        }
    }

    /**
     * Broadcast movement to all viewers
     */
    private void broadcastMovement(FakePlayerEntity fakePlayer, Location location,
                                   boolean onGround, Player owner) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(owner)) {
                try {
                    fakePlayer.move(location, onGround, viewer);
                } catch (Exception e) {
                    // Ignore packet errors
                }
            }
        }
    }

    /**
     * Broadcast rotation to all viewers
     */
    private void broadcastRotation(FakePlayerEntity fakePlayer, float yaw,
                                   float pitch, Player owner) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(owner)) {
                try {
                    fakePlayer.rotate(yaw, pitch, viewer);
                } catch (Exception e) {
                    // Ignore packet errors
                }
            }
        }
    }

    /**
     * Broadcast state changes to all viewers
     */
    private void broadcastStates(FakePlayerEntity fakePlayer, MovementController controller,
                                 Player owner) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(owner)) {
                try {
                    fakePlayer.setSneaking(controller.isSneak(), viewer);
                    fakePlayer.setSprinting(controller.isSprint(), viewer);
                } catch (Exception e) {
                    // Ignore packet errors
                }
            }
        }
    }

    /**
     * Broadcast equipment update to all viewers
     */
    private void broadcastEquipmentUpdate(FakePlayerEntity fakePlayer, Player owner) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(owner)) {
                try {
                    fakePlayer.copyEquipment(owner, viewer);
                } catch (Exception e) {
                    // Ignore packet errors
                }
            }
        }
    }

    /**
     * Get fake player for a session
     * @param player The player
     * @return FakePlayerEntity or null
     */
    public FakePlayerEntity getFakePlayer(Player player) {
        return fakePlayerMap.get(player.getUniqueId());
    }

    /**
     * Get movement controller for a session
     * @param player The player
     * @return MovementController or null
     */
    public MovementController getMovementController(Player player) {
        return movementControllers.get(player.getUniqueId());
    }
}