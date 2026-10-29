import it.fantozziforever.player.ReleaseVersion;

public final class ReleaseVersionTest {
    private static void check(boolean pass) { if (!pass) throw new AssertionError("Confronto versioni errato"); }
    public static void main(String[] args) {
        check(ReleaseVersion.compare("v0.4.0", "0.3.0") > 0);
        check(ReleaseVersion.compare("v0.3.0", "0.3.0") == 0);
        check(ReleaseVersion.compare("v0.2.9", "0.3.0") < 0);
        check(ReleaseVersion.compare("v0.10.0", "0.9.99") > 0);
        check(ReleaseVersion.compare("v1.0.0", "0.99.99") > 0);
        check(!ReleaseVersion.valid("v0.4.0-beta"));
        check(!ReleaseVersion.valid("../4.0"));
        check(!ReleaseVersion.valid("v999999999999999.0.0"));
        System.out.println("Test versioni superati");
    }
}
