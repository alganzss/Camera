package my.pikrew.rideablecamera.camera.impl;

import my.pikrew.rideablecamera.RideableCameraPlugin;
import my.pikrew.rideablecamera.camera.ICameraImplementation;
import my.pikrew.rideablecamera.models.CameraSession;
import my.pikrew.rideablecamera.models.CameraSettings;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Camera implementation using ArmorStand entity
 * Simple and stable, works on all versions
 */
public class ArmorStandCamera implements ICameraImplementation {

    private final RideableCameraPlugin plugin;

    public ArmorStandCamera(RideableCameraPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public CameraSession createSession(Player player) {
        // Get settings from config
        CameraSettings settings = plugin.getConfigManager().getDefaultCameraSettings();

        // Create session
        CameraSession session = new CameraSession(player, settings);

        // Spawn armor stand
        Location spawnLoc = player.getLocation();
        ArmorStand armorStand = (ArmorStand) spawnLoc.getWorld().spawnEntity(
                spawnLoc,
                EntityType.ARMOR_STAND
        );

        // Configure armor stand
        configureArmorStand(armorStand, player, settings);

        // Set as camera entity
        session.setCameraEntity(armorStand);

        // Setup player
        setupPlayer(player, armorStand);

        return session;
    }

    @Override
    public void updateSession(CameraSession session, Player player) {
        if (!session.isValid()) {
            return;
        }

        ArmorStand armorStand = (ArmorStand) session.getCameraEntity();

        // Update armor stand rotation to match player view
        Location playerLoc = player.getLocation();
        armorStand.setRotation(playerLoc.getYaw(), playerLoc.getPitch());

        // Sync equipment if enabled
        if (session.getSettings().shouldCopyEquipment()) {
            syncEquipment(player, armorStand);
        }

        // Keep spectator target locked
        if (player.getSpectatorTarget() != armorStand) {
            player.setSpectatorTarget(armorStand);
        }
    }

    @Override
    public void destroySession(CameraSession session) {
        Player player = session.getPlayer();
        ArmorStand armorStand = (ArmorStand) session.getCameraEntity();

        // Remove player effects
        if (player != null && player.isOnline()) {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);

            // Remove from armor stand
            if (armorStand != null && armorStand.getPassengers().contains(player)) {
                armorStand.removePassenger(player);
            }

            // Reset spectator target
            player.setSpectatorTarget(null);

            // Restore gamemode
            player.setGameMode(session.getOriginalGameMode());

            // Teleport to armor stand location
            if (armorStand != null && armorStand.isValid()) {
                player.teleport(armorStand.getLocation());
            }

            // Restore flying state
            if (session.wasFlying() && player.getAllowFlight()) {
                player.setFlying(true);
            }
        }

        // Remove armor stand
        if (armorStand != null && armorStand.isValid()) {
            armorStand.remove();
        }

        session.setActive(false);
    }

    @Override
    public String getName() {
        return "ArmorStand";
    }

    /**
     * Configure armor stand properties
     */
    private void configureArmorStand(ArmorStand armorStand, Player player, CameraSettings settings) {
        armorStand.setVisible(true);
        armorStand.setGravity(false);
        armorStand.setInvulnerable(true);
        armorStand.setBasePlate(false);
        armorStand.setArms(true);
        armorStand.setSmall(false);
        armorStand.setMarker(false);

        // Set name
        if (settings.shouldShowNpcName()) {
            armorStand.setCustomName("§e" + player.getName());
            armorStand.setCustomNameVisible(true);
        } else {
            armorStand.setCustomNameVisible(false);
        }

        // Copy equipment
        if (settings.shouldCopyEquipment()) {
            syncEquipment(player, armorStand);
        }
    }

    /**
     * Setup player for camera mode
     */
    private void setupPlayer(Player player, ArmorStand armorStand) {
        // Make player invisible
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.INVISIBILITY,
                Integer.MAX_VALUE,
                1,
                false,
                false
        ));

        // Make player ride armor stand
        armorStand.addPassenger(player);

        // Set spectator mode
        player.setGameMode(GameMode.SPECTATOR);

        // Set spectator target
        player.setSpectatorTarget(armorStand);
    }

    /**
     * Sync equipment from player to armor stand
     */
    private void syncEquipment(Player player, ArmorStand armorStand) {
        armorStand.getEquipment().setHelmet(player.getInventory().getHelmet());
        armorStand.getEquipment().setChestplate(player.getInventory().getChestplate());
        armorStand.getEquipment().setLeggings(player.getInventory().getLeggings());
        armorStand.getEquipment().setBoots(player.getInventory().getBoots());
        armorStand.getEquipment().setItemInMainHand(player.getInventory().getItemInMainHand());
        armorStand.getEquipment().setItemInOffHand(player.getInventory().getItemInOffHand());
    }
}