package it.fantozziforever.player;

import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

/** Servizio Media3: mantiene il lettore in background e nei controlli Android. */
public final class PlaybackService extends MediaSessionService {
    private ExoPlayer player;
    private MediaSession session;

    @Override public void onCreate() {
        super.onCreate();
        player = new ExoPlayer.Builder(this).build();
        player.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(), true);
        player.setHandleAudioBecomingNoisy(true);
        session = new MediaSession.Builder(this, player).build();
    }

    @Nullable @Override public MediaSession onGetSession(MediaSession.ControllerInfo info) {
        return session;
    }

    @Override public void onDestroy() {
        if (session != null) { session.release(); session = null; }
        if (player != null) { player.release(); player = null; }
        super.onDestroy();
    }
}
