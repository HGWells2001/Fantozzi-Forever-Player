package it.fantozziforever.player;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Button;

public final class MainActivity extends Activity {
    private static final String HOME = "https://www.raiplaysound.it/programmi/fantozziforever";
    private WebView browser;
    private ProgressBar progress;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        progress = findViewById(R.id.loading);
        browser = findViewById(R.id.browser);
        WebSettings settings = browser.getSettings();
        settings.setJavaScriptEnabled(true); // Il sito ufficiale richiede JavaScript.
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        browser.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int percentage) {
                progress.setProgress(percentage);
                progress.setVisibility(percentage >= 100 ? View.GONE : View.VISIBLE);
            }
        });
        browser.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (!"https".equalsIgnoreCase(uri.getScheme())) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); } catch (Exception ignored) { }
                    return true;
                }
                String host = uri.getHost();
                if (host != null && (host.equals("raiplaysound.it") || host.endsWith(".raiplaysound.it")
                        || host.equals("rai.it") || host.endsWith(".rai.it"))) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); } catch (Exception ignored) { }
                return true;
            }
        });
        bind(R.id.home, () -> browser.loadUrl(HOME));
        bind(R.id.back, () -> { if (browser.canGoBack()) browser.goBack(); });
        bind(R.id.forward, () -> { if (browser.canGoForward()) browser.goForward(); });
        bind(R.id.reload, () -> browser.reload());
        if (state == null) browser.loadUrl(HOME); else browser.restoreState(state);
    }
    private void bind(int id, Runnable action) { ((Button)findViewById(id)).setOnClickListener(view -> action.run()); }
    @Override protected void onSaveInstanceState(Bundle state) { browser.saveState(state); super.onSaveInstanceState(state); }
    @Override public void onBackPressed() { if (browser.canGoBack()) browser.goBack(); else super.onBackPressed(); }
    @Override protected void onDestroy() { browser.destroy(); super.onDestroy(); }
}
