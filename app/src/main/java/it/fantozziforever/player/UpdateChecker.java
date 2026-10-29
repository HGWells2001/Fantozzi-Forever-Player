package it.fantozziforever.player;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/** Gestisce gli aggiornamenti senza una WebView e senza permessi di archiviazione. */
public final class UpdateChecker {
    private static final String RELEASES =
            "https://api.github.com/repos/HGWells2001/Fantozzi-Forever-Player/releases?per_page=20";
    private static final String DOWNLOAD_PREFIX =
            "https://github.com/HGWells2001/Fantozzi-Forever-Player/releases/download/";
    private static final String MIME_APK = "application/vnd.android.package-archive";
    private static final long MAX_APK_BYTES = 300L * 1024L * 1024L;
    private final Activity activity;
    private volatile boolean checking;
    private volatile boolean downloading;
    private File pendingInstallation;

    public UpdateChecker(Activity activity) { this.activity = activity; }

    public void check(boolean manual) {
        if (checking || downloading) return;
        checking = true;
        new Thread(() -> {
            try {
                UpdateInfo next = latestUpdate();
                ui(() -> {
                    checking = false;
                    if (next != null) {
                        showAvailable(next);
                    } else if (manual) {
                        message("Aggiornamenti", "Hai già la versione più recente disponibile.");
                    }
                });
            } catch (Exception e) {
                ui(() -> {
                    checking = false;
                    if (manual) message("Aggiornamenti", "Controllo non riuscito. Verifica la connessione e riprova.\n\n" + e.getMessage());
                });
            }
        }, "controllo-aggiornamenti").start();
    }

    public void onResume() {
        if (pendingInstallation != null && activity.getPackageManager().canRequestPackageInstalls()) {
            File apk = pendingInstallation;
            pendingInstallation = null;
            install(apk);
        }
    }

    private UpdateInfo latestUpdate() throws Exception {
        HttpURLConnection connection = open(RELEASES);
        String response;
        try {
            if (connection.getResponseCode() != 200) throw new IOException("GitHub: HTTP " + connection.getResponseCode());
            try (InputStream input = connection.getInputStream()) {
                byte[] buffer = new byte[4096];
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                int n;
                while ((n = input.read(buffer)) >= 0) {
                    if (out.size() + n > 2_000_000) throw new IOException("Risposta troppo grande");
                    out.write(buffer, 0, n);
                }
                response = out.toString("UTF-8");
            }
        } finally { connection.disconnect(); }

        String installed = activity.getPackageManager()
                .getPackageInfo(activity.getPackageName(), 0).versionName;
        JSONArray releases = new JSONArray(response);
        UpdateInfo selected = null;
        for (int i = 0; i < releases.length(); i++) {
            JSONObject release = releases.getJSONObject(i);
            if (release.optBoolean("draft") || ReleaseVersion.compare(
                    release.optString("tag_name"), installed) <= 0) continue;
            String version = release.optString("tag_name");
            if (!ReleaseVersion.valid(version)) continue;
            JSONArray assets = release.optJSONArray("assets");
            if (assets == null) continue;
            for (int j = 0; j < assets.length(); j++) {
                JSONObject asset = assets.getJSONObject(j);
                String name = asset.optString("name");
                String url = asset.optString("browser_download_url");
                String digest = asset.optString("digest");
                long size = asset.optLong("size");
                if (!name.matches("FantozziForever_v[0-9]+\\.[0-9]+\\.[0-9]+_(debug|release)\\.apk")
                        || !name.startsWith("FantozziForever_" + version + "_")
                        || !url.startsWith(DOWNLOAD_PREFIX)
                        || !url.endsWith("/" + name)
                        || !digest.matches("sha256:[a-fA-F0-9]{64}")
                        || size <= 0 || size > MAX_APK_BYTES) continue;
                UpdateInfo candidate = new UpdateInfo(version, url, digest.substring(7), size,
                        name.endsWith("_debug.apk"));
                if (selected == null || ReleaseVersion.compare(candidate.version, selected.version) > 0
                        || (ReleaseVersion.compare(candidate.version, selected.version) == 0
                        && selected.debug && !candidate.debug)) selected = candidate;
            }
        }
        return selected;
    }

    private void showAvailable(UpdateInfo info) {
        String warning = info.debug
                ? "\n\nÈ una build di prova: prima di installarla verrà verificata la compatibilità della firma."
                : "\n\nL'installazione manterrà i dati soltanto se la firma corrisponde.";
        new AlertDialog.Builder(activity)
                .setTitle("Aggiornamento " + info.version)
                .setMessage("È disponibile una versione più recente di Fantozzi Forever."
                        + warning + "\n\nVuoi scaricarla?")
                .setNegativeButton("Più tardi", null)
                .setPositiveButton("Scarica", (dialog, which) -> download(info))
                .show();
    }

    private void download(UpdateInfo info) {
        if (downloading) return;
        downloading = true;
        AtomicBoolean canceled = new AtomicBoolean(false);
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        int space = (int) (20 * activity.getResources().getDisplayMetrics().density);
        content.setPadding(space, space, space, space);
        TextView label = new TextView(activity);
        label.setText("Preparazione del download…");
        ProgressBar bar = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        content.addView(label);
        content.addView(bar);
        AlertDialog progressDialog = new AlertDialog.Builder(activity)
                .setTitle("Download aggiornamento")
                .setView(content)
                .setNegativeButton("Annulla", (d, w) -> canceled.set(true))
                .create();
        progressDialog.setOnCancelListener(d -> canceled.set(true));
        progressDialog.show();

        new Thread(() -> {
            File partial = null;
            try {
                File folder = new File(activity.getCacheDir(), "aggiornamenti");
                if (!folder.exists() && !folder.mkdirs()) throw new IOException("Cartella non accessibile");
                partial = new File(folder, "FantozziForever.apk.part");
                File completed = new File(folder, "FantozziForever.apk");
                if (partial.exists() && !partial.delete()) throw new IOException("Download temporaneo bloccato");
                if (completed.exists() && !completed.delete()) throw new IOException("APK precedente bloccato");
                MessageDigest sha = MessageDigest.getInstance("SHA-256");
                HttpURLConnection connection = open(info.url);
                try {
                    if (connection.getResponseCode() != 200) throw new IOException("Download: HTTP " + connection.getResponseCode());
                    long received = 0;
                    int lastPercent = -1;
                    try (InputStream input = connection.getInputStream();
                         FileOutputStream output = new FileOutputStream(partial)) {
                        byte[] buffer = new byte[32768];
                        int n;
                        while ((n = input.read(buffer)) != -1) {
                            if (canceled.get()) throw new IOException("Download annullato");
                            received += n;
                            if (received > MAX_APK_BYTES || received > info.size)
                                throw new IOException("Dimensione APK non valida");
                            sha.update(buffer, 0, n);
                            output.write(buffer, 0, n);
                            int percent = (int) ((received * 100L) / info.size);
                            if (percent != lastPercent) {
                                lastPercent = percent;
                                final int progress = percent;
                                ui(() -> {
                                    bar.setProgress(progress);
                                    label.setText("Download: " + progress + "%");
                                });
                            }
                        }
                    }
                    if (received != info.size) throw new IOException("Download incompleto");
                } finally { connection.disconnect(); }
                if (canceled.get()) throw new IOException("Download annullato");
                if (!hex(sha.digest()).equalsIgnoreCase(info.sha256))
                    throw new IOException("Verifica SHA-256 fallita. File scartato.");
                if (!partial.renameTo(completed)) throw new IOException("Impossibile preparare l'APK");
                verifyPackage(completed);
                ui(() -> {
                    downloading = false;
                    progressDialog.dismiss();
                    new AlertDialog.Builder(activity)
                            .setTitle("Download verificato")
                            .setMessage("L'APK è integro e la firma è compatibile con l'app installata. Aprire l'installazione?")
                            .setNegativeButton("Non ora", null)
                            .setPositiveButton("Installa", (d, w) -> install(completed))
                            .show();
                });
            } catch (Exception e) {
                if (partial != null) partial.delete();
                ui(() -> {
                    downloading = false;
                    progressDialog.dismiss();
                    if (!canceled.get())
                        message("Aggiornamento non installabile", e.getMessage()
                                + "\n\nL'app attuale e i suoi dati restano invariati.");
                });
            }
        }, "download-aggiornamento").start();
    }

    private void verifyPackage(File apk) throws Exception {
        PackageManager pm = activity.getPackageManager();
        int flags = Build.VERSION.SDK_INT >= 28
                ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES;
        PackageInfo oldApp = pm.getPackageInfo(activity.getPackageName(), flags);
        PackageInfo candidate = pm.getPackageArchiveInfo(apk.getAbsolutePath(), flags);
        if (candidate == null || !activity.getPackageName().equals(candidate.packageName))
            throw new IOException("L'APK non appartiene a Fantozzi Forever Player.");
        long oldCode = Build.VERSION.SDK_INT >= 28 ? oldApp.getLongVersionCode() : oldApp.versionCode;
        long newCode = Build.VERSION.SDK_INT >= 28 ? candidate.getLongVersionCode() : candidate.versionCode;
        if (newCode <= oldCode) throw new IOException("La versione scaricata non è più recente.");
        Signature[] oldSignatures = signatures(oldApp);
        Signature[] newSignatures = signatures(candidate);
        if (oldSignatures == null || newSignatures == null || oldSignatures.length == 0
                || !Arrays.equals(oldSignatures, newSignatures))
            throw new IOException("Firma diversa: Android non può aggiornare questa installazione. Non disinstallare l'app se vuoi conservare i tuoi dati.");
    }

    private static Signature[] signatures(PackageInfo info) {
        if (Build.VERSION.SDK_INT >= 28) {
            return info.signingInfo == null ? null : info.signingInfo.getApkContentsSigners();
        }
        return info.signatures;
    }

    private void install(File apk) {
        if (!activity.getPackageManager().canRequestPackageInstalls()) {
            pendingInstallation = apk;
            new AlertDialog.Builder(activity)
                    .setTitle("Autorizzazione Android")
                    .setMessage("Per aggiornare l'app devi autorizzare Fantozzi Forever a installare APK. Android ti chiederà di confermare ogni installazione.")
                    .setNegativeButton("Annulla", (d, w) -> pendingInstallation = null)
                    .setPositiveButton("Apri impostazioni", (d, w) -> {
                        Intent settings = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                Uri.parse("package:" + activity.getPackageName()));
                        try { activity.startActivity(settings); }
                        catch (Exception e) { message("Installazione", "Impossibile aprire le impostazioni di Android."); }
                    }).show();
            return;
        }
        try {
            Uri uri = FileProvider.getUriForFile(activity,
                    activity.getPackageName() + ".fileprovider", apk);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, MIME_APK);
            intent.setClipData(ClipData.newUri(activity.getContentResolver(), "APK aggiornamento", uri));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(intent);
        } catch (Exception e) {
            message("Installazione", "Impossibile aprire l'installazione: " + e.getMessage());
        }
    }

    private static HttpURLConnection open(String initialUrl) throws IOException {
        URL next = new URL(initialUrl);
        for (int i = 0; i < 7; i++) {
            if (!"https".equalsIgnoreCase(next.getProtocol()) || !trustedHost(next.getHost()))
                throw new IOException("Indirizzo di download non autorizzato");
            HttpURLConnection connection = (HttpURLConnection) next.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(25000);
            connection.setRequestProperty("User-Agent", "FantozziForeverPlayer-Android");
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            int status = connection.getResponseCode();
            if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                String location = connection.getHeaderField("Location");
                connection.disconnect();
                if (location == null) throw new IOException("Reindirizzamento senza destinazione");
                next = new URL(next, location);
                continue;
            }
            return connection;
        }
        throw new IOException("Troppi reindirizzamenti");
    }

    private static boolean trustedHost(String host) {
        return host.equalsIgnoreCase("api.github.com")
                || host.equalsIgnoreCase("github.com")
                || host.equalsIgnoreCase("release-assets.githubusercontent.com")
                || host.equalsIgnoreCase("objects.githubusercontent.com");
    }

    private static String hex(byte[] data) {
        StringBuilder result = new StringBuilder(data.length * 2);
        for (byte b : data) result.append(String.format(Locale.ROOT, "%02x", b & 0xff));
        return result.toString();
    }

    private void message(String title, String text) {
        new AlertDialog.Builder(activity).setTitle(title).setMessage(text)
                .setPositiveButton("Chiudi", null).show();
    }

    private void ui(Runnable action) {
        activity.runOnUiThread(() -> {
            if (!activity.isFinishing() && !activity.isDestroyed()) action.run();
        });
    }

    private static final class UpdateInfo {
        final String version, url, sha256;
        final long size;
        final boolean debug;
        UpdateInfo(String version, String url, String sha256, long size, boolean debug) {
            this.version = version;
            this.url = url;
            this.sha256 = sha256;
            this.size = size;
            this.debug = debug;
        }
    }
}
