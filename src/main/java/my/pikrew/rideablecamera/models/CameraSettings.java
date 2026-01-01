package my.pikrew.rideablecamera.models;

/**
 * Camera settings model
 * Contains all configurable camera parameters
 */
public class CameraSettings {

    private double distance;
    private double height;
    private double sideOffset;
    private double smoothness;
    private boolean copyEquipment;
    private boolean showNpcName;

    /**
     * Default constructor with default values
     */
    public CameraSettings() {
        this.distance = 3.5;
        this.height = 1.5;
        this.sideOffset = 0.0;
        this.smoothness = 0.15;
        this.copyEquipment = true;
        this.showNpcName = false;
    }

    /**
     * Constructor with custom values
     */
    public CameraSettings(double distance, double height, double sideOffset) {
        this.distance = distance;
        this.height = height;
        this.sideOffset = sideOffset;
        this.smoothness = 0.15;
        this.copyEquipment = true;
        this.showNpcName = false;
    }

    // Getters
    public double getDistance() {
        return distance;
    }

    public double getHeight() {
        return height;
    }

    public double getSideOffset() {
        return sideOffset;
    }

    public double getSmoothness() {
        return smoothness;
    }

    public boolean shouldCopyEquipment() {
        return copyEquipment;
    }

    public boolean shouldShowNpcName() {
        return showNpcName;
    }

    // Setters
    public void setDistance(double distance) {
        this.distance = Math.max(0.5, Math.min(distance, 10.0));
    }

    public void setHeight(double height) {
        this.height = Math.max(-2.0, Math.min(height, 10.0));
    }

    public void setSideOffset(double sideOffset) {
        this.sideOffset = Math.max(-5.0, Math.min(sideOffset, 5.0));
    }

    public void setSmoothness(double smoothness) {
        this.smoothness = Math.max(0.0, Math.min(smoothness, 1.0));
    }

    public void setCopyEquipment(boolean copyEquipment) {
        this.copyEquipment = copyEquipment;
    }

    public void setShowNpcName(boolean showNpcName) {
        this.showNpcName = showNpcName;
    }

    /**
     * Create a copy of these settings
     * @return New CameraSettings instance
     */
    public CameraSettings copy() {
        CameraSettings copy = new CameraSettings(distance, height, sideOffset);
        copy.setSmoothness(smoothness);
        copy.setCopyEquipment(copyEquipment);
        copy.setShowNpcName(showNpcName);
        return copy;
    }

    @Override
    public String toString() {
        return String.format("CameraSettings{distance=%.1f, height=%.1f, side=%.1f}",
                distance, height, sideOffset);
    }
}