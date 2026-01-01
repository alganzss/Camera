package my.pikrew.rideablecamera.nms;

import org.bukkit.Bukkit;

/**
 * NMS Utility class for version detection and abstraction
 */
public class NMSUtil {

    private static final String VERSION;
    private static final int MAJOR_VERSION;
    private static final int MINOR_VERSION;
    private static final int PATCH_VERSION;

    static {
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        VERSION = packageName.substring(packageName.lastIndexOf('.') + 1);

        // Parse version like "1.21.4"
        String[] versionParts = Bukkit.getBukkitVersion().split("-")[0].split("\\.");
        MAJOR_VERSION = Integer.parseInt(versionParts[0]);
        MINOR_VERSION = Integer.parseInt(versionParts[1]);
        PATCH_VERSION = versionParts.length > 2 ? Integer.parseInt(versionParts[2]) : 0;
    }

    /**
     * Get NMS version string
     * @return Version string (e.g., "v1_21_R1")
     */
    public static String getVersion() {
        return VERSION;
    }

    /**
     * Get major version number
     * @return Major version (e.g., 1)
     */
    public static int getMajorVersion() {
        return MAJOR_VERSION;
    }

    /**
     * Get minor version number
     * @return Minor version (e.g., 21)
     */
    public static int getMinorVersion() {
        return MINOR_VERSION;
    }

    /**
     * Get patch version number
     * @return Patch version (e.g., 4)
     */
    public static int getPatchVersion() {
        return PATCH_VERSION;
    }

    /**
     * Check if server version is 1.21 or higher
     * @return true if 1.21+
     */
    public static boolean is1_21OrHigher() {
        return MINOR_VERSION >= 21;
    }

    /**
     * Check if server version is 1.20 or higher
     * @return true if 1.20+
     */
    public static boolean is1_20OrHigher() {
        return MINOR_VERSION >= 20;
    }

    /**
     * Get full version string
     * @return Full version (e.g., "1.21.4")
     */
    public static String getFullVersion() {
        return MAJOR_VERSION + "." + MINOR_VERSION + "." + PATCH_VERSION;
    }
}