package shortestpath;

public final class ShortestPathPlugin {
    private static final int POH_MIN_X = 1856;
    private static final int POH_MAX_X = 2047;
    private static final int POH_MIN_Y = 5696;
    private static final int POH_MAX_Y = 5767;

    private ShortestPathPlugin() {
    }

    public static boolean isInsidePoh(int x, int y) {
        return x >= POH_MIN_X && x <= POH_MAX_X && y >= POH_MIN_Y && y <= POH_MAX_Y;
    }

    public static boolean override(String configOverrideKey, boolean defaultValue) {
        return defaultValue;
    }

    public static int override(String configOverrideKey, int defaultValue) {
        return defaultValue;
    }

    public static TeleportationItem override(String configOverrideKey, TeleportationItem defaultValue) {
        return defaultValue;
    }

    public static JewelleryBoxTier override(String configOverrideKey, JewelleryBoxTier defaultValue) {
        return defaultValue;
    }
}
