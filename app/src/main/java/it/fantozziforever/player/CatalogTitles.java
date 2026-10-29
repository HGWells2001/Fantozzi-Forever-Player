package it.fantozziforever.player;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Catalogo di riserva verificato sulla pagina ufficiale del programma. */
public final class CatalogTitles {
    public static final String[] TITLES = {
        "La nascita di Fantozzi: dal cabaret alla televisione",
        "Paolo Villaggio in tv: Fantozzi, Kranz e Fracchia",
        "Fantozzi diventa un libro",
        "Fantozzi al cinema: l'inizio di una lunga saga",
        "Fantozzi e i coprotagonisti della saga",
        "Fantozzi: anatomia di una maschera",
        "Il dizionario di Fantozzi: universo letterario e stile linguistico",
        "Fantozzi e l'impatto sulla società italiana",
        "Paolo Villaggio uomo: aneddoti e leggende",
        "Fantozzi Forever: un mito popolare senza tempo"
    };
    public static final int[] MINUTES = {23,24,24,28,27,25,25,27,29,22};
    private static final Pattern PREFIX = Pattern.compile("^\\s*(\\d{1,2})\\s*\\.\\s*");
    private CatalogTitles() {}

    public static int indexOf(String title) {
        if (title == null) return -1;
        Matcher matcher = PREFIX.matcher(title);
        if (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1)) - 1;
            if (index >= 0 && index < TITLES.length) return index;
        }
        String normalized = normalize(title);
        for (int i = 0; i < TITLES.length; i++) {
            if (normalized.equals(normalize(TITLES[i]))) return i;
        }
        return -1;
    }
    private static String normalize(String title) {
        return title.toLowerCase(Locale.ITALIAN)
                .replaceAll("^[0-9]+[. ]+", "")
                .replaceAll("[^\\p{L}0-9]", "");
    }
}
