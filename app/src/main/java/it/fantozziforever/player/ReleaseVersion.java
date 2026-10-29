package it.fantozziforever.player;

/** Confronto semantico tra versioni GitHub vX.Y.Z e versionName Android. */
public final class ReleaseVersion {
    private ReleaseVersion() {}
    public static boolean valid(String version) { return parse(version) != null; }
    public static int compare(String a, String b) {
        int[] left = parse(a);
        int[] right = parse(b);
        if (left == null) return -1;
        if (right == null) return 1;
        for (int i = 0; i < 3; i++) {
            if (left[i] != right[i]) return Integer.compare(left[i], right[i]);
        }
        return 0;
    }
    private static int[] parse(String version) {
        if (version == null) return null;
        String text = version.startsWith("v") ? version.substring(1) : version;
        if (!text.matches("[0-9]+\\.[0-9]+\\.[0-9]+")) return null;
        String[] parts = text.split("\\.");
        int[] result = new int[3];
        try {
            for (int i = 0; i < 3; i++) result[i] = Integer.parseInt(parts[i]);
            return result;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
