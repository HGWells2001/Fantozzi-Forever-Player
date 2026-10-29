package it.fantozziforever.player;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import android.webkit.WebSettings;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Button;
import android.webkit.RenderProcessGoneDetail;

public final class MainActivity extends Activity {
    private static final String HOME = "https://www.raiplaysound.it/programmi/fantozziforever";
    private WebView browser;
    private ProgressBar progress;
    private TextView status;
    private Button backward;
    private Button forward;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        progress = findViewById(R.id.loading);
        status = findViewById(R.id.status);
        browser = findViewById(R.id.browser);
        backward = findViewById(R.id.back);
        forward = findViewById(R.id.forward);

        WebSettings settings = browser.getSettings();
        settings.setJavaScriptEnabled(true); // RaiPlay Sound richiede JavaScript.
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);

        browser.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int percentage) {
                progress.setProgress(percentage);
                progress.setVisibility(percentage >= 100 ? View.GONE : View.VISIBLE);
            }
        });
        browser.setWebViewClient(new WebViewClient() {
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                status.setVisibility(View.GONE);
                updateNavigation();
            }
            @Override public void onPageFinished(WebView view, String url) { updateNavigation(); }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) showError(R.string.error_network);
            }
            @Override public void onReceivedHttpError(WebView view, WebResourceRequest request,
                    android.webkit.WebResourceResponse response) {
                if (request.isForMainFrame() && response.getStatusCode() >= 400) showError(R.string.error_page);
            }
            @Override public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                // Non trattenere una WebView il cui processo è terminato.
                ((android.view.ViewGroup)view.getParent()).removeView(view);
                view.destroy();
                browser = new WebView(MainActivity.this);
                ((android.widget.LinearLayout)findViewById(R.id.browser_holder)).addView(browser,
                        new android.widget.LinearLayout.LayoutParams(-1, -1));
                showError(R.string.error_page);
                // La nuova WebView non viene riutilizzata: riaprire l'Activity
                // evita di assumere configurazioni incomplete.
                findViewById(R.id.reload).setOnClickListener(v -> recreate());
                return true;
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                if (!"https".equalsIgnoreCase(scheme)) {
                    if ("intent".equalsIgnoreCase(scheme)) return true;
                    openExternal(uri);
                    return true;
                }
                String host = uri.getHost();
                if (host != null && (host.equals("raiplaysound.it") || host.endsWith(".raiplaysound.it")
                        || host.equals("rai.it") || host.endsWith(".rai.it"))) return false;
                openExternal(uri);
                return true;
            }
        });
        bind(R.id.home, () -> browser.loadUrl(HOME));
        bind(R.id.back, () -> { if (browser.canGoBack()) browser.goBack(); });
        bind(R.id.forward, () -> { if (browser.canGoForward()) browser.goForward(); });
        bind(R.id.reload, () -> browser.reload());
        bind(R.id.external, () -> openExternal(Uri.parse(browser.getUrl() == null ? HOME : browser.getUrl())));
        if (state == null || browser.restoreState(state) == null) browser.loadUrl(HOME);
        updateNavigation();
    }
    private void showError(int messageId) {
        status.setText(messageId);
        status.setVisibility(View.VISIBLE);
        progress.setVisibility(View.GONE);
    }
    private void updateNavigation() {
        backward.setEnabled(browser.canGoBack());
        forward.setEnabled(browser.canGoForward());
    }
    private void openExternal(Uri uri) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
        catch (Exception ignored) { showError(R.string.error_page); }
    }
    private void bind(int id, Runnable action) {
        ((Button)findViewById(id)).setOnClickListener(view -> action.run());
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        browser.saveState(state);
        super.onSaveInstanceState(state);
    }
    @Override public void onBackPressed() {
        if (browser.canGoBack()) browser.goBack();
        else super.onBackPressed();
    }
    @Override protected void onDestroy() {
        if (browser != null) browser.destroy();
        super.onDestroy();
    }
}
