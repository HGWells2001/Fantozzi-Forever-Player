import it.fantozziforever.player.CatalogTitles;

public final class CatalogTitlesTest {
    private static void check(boolean value) {
        if (!value) throw new AssertionError("Verifica catalogo fallita");
    }
    public static void main(String[] args) {
        check(CatalogTitles.TITLES.length == 10);
        check(CatalogTitles.MINUTES.length == 10);
        for (int i = 0; i < 10; i++) {
            check(CatalogTitles.indexOf((i + 1) + ". " + CatalogTitles.TITLES[i]) == i);
            check(CatalogTitles.indexOf(CatalogTitles.TITLES[i]) == i);
        }
        check(CatalogTitles.indexOf("Titolo inesistente") == -1);
        System.out.println("Catalogo 10 puntate verificato");
    }
}
