package it.fantozziforever.player;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** App Android nativa, senza WebView. */
public final class MainActivity extends Activity {
    private static final String PROGRAM = "https://www.raiplaysound.it/programmi/fantozziforever";
    private final String[] stream = new String[CatalogTitles.TITLES.length];
    private final String[] pages = new String[CatalogTitles.TITLES.length];
    private final long[] durationMs = new long[CatalogTitles.TITLES.length];
    private final List<Integer> filtered = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable progressTimer = new Runnable() {
        @Override public void run() {
            savePosition();
            updatePlayer();
            handler.postDelayed(this, 1500L);
        }
    };
    private SharedPreferences prefs;
    private UpdateChecker updater;
    private BaseAdapter adapter;
    private EditText search;
    private TextView count, selected, status, times;
    private SeekBar seek;
    private Button listen;
    private MediaController controller;
    private ListenableFuture<MediaController> controllerFuture;
    private boolean dragging;
    private int active = -1;

    private final Player.Listener listener = new Player.Listener() {
        @Override public void onEvents(Player player, Player.Events events) {
            if (events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) &&
                    player.getPlaybackState() == Player.STATE_ENDED) {
                int index = mediaIndex();
                if (index >= 0) prefs.edit().putBoolean("ascoltata_" + index, true)
                        .putLong("ripresa_" + index, 0).apply();
            }
            updatePlayer();
        }
        @Override public void onPlayerError(androidx.media3.common.PlaybackException error) {
            status.setText("Audio non riproducibile: puoi aprire la puntata su RaiPlay Sound");
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences("catalogo_fantozzi", MODE_PRIVATE);
        updater = new UpdateChecker(this);
        search = findViewById(R.id.search);
        count = findViewById(R.id.count);
        selected = findViewById(R.id.selected);
        status = findViewById(R.id.notice);
        times = findViewById(R.id.times);
        seek = findViewById(R.id.seek);
        listen = findViewById(R.id.listen);
        active = prefs.getInt("selezionata", -1);
        for (int i = 0; i < stream.length; i++) {
            pages[i] = PROGRAM;
            durationMs[i] = CatalogTitles.MINUTES[i] * 60_000L;
        }

        adapter = new BaseAdapter() {
            @Override public int getCount() { return filtered.size(); }
            @Override public Object getItem(int p) { return filtered.get(p); }
            @Override public long getItemId(int p) { return filtered.get(p); }
            @Override public View getView(int p, View recycled, ViewGroup parent) {
                View row = recycled == null ? LayoutInflater.from(MainActivity.this)
                        .inflate(R.layout.episode_row, parent, false) : recycled;
                int index = filtered.get(p);
                ((TextView)row.findViewById(R.id.title)).setText(
                        (index + 1) + ". " + CatalogTitles.TITLES[index]);
                boolean done = prefs.getBoolean("ascoltata_" + index, false);
                long position = prefs.getLong("ripresa_" + index, 0L);
                String meta = "27 mar 2025 · " + CatalogTitles.MINUTES[index] + " min"
                        + (done ? " · ✓ Ascoltata" : position > 0 ? " · Riprendi" : "");
                ((TextView)row.findViewById(R.id.meta)).setText(meta);
                TextView star = row.findViewById(R.id.star);
                star.setText(prefs.getBoolean("preferito_" + index, false) ? "★" : "☆");
                star.setOnClickListener(v -> {
                    prefs.edit().putBoolean("preferito_" + index,
                            !prefs.getBoolean("preferito_" + index, false)).apply();
                    updateList();
                });
                return row;
            }
        };
        ListView list = findViewById(R.id.episodes);
        list.setAdapter(adapter);
        list.setOnItemClickListener((p, v, position, id) -> choose(filtered.get(position)));
        listen.setOnClickListener(v -> toggle());
        findViewById(R.id.previous).setOnClickListener(v -> move(-1));
        findViewById(R.id.next).setOnClickListener(v -> move(1));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateList(); }
            @Override public void afterTextChanged(Editable e) {}
        });
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar s) { dragging = true; }
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {}
            @Override public void onStopTrackingTouch(SeekBar s) {
                dragging = false;
                if (controller != null && controller.getDuration() > 0) {
                    controller.seekTo(controller.getDuration() * s.getProgress() / 1000);
                    savePosition();
                }
            }
        });
        findViewById(R.id.menu).setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(this, v);
            menu.getMenu().add("Cerca puntate").setOnMenuItemClickListener(item -> {
                search.setVisibility(search.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                if (search.getVisibility() == View.VISIBLE) search.requestFocus();
                return true;
            });
            menu.getMenu().add("Aggiorna catalogo").setOnMenuItemClickListener(item -> {
                refreshCatalog(true); return true;
            });
            menu.getMenu().add("Aggiornamenti").setOnMenuItemClickListener(item -> {
                updater.check(true); return true;
            });
            menu.getMenu().add("Informazioni").setOnMenuItemClickListener(item -> {
                new AlertDialog.Builder(this).setTitle("Fantozzi Forever Player")
                        .setMessage("Versione 0.5.1 · App indipendente in italiano. "
                                + "Catalogo e audio provengono dalla piattaforma RaiPlay Sound.")
                        .setPositiveButton("Chiudi", null).show();
                return true;
            });
            menu.show();
        });
        if (active >= 0 && active < CatalogTitles.TITLES.length) choose(active);
        updateList();
        connectPlayer();
        refreshCatalog(false);
        updater.check(false);
    }

    private void connectPlayer() {
        SessionToken token = new SessionToken(this, new ComponentName(this, PlaybackService.class));
        controllerFuture = new MediaController.Builder(this, token).buildAsync();
        controllerFuture.addListener(() -> {
            if (isFinishing() || isDestroyed()) return;
            try {
                controller = controllerFuture.get();
                controller.addListener(listener);
                int index = mediaIndex();
                if (index >= 0) choose(index);
                updatePlayer();
            } catch (Exception e) {
                status.setText("Connessione al lettore Media3 non riuscita");
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void refreshCatalog(boolean manual) {
        status.setText("Aggiornamento collegamenti audio…");
        new Thread(() -> {
            try {
                List<RaiCatalogClient.Entry> data = RaiCatalogClient.load();
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    int available = 0;
                    for (RaiCatalogClient.Entry episode : data) {
                        int index = episode.index;
                        if (index < 0 || index >= stream.length) continue;
                        pages[index] = episode.webUrl;
                        if (!episode.audioUrl.isEmpty()) stream[index] = episode.audioUrl;
                        if (episode.durationMs > 0) durationMs[index] = episode.durationMs;
                    }
                    for (String url : stream) if (url != null && !url.isEmpty()) available++;
                    status.setText(available > 0
                            ? "Audio disponibile per " + available + " puntate"
                            : "Audio non disponibile · puoi aprire il sito ufficiale");
                    updatePlayer();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    status.setText("Catalogo locale pronto · sorgente audio non raggiungibile");
                    if (manual) Toast.makeText(this, "Impossibile aggiornare il catalogo", Toast.LENGTH_LONG).show();
                });
            }
        }, "catalogo-rai").start();
    }

    private void toggle() {
        if (active < 0 || active >= CatalogTitles.TITLES.length) {
            Toast.makeText(this, "Seleziona una puntata", Toast.LENGTH_SHORT).show();
            return;
        }
        if (stream[active] == null || stream[active].isEmpty()) {
            new AlertDialog.Builder(this).setTitle("Audio non disponibile nell'app")
                    .setMessage("Aprire la puntata nel sito ufficiale di RaiPlay Sound?")
                    .setNegativeButton("Annulla", null)
                    .setPositiveButton("Apri", (d, w) -> startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse(pages[active]))))
                    .show();
            return;
        }
        if (controller == null) {
            Toast.makeText(this, "Lettore audio in preparazione", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mediaIndex() == active) {
            if (controller.isPlaying()) controller.pause();
            else controller.play();
        } else {
            savePosition();
            controller.setMediaItem(new MediaItem.Builder().setUri(stream[active])
                    .setMediaId(Integer.toString(active))
                    .setMediaMetadata(new MediaMetadata.Builder()
                            .setTitle(CatalogTitles.TITLES[active])
                            .setArtist("Fantozzi Forever").build()).build());
            controller.prepare();
            long savedMs = prefs.getLong("ripresa_" + active, 0L);
            if (savedMs > 0L) controller.seekTo(savedMs);
            controller.play();
        }
        updatePlayer();
    }

    private void savePosition() {
        int index = mediaIndex();
        if (index < 0 || controller == null) return;
        if (controller.getPlaybackState() == Player.STATE_ENDED) {
            prefs.edit().putBoolean("ascoltata_" + index, true).putLong("ripresa_" + index, 0).apply();
        } else {
            long pos = Math.max(0, controller.getCurrentPosition());
            if (pos > 0) prefs.edit().putLong("ripresa_" + index, pos).apply();
        }
    }
    private int mediaIndex() {
        if (controller == null || controller.getCurrentMediaItem() == null) return -1;
        try {
            int index = Integer.parseInt(controller.getCurrentMediaItem().mediaId);
            return index >= 0 && index < CatalogTitles.TITLES.length ? index : -1;
        } catch (NumberFormatException e) { return -1; }
    }
    private void move(int delta) {
        if (active < 0) { choose(0); return; }
        boolean playing = controller != null && controller.isPlaying();
        int old = active;
        choose(Math.max(0, Math.min(CatalogTitles.TITLES.length - 1, active + delta)));
        if (playing && old != active) toggle();
    }
    private void choose(int index) {
        if (index < 0 || index >= CatalogTitles.TITLES.length) return;
        active = index;
        prefs.edit().putInt("selezionata", index).apply();
        selected.setText((index + 1) + ". " + CatalogTitles.TITLES[index]);
        updatePlayer();
        updateList();
    }
    private void updatePlayer() {
        if (active < 0 || active >= CatalogTitles.TITLES.length) {
            listen.setText("Seleziona una puntata");
            return;
        }
        boolean same = controller != null && mediaIndex() == active;
        listen.setText(stream[active] == null ? "Apri RaiPlay Sound"
                : same && controller.isPlaying() ? "❚❚ Pausa" : "▶ Ascolta");
        long position = same ? Math.max(0, controller.getCurrentPosition())
                : prefs.getLong("ripresa_" + active, 0L);
        long length = same && controller.getDuration() > 0 ? controller.getDuration() : durationMs[active];
        times.setText(format(position) + " / " + format(length));
        if (!dragging) seek.setProgress(length > 0 ? (int)Math.min(1000, position * 1000L / length) : 0);
        seek.setEnabled(same && controller.getDuration() > 0L);
    }
    private static String format(long ms) {
        long s = Math.max(0L, ms / 1000L);
        return String.format(Locale.ITALY, "%d:%02d", s / 60L, s % 60L);
    }
    private void updateList() {
        filtered.clear();
        String q = search.getText().toString().trim().toLowerCase(Locale.ITALIAN);
        for (int i = 0; i < CatalogTitles.TITLES.length; i++) {
            if (CatalogTitles.TITLES[i].toLowerCase(Locale.ITALIAN).contains(q)) filtered.add(i);
        }
        count.setText(filtered.size() + " di " + CatalogTitles.TITLES.length + " puntate");
        adapter.notifyDataSetChanged();
    }

    @Override protected void onResume() {
        super.onResume();
        if (updater != null) updater.onResume();
        handler.removeCallbacks(progressTimer);
        handler.post(progressTimer);
    }
    @Override protected void onPause() {
        handler.removeCallbacks(progressTimer);
        savePosition();
        super.onPause();
    }
    @Override protected void onDestroy() {
        handler.removeCallbacks(progressTimer);
        if (controller != null) controller.removeListener(listener);
        if (controllerFuture != null) MediaController.releaseFuture(controllerFuture);
        super.onDestroy();
    }
}
