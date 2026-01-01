package my.pikrew.rideablecamera.controller;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Controls movement of fake player based on player input
 */
public class MovementController {

    // Movement states
    private boolean forward;
    private boolean backward;
    private boolean left;
    private boolean right;
    private boolean jump;
    private boolean sneak;
    private boolean sprint;

    // Movement speeds
    private double walkSpeed = 0.2;
    private double sprintSpeed = 0.28;
    private double sneakSpeed = 0.13;
    private double jumpPower = 0.42;
    private double gravity = 0.08;

    // Current velocity
    private Vector velocity = new Vector(0, 0, 0);

    // Ground detection
    private boolean onGround = true;

    // Friction and acceleration
    private double groundFriction = 0.6;
    private double airFriction = 0.98;
    private double acceleration = 0.1;

    /**
     * Update movement state from player input
     * @param player The controlling player
     */
    public void updateFromPlayer(Player player) {
        // Movement keys are detected through the player's movement
        // We'll use a different approach - detect from actual position changes
        // This is handled in the camera implementation
    }

    /**
     * Set movement state manually
     * @param forward Moving forward
     * @param backward Moving backward
     * @param left Moving left
     * @param right Moving right
     * @param jump Jumping
     * @param sneak Sneaking
     * @param sprint Sprinting
     */
    public void setMovementState(boolean forward, boolean backward, boolean left,
                                 boolean right, boolean jump, boolean sneak, boolean sprint) {
        this.forward = forward;
        this.backward = backward;
        this.left = left;
        this.right = right;
        this.jump = jump;
        this.sneak = sneak;
        this.sprint = sprint;
    }

    /**
     * Calculate movement direction
     * @param yaw Player yaw rotation
     * @return Movement vector
     */
    public Vector calculateMovement(float yaw) {
        Vector movement = new Vector(0, 0, 0);

        // Calculate forward/backward
        double forwardAmount = 0;
        if (forward) forwardAmount += 1;
        if (backward) forwardAmount -= 1;

        // Calculate left/right
        double strafeAmount = 0;
        if (left) strafeAmount += 1;
        if (right) strafeAmount -= 1;

        // Normalize if moving diagonally
        if (forwardAmount != 0 && strafeAmount != 0) {
            double length = Math.sqrt(forwardAmount * forwardAmount + strafeAmount * strafeAmount);
            forwardAmount /= length;
            strafeAmount /= length;
        }

        // Apply speed
        double speed = getCurrentSpeed();

        // Convert to world coordinates based on yaw
        double yawRad = Math.toRadians(yaw);
        double moveX = (strafeAmount * Math.cos(yawRad)) - (forwardAmount * Math.sin(yawRad));
        double moveZ = (strafeAmount * Math.sin(yawRad)) + (forwardAmount * Math.cos(yawRad));

        movement.setX(moveX * speed);
        movement.setZ(moveZ * speed);

        return movement;
    }

    /**
     * Update velocity and position
     * @param currentLocation Current location
     * @param yaw Current yaw
     * @return New location after applying movement
     */
    public Location applyMovement(Location currentLocation, float yaw) {
        Location newLocation = currentLocation.clone();

        // Calculate desired movement
        Vector movement = calculateMovement(yaw);

        // Apply acceleration
        velocity.setX(velocity.getX() + (movement.getX() - velocity.getX()) * acceleration);
        velocity.setZ(velocity.getZ() + (movement.getZ() - velocity.getZ()) * acceleration);

        // Apply friction
        double friction = onGround ? groundFriction : airFriction;
        if (!forward && !backward && !left && !right) {
            velocity.setX(velocity.getX() * friction);
            velocity.setZ(velocity.getZ() * friction);
        }

        // Apply jump
        if (jump && onGround) {
            velocity.setY(jumpPower);
            onGround = false;
        }

        // Apply gravity
        if (!onGround) {
            velocity.setY(velocity.getY() - gravity);
        }

        // Apply velocity to position
        newLocation.add(velocity);

        // Ground check (simplified)
        if (newLocation.getY() <= currentLocation.getY() && velocity.getY() <= 0) {
            newLocation.setY(currentLocation.getY());
            velocity.setY(0);
            onGround = true;
        }

        // Stop small movements
        if (Math.abs(velocity.getX()) < 0.003) velocity.setX(0);
        if (Math.abs(velocity.getZ()) < 0.003) velocity.setZ(0);

        return newLocation;
    }

    /**
     * Get current movement speed based on state
     * @return Current speed
     */
    private double getCurrentSpeed() {
        if (sneak) return sneakSpeed;
        if (sprint) return sprintSpeed;
        return walkSpeed;
    }

    /**
     * Detect movement from player position change
     * @param oldLocation Old player location
     * @param newLocation New player location
     * @return true if player moved
     */
    public boolean detectMovementFromPlayer(Location oldLocation, Location newLocation) {
        double deltaX = newLocation.getX() - oldLocation.getX();
        double deltaZ = newLocation.getZ() - oldLocation.getZ();
        double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        if (distance > 0.001) {
            // Player is moving, calculate direction relative to their yaw
            float yaw = newLocation.getYaw();
            double yawRad = Math.toRadians(yaw);

            // Transform world movement to local movement
            double localX = deltaX * Math.cos(-yawRad) - deltaZ * Math.sin(-yawRad);
            double localZ = deltaX * Math.sin(-yawRad) + deltaZ * Math.cos(-yawRad);

            // Determine movement direction
            forward = localZ > 0.001;
            backward = localZ < -0.001;
            right = localX > 0.001;
            left = localX < -0.001;

            // Detect sprint (faster movement)
            sprint = distance > 0.25 && !sneak;

            return true;
        } else {
            forward = false;
            backward = false;
            left = false;
            right = false;
            return false;
        }
    }

    /**
     * Reset movement state
     */
    public void reset() {
        forward = false;
        backward = false;
        left = false;
        right = false;
        jump = false;
        sneak = false;
        sprint = false;
        velocity = new Vector(0, 0, 0);
        onGround = true;
    }

    // Getters and setters
    public boolean isForward() { return forward; }
    public boolean isBackward() { return backward; }
    public boolean isLeft() { return left; }
    public boolean isRight() { return right; }
    public boolean isJump() { return jump; }
    public boolean isSneak() { return sneak; }
    public boolean isSprint() { return sprint; }
    public boolean isOnGround() { return onGround; }

    public void setForward(boolean forward) { this.forward = forward; }
    public void setBackward(boolean backward) { this.backward = backward; }
    public void setLeft(boolean left) { this.left = left; }
    public void setRight(boolean right) { this.right = right; }
    public void setJump(boolean jump) { this.jump = jump; }
    public void setSneak(boolean sneak) { this.sneak = sneak; }
    public void setSprint(boolean sprint) { this.sprint = sprint; }
    public void setOnGround(boolean onGround) { this.onGround = onGround; }

    public Vector getVelocity() { return velocity.clone(); }
    public void setVelocity(Vector velocity) { this.velocity = velocity.clone(); }

    public double getWalkSpeed() { return walkSpeed; }
    public void setWalkSpeed(double walkSpeed) { this.walkSpeed = walkSpeed; }

    public double getSprintSpeed() { return sprintSpeed; }
    public void setSprintSpeed(double sprintSpeed) { this.sprintSpeed = sprintSpeed; }

    public double getSneakSpeed() { return sneakSpeed; }
    public void setSneakSpeed(double sneakSpeed) { this.sneakSpeed = sneakSpeed; }

    public double getJumpPower() { return jumpPower; }
    public void setJumpPower(double jumpPower) { this.jumpPower = jumpPower; }
}