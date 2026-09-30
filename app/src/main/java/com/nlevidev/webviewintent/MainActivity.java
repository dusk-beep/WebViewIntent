package com.nlevidev.webviewintent;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

  private static final String[] blacklistDomains = {
    "facebook.com",
    "instagram.com",
    "threads.net",
    "twitter.com",
    "x.com",
    "tiktok.com",
    "snapchat.com",
    "discord.com",
    "pinterest.com",
    "tumblr.com",
    "linkedin.com",
    "apkvision.org",
    "nxbrew.net",
    "ziperto.com",
    "yts.gg",
    "yts.bz",
    "anilist.co",
    "eden-emu.dev",
    "youtube.com",
    "m.youtube.com",
    "reddit.com",
    "safereddit.com",
    "red.artimeslena.eu",
    };
    private WebView webView;
    private ProgressBar progressBar;
    private TextView titleView;
    private String currentUrl = "";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        Objects.requireNonNull(getSupportActionBar()).hide();

        titleView = findViewById(R.id.actionbar_title);
        titleView.setOnClickListener(v -> copyUrlToClipboard());
        progressBar = findViewById(R.id.actionbar_progress);

        webView = findViewById(R.id.webview);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                updateTitle(url);
                if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                updateTitle(url);
                if (progressBar != null) progressBar.setVisibility(View.GONE);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();

              if (checkBlacklist(uri.toString())) {
                  return true;
              }

              if (isHttpUrl(uri)) {
                  return false;
              }

              return true;
            }
        });

        handleIntent(getIntent());
    }

    private void updateTitle(String url) {
        currentUrl = url;
        if (titleView != null) {
            titleView.setText(url);
        }
    }

    private void copyUrlToClipboard() {
        if (currentUrl == null || currentUrl.isEmpty()) return;

        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("URL", currentUrl);
        clipboard.setPrimaryClip(clip);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

      private boolean isBlacklisted(String url) {
        Uri uri = Uri.parse(url);
        String host = uri.getHost();

        if (host == null) {
            return false;
        }

        host = host.toLowerCase();

        for (String domain : blacklistDomains) {
            domain = domain.toLowerCase();

            if (host.equals(domain) || host.endsWith("." + domain)) {
                return true;
            }
        }

        return false;
    }

    private boolean checkBlacklist(String url) {
        if (isBlacklisted(url)) {
            showBlacklistToast();
            return true;
        }

        return false;
    }

    private void showBlacklistToast() {
      Toast.makeText(this, "Blacklisted site", Toast.LENGTH_LONG).show();
      finishAndRemoveTask();
    }

  private boolean isHttpUrl(Uri uri) {
    String scheme = uri.getScheme();
    return "http".equals(scheme) || "https".equals(scheme);
  }

  private void handleIntent(Intent intent) {
    Uri data = intent.getData();

    if (data != null) {
        if (!isHttpUrl(data) || checkBlacklist(data.toString())) {
            return;
        }

        webView.loadUrl(data.toString());
    } else {
        webView.loadUrl("about:blank");
    }
}

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
