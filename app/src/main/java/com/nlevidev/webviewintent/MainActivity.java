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
import android.webkit.WebSettings;
import android.webkit.JavascriptInterface;
import android.widget.Toast;
import android.widget.PopupMenu;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

  private void showLinkMenu(String url) {
    if (url == null || url.isEmpty()) {
      return;
    }

    PopupMenu menu = new PopupMenu(this, titleView);
    menu.getMenu().add("Copy link address");

    menu.setOnMenuItemClickListener(item -> {
      ClipboardManager clipboard =
    (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

      clipboard.setPrimaryClip(
        ClipData.newPlainText("URL", url)
      );

      Toast.makeText(this, "Link copied", Toast.LENGTH_SHORT).show();
      return true;
    });

    menu.show();
  }

  private class LinkHandler {
    @JavascriptInterface
    public void onLongPress(String url) {
      runOnUiThread(() -> showLinkMenu(url));
    }
  }

  private void installLinkLongPressHandler() {
    String js = """
    (function() {
    if (window.__linkLongPressInstalled) return;

    window.__linkLongPressInstalled = true;

    document.addEventListener('contextmenu', function(e) {
    var a = e.target.closest('a[href]');
    if (!a) return;

    e.preventDefault();
    LinkHandler.onLongPress(a.href);
    }, true);
    })();
    """;

    webView.evaluateJavascript(js, null);
  }

  private static final Set<String> BLACKLIST_DOMAINS = Set.of(
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
    "youtube.com",
    "reddit.com",
    "safereddit.com",
    "red.artimeslena.eu",
    "gamebounty.world"
  );

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
    getSupportActionBar().hide();

    titleView = findViewById(R.id.actionbar_title);
    titleView.setOnClickListener(v -> copyUrlToClipboard());
    progressBar = findViewById(R.id.actionbar_progress);

    webView = findViewById(R.id.webview);

    WebSettings settings = webView.getSettings();

    settings.setJavaScriptEnabled(true);
    settings.setDomStorageEnabled(true);
    settings.setAllowFileAccess(false);
    settings.setAllowContentAccess(false);
    settings.setSupportMultipleWindows(false);
    settings.setJavaScriptCanOpenWindowsAutomatically(false);

    webView.addJavascriptInterface(new LinkHandler(), "LinkHandler");

    webView.setWebViewClient(new WebViewClient() {

      @Override
      public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
        updateTitle(url);
        progressBar.setVisibility(View.VISIBLE);
      }

      @Override
      public void onPageFinished(WebView view, String url) {
        updateTitle(url);
        progressBar.setVisibility(View.GONE);
        installLinkLongPressHandler();
      }

      @Override
      public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        Uri uri = request.getUrl();

        if (checkBlacklist(uri)) {
          return true;
        }

        return !isHttpUrl(uri);
      }
    });

    handleIntent(getIntent());

    getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
      @Override
      public void handleOnBackPressed() {
        if (webView.canGoBack()) {
          webView.goBack();
        } else {
          finish();
        }
      }
    });
  }

  private void updateTitle(String url) {
    currentUrl = url;
    titleView.setText(url);
  }

  private void copyUrlToClipboard() {
    if (currentUrl.isEmpty()) {
      return;
    }

    ClipboardManager clipboard =
  (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

    ClipData clip = ClipData.newPlainText("URL", currentUrl);
    clipboard.setPrimaryClip(clip);
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    handleIntent(intent);
  }

  private boolean isBlacklisted(Uri uri) {
    String host = uri.getHost();

    if (host == null) {
      return false;
    }

    host = host.toLowerCase(Locale.ROOT);

    if (BLACKLIST_DOMAINS.contains(host)) {
      return true;
    }

    for (String domain : BLACKLIST_DOMAINS) {
      if (host.endsWith("." + domain)) {
        return true;
      }
    }

    return false;
  }

  private boolean checkBlacklist(Uri uri) {
    if (isBlacklisted(uri)) {
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
      if (!isHttpUrl(data) || checkBlacklist(data)) {
        return;
      }

      webView.loadUrl(data.toString());
    } else {
      webView.loadUrl("about:blank");
    }
  }
}
