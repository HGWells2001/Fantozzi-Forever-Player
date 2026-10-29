package it.fantozziforever.player;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Carica il catalogo dall'endpoint JSON pubblico utilizzato da RaiPlay Sound.
 * L'endpoint non e' ufficialmente documentato: l'app conserva il catalogo
 * locale anche quando il server cambia formato.
 */
public final class RaiCatalogClient {
    private static final String ROOT = "https://www.raiplaysound.it";
    private static final String ENDPOINT = ROOT + "/programmi/fantozziforever.json";
    public static final class Entry {
        public final int index;
        public final String audioUrl;
        public final String webUrl;
        public final long durationMs;
        Entry(int i, String audio, String web, long duration) {
            index = i; audioUrl = audio; webUrl = web; durationMs = duration;
        }
    }
    private RaiCatalogClient() {}
    public static List<Entry> load() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(ENDPOINT).openConnection();
        conn.setConnectTimeout(12000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; FantozziForeverPlayer)");
        conn.setRequestProperty("Accept", "application/json");
        try {
            if (conn.getResponseCode() != 200) throw new IOException("Risposta Rai: " + conn.getResponseCode());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (InputStream in = conn.getInputStream()) {
                byte[] bytes = new byte[8192]; int n;
                while ((n = in.read(bytes)) != -1) {
                    if (out.size() + n > 3_000_000) throw new IOException("Catalogo troppo grande");
                    out.write(bytes, 0, n);
                }
            }
            return parse(out.toString("UTF-8"));
        } finally { conn.disconnect(); }
    }
    public static List<Entry> parse(String content) throws Exception {
        JSONObject root = new JSONObject(content);
        JSONObject block = root.optJSONObject("block");
        JSONArray cards = block == null ? null : block.optJSONArray("cards");
        if (cards == null) throw new IOException("Formato catalogo non riconosciuto");
        List<Entry> result = new ArrayList<>();
        for (int i = 0; i < cards.length(); i++) {
            JSONObject card = cards.optJSONObject(i);
            if (card == null) continue;
            int index = CatalogTitles.indexOf(card.optString("episode_title", card.optString("title")));
            if (index < 0) continue;
            JSONObject audio = card.optJSONObject("audio");
            JSONObject download = card.optJSONObject("downloadable_audio");
            String url = download == null ? "" : download.optString("url", "");
            if (!allowedMediaUrl(url)) url = audio == null ? "" : audio.optString("url", "");
            if (!allowedMediaUrl(url)) url = "";
            String page = card.optString("weblink", "");
            if (page.startsWith("/") && !page.startsWith("//")) page = ROOT + page;
            if (!page.startsWith(ROOT + "/")) page = ROOT + "/programmi/fantozziforever";
            long duration = parseDuration(audio == null ? "" : audio.optString("duration", ""));
            result.add(new Entry(index, url, page, duration));
        }
        return result;
    }
    public static long parseDuration(String raw) {
        if (raw == null || !raw.matches("\\d{1,3}:[0-5]\\d:[0-5]\\d")) return 0;
        String[] parts = raw.split(":");
        return (Long.parseLong(parts[0]) * 3600L
                + Long.parseLong(parts[1]) * 60L + Long.parseLong(parts[2])) * 1000L;
    }
    public static boolean allowedMediaUrl(String raw) {
        try {
            URL url = new URL(raw);
            String host = url.getHost().toLowerCase(Locale.ROOT);
            return "https".equalsIgnoreCase(url.getProtocol()) &&
                    (host.equals("rai.it") || host.endsWith(".rai.it")
                    || host.equals("raiplaysound.it") || host.endsWith(".raiplaysound.it")
                    || host.endsWith(".akamaized.net"));
        } catch (Exception e) { return false; }
    }
}
