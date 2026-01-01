package my.pikrew.rideablecamera.nms;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Pose;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_21_R2.CraftServer;
import org.bukkit.craftbukkit.v1_21_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * Wrapper for creating and managing fake player entities
 * Uses NMS for Minecraft 1.21.4
 */
public class FakePlayerEntity {

    private static final EntityDataAccessor<Byte> DATA_PLAYER_MODE_CUSTOMISATION;

    static {
        // Player skin layer flags
        DATA_PLAYER_MODE_CUSTOMISATION = EntityDataAccessor.createKey(
                net.minecraft.world.entity.player.Player.class,
                EntityDataSerializers.BYTE
        );
    }

    private final ServerPlayer nmsPlayer;
    private final GameProfile gameProfile;
    private Location currentLocation;

    /**
     * Create a new fake player
     * @param location Spawn location
     * @param name Display name
     * @param skinUUID UUID for skin (use player's UUID to copy skin)
     */
    public FakePlayerEntity(Location location, String name, UUID skinUUID) {
        this.currentLocation = location.clone();

        // Create game profile
        this.gameProfile = new GameProfile(UUID.randomUUID(), name);

        // Get NMS world
        ServerLevel world = ((CraftWorld) location.getWorld()).getHandle();

        // Create fake player entity
        this.nmsPlayer = new ServerPlayer(
                ((CraftServer) Bukkit.getServer()).getServer(),
                world,
                gameProfile,
                net.minecraft.world.entity.player.ClientInformation.createDefault()
        );

        // Set position
        nmsPlayer.setPos(location.getX(), location.getY(), location.getZ());
        nmsPlayer.setYRot(location.getYaw());
        nmsPlayer.setXRot(location.getPitch());
        nmsPlayer.setYHeadRot(location.getYaw());

        // Set properties
        nmsPlayer.setInvulnerable(true);
        nmsPlayer.setNoGravity(false);

        // Enable all skin layers
        setSkinLayers(true, true, true, true, true, true, true);
    }

    /**
     * Spawn the fake player for a viewer
     * @param viewer Player who will see this fake player
     */
    public void spawn(Player viewer) {
        ServerPlayer nmsViewer = ((CraftPlayer) viewer).getHandle();
        ServerGamePacketListenerImpl connection = nmsViewer.connection;

        // Send spawn packet
        connection.send(new ClientboundPlayerInfoUpdatePacket(
                ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,
                nmsPlayer
        ));

        // Send entity spawn packet
        connection.send(new ClientboundAddEntityPacket(nmsPlayer));

        // Send metadata
        connection.send(new ClientboundSetEntityDataPacket(
                nmsPlayer.getId(),
                nmsPlayer.getEntityData().getNonDefaultValues()
        ));

        // Send head rotation
        connection.send(new ClientboundRotateHeadPacket(
                nmsPlayer,
                (byte) ((nmsPlayer.getYHeadRot() * 256.0F) / 360.0F)
        ));

        // Send equipment if any
        sendEquipment(viewer);
    }

    /**
     * Despawn the fake player for a viewer
     * @param viewer Player who will no longer see this fake player
     */
    public void despawn(Player viewer) {
        ServerPlayer nmsViewer = ((CraftPlayer) viewer).getHandle();
        ServerGamePacketListenerImpl connection = nmsViewer.connection;

        // Send remove entity packet
        connection.send(new ClientboundRemoveEntitiesPacket(nmsPlayer.getId()));

        // Remove from player info
        connection.send(new ClientboundPlayerInfoRemovePacket(List.of(nmsPlayer.getUUID())));
    }

    /**
     * Teleport fake player to location
     * @param location New location
     * @param viewer Player who will see the teleport
     */
    public void teleport(Location location, Player viewer) {
        this.currentLocation = location.clone();

        nmsPlayer.setPos(location.getX(), location.getY(), location.getZ());
        nmsPlayer.setYRot(location.getYaw());
        nmsPlayer.setXRot(location.getPitch());
        nmsPlayer.setYHeadRot(location.getYaw());

        ServerPlayer nmsViewer = ((CraftPlayer) viewer).getHandle();
        ServerGamePacketListenerImpl connection = nmsViewer.connection;

        // Send teleport packet
        connection.send(new ClientboundTeleportEntityPacket(nmsPlayer));

        // Send head rotation
        connection.send(new ClientboundRotateHeadPacket(
                nmsPlayer,
                (byte) ((location.getYaw() * 256.0F) / 360.0F)
        ));
    }

    /**
     * Move fake player with animation
     * @param location New location
     * @param onGround Is on ground
     * @param viewer Player who will see the movement
     */
    public void move(Location location, boolean onGround, Player viewer) {
        this.currentLocation = location.clone();

        // Calculate deltas
        double deltaX = (location.getX() * 32 - nmsPlayer.getX() * 32) * 128;
        double deltaY = (location.getY() * 32 - nmsPlayer.getY() * 32) * 128;
        double deltaZ = (location.getZ() * 32 - nmsPlayer.getZ() * 32) * 128;

        // Update position
        nmsPlayer.setPos(location.getX(), location.getY(), location.getZ());
        nmsPlayer.setYRot(location.getYaw());
        nmsPlayer.setXRot(location.getPitch());
        nmsPlayer.setYHeadRot(location.getYaw());
        nmsPlayer.setOnGround(onGround);

        ServerPlayer nmsViewer = ((CraftPlayer) viewer).getHandle();
        ServerGamePacketListenerImpl connection = nmsViewer.connection;

        // Send movement packet if delta is small enough
        if (Math.abs(deltaX) < 32768 && Math.abs(deltaY) < 32768 && Math.abs(deltaZ) < 32768) {
            connection.send(new ClientboundMoveEntityPacket.PosRot(
                    nmsPlayer.getId(),
                    (short) deltaX,
                    (short) deltaY,
                    (short) deltaZ,
                    (byte) ((location.getYaw() * 256.0F) / 360.0F),
                    (byte) ((location.getPitch() * 256.0F) / 360.0F),
                    onGround
            ));
        } else {
            // Too far, use teleport
            connection.send(new ClientboundTeleportEntityPacket(nmsPlayer));
        }

        // Send head rotation
        connection.send(new ClientboundRotateHeadPacket(
                nmsPlayer,
                (byte) ((location.getYaw() * 256.0F) / 360.0F)
        ));
    }

    /**
     * Update only rotation
     * @param yaw New yaw
     * @param pitch New pitch
     * @param viewer Player who will see the rotation
     */
    public void rotate(float yaw, float pitch, Player viewer) {
        nmsPlayer.setYRot(yaw);
        nmsPlayer.setXRot(pitch);
        nmsPlayer.setYHeadRot(yaw);

        currentLocation.setYaw(yaw);
        currentLocation.setPitch(pitch);

        ServerPlayer nmsViewer = ((CraftPlayer) viewer).getHandle();
        ServerGamePacketListenerImpl connection = nmsViewer.connection;

        // Send rotation packet
        connection.send(new ClientboundMoveEntityPacket.Rot(
                nmsPlayer.getId(),
                (byte) ((yaw * 256.0F) / 360.0F),
                (byte) ((pitch * 256.0F) / 360.0F),
                nmsPlayer.onGround()
        ));

        // Send head rotation
        connection.send(new ClientboundRotateHeadPacket(
                nmsPlayer,
                (byte) ((yaw * 256.0F) / 360.0F)
        ));
    }

    /**
     * Copy equipment from real player
     * @param player Source player
     * @param viewer Player who will see the equipment
     */
    public void copyEquipment(Player player, Player viewer) {
        ServerPlayer nmsSource = ((CraftPlayer) player).getHandle();

        // Copy inventory
        nmsPlayer.getInventory().replaceWith(nmsSource.getInventory());

        // Send equipment update
        sendEquipment(viewer);
    }

    /**
     * Send equipment packets
     * @param viewer Player who will see the equipment
     */
    private void sendEquipment(Player viewer) {
        ServerPlayer nmsViewer = ((CraftPlayer) viewer).getHandle();
        ServerGamePacketListenerImpl connection = nmsViewer.connection;

        // Send equipment packet
        connection.send(new ClientboundSetEquipmentPacket(
                nmsPlayer.getId(),
                List.of(
                        com.mojang.datafixers.util.Pair.of(
                                net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                                nmsPlayer.getInventory().getItem(nmsPlayer.getInventory().selected)
                        ),
                        com.mojang.datafixers.util.Pair.of(
                                net.minecraft.world.entity.EquipmentSlot.OFFHAND,
                                nmsPlayer.getInventory().offhand.get(0)
                        ),
                        com.mojang.datafixers.util.Pair.of(
                                net.minecraft.world.entity.EquipmentSlot.HEAD,
                                nmsPlayer.getInventory().armor.get(3)
                        ),
                        com.mojang.datafixers.util.Pair.of(
                                net.minecraft.world.entity.EquipmentSlot.CHEST,
                                nmsPlayer.getInventory().armor.get(2)
                        ),
                        com.mojang.datafixers.util.Pair.of(
                                net.minecraft.world.entity.EquipmentSlot.LEGS,
                                nmsPlayer.getInventory().armor.get(1)
                        ),
                        com.mojang.datafixers.util.Pair.of(
                                net.minecraft.world.entity.EquipmentSlot.FEET,
                                nmsPlayer.getInventory().armor.get(0)
                        )
                )
        ));
    }

    /**
     * Set skin layer visibility
     */
    private void setSkinLayers(boolean cape, boolean jacket, boolean leftSleeve,
                               boolean rightSleeve, boolean leftPants,
                               boolean rightPants, boolean hat) {
        byte b = 0;
        if (cape) b |= 0x01;
        if (jacket) b |= 0x02;
        if (leftSleeve) b |= 0x04;
        if (rightSleeve) b |= 0x08;
        if (leftPants) b |= 0x10;
        if (rightPants) b |= 0x20;
        if (hat) b |= 0x40;

        nmsPlayer.getEntityData().set(DATA_PLAYER_MODE_CUSTOMISATION, b);
    }

    /**
     * Set sneaking state
     * @param sneaking Is sneaking
     * @param viewer Player who will see the change
     */
    public void setSneaking(boolean sneaking, Player viewer) {
        nmsPlayer.setShiftKeyDown(sneaking);
        nmsPlayer.setPose(sneaking ? Pose.CROUCHING : Pose.STANDING);

        ServerPlayer nmsViewer = ((CraftPlayer) viewer).getHandle();
        nmsViewer.connection.send(new ClientboundSetEntityDataPacket(
                nmsPlayer.getId(),
                nmsPlayer.getEntityData().getNonDefaultValues()
        ));
    }

    /**
     * Set sprinting state
     * @param sprinting Is sprinting
     * @param viewer Player who will see the change
     */
    public void setSprinting(boolean sprinting, Player viewer) {
        nmsPlayer.setSprinting(sprinting);

        ServerPlayer nmsViewer = ((CraftPlayer) viewer).getHandle();
        nmsViewer.connection.send(new ClientboundSetEntityDataPacket(
                nmsPlayer.getId(),
                nmsPlayer.getEntityData().getNonDefaultValues()
        ));
    }

    /**
     * Play animation
     * @param animation Animation type (0=swing main hand, 3=leave bed, 4=swing offhand, etc)
     * @param viewer Player who will see the animation
     */
    public void playAnimation(int animation, Player viewer) {
        ServerPlayer nmsViewer = ((CraftPlayer) viewer).getHandle();
        nmsViewer.connection.send(new ClientboundAnimatePacket(nmsPlayer, animation));
    }

    // Getters
    public ServerPlayer getNmsPlayer() {
        return nmsPlayer;
    }

    public int getEntityId() {
        return nmsPlayer.getId();
    }

    public Location getLocation() {
        return currentLocation.clone();
    }

    public GameProfile getGameProfile() {
        return gameProfile;
    }
}