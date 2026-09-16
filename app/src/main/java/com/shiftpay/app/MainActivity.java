package com.shiftpay.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends Activity {
    private static final String PRIVACY_POLICY_URL =
            "https://evancachau.github.io/ShiftPay-Android/ShiftPay-privacy-policy.html";
    private static final String SMOKE_TAG = "ShiftPaySmoke";

    private WebView webView;
    private FrameLayout adContainer;
    private AdView adView;
    private InterstitialAd interstitialAd;
    private ConsentInformation consentInformation;
    private final AtomicBoolean adsInitialized = new AtomicBoolean(false);
    private boolean webPageReady = false;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF080B12);

        webView = new WebView(this);
        webView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setSupportZoom(false);

        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new AndroidAdsBridge(), "AndroidAds");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                webPageReady = true;
                updatePrivacyOptionsUi();
                runDebugSmokeTestIfRequested();
            }
        });

        adContainer = new FrameLayout(this);
        adContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(webView);
        root.addView(adContainer);
        setContentView(root);

        webView.loadDataWithBaseURL(
                "https://shiftpay.local/",
                readAsset("index.html"),
                "text/html",
                "UTF-8",
                null
        );

        requestConsentAndInitializeAds();
    }

    private void requestConsentAndInitializeAds() {
        consentInformation = UserMessagingPlatform.getConsentInformation(this);
        ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();

        consentInformation.requestConsentInfoUpdate(
                this,
                params,
                () -> {
                    updatePrivacyOptionsUi();

                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                            this,
                            formError -> {
                                updatePrivacyOptionsUi();
                                if (consentInformation.canRequestAds()) {
                                    initializeAds();
                                }
                            }
                    );

                    if (consentInformation.canRequestAds()) {
                        initializeAds();
                    }
                },
                requestError -> {
                    updatePrivacyOptionsUi();
                    if (consentInformation.canRequestAds()) {
                        initializeAds();
                    }
                }
        );
    }

    private boolean isPrivacyOptionsRequired() {
        return consentInformation != null
                && consentInformation.getPrivacyOptionsRequirementStatus()
                == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
    }

    private void updatePrivacyOptionsUi() {
        if (!webPageReady || webView == null) return;
        boolean required = isPrivacyOptionsRequired();
        runOnUiThread(() -> webView.evaluateJavascript(
                "window.setPrivacyOptionsRequired && window.setPrivacyOptionsRequired("
                        + required + ");",
                null
        ));
    }

    private void initializeAds() {
        if (!adsInitialized.compareAndSet(false, true)) return;

        MobileAds.initialize(this, status -> runOnUiThread(() -> {
            loadBanner();
            loadInterstitial();
        }));
    }

    private void loadBanner() {
        if (adView != null) {
            adView.destroy();
        }

        adView = new AdView(this);
        adView.setAdSize(AdSize.BANNER);
        adView.setAdUnitId(getString(R.string.admob_banner_id));

        adContainer.removeAllViews();
        adContainer.addView(adView);
        adView.loadAd(new AdRequest.Builder().build());
    }

    private void loadInterstitial() {
        InterstitialAd.load(
                this,
                getString(R.string.admob_interstitial_id),
                new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(InterstitialAd ad) {
                        interstitialAd = ad;
                        interstitialAd.setFullScreenContentCallback(
                                new FullScreenContentCallback() {
                                    @Override
                                    public void onAdDismissedFullScreenContent() {
                                        interstitialAd = null;
                                        loadInterstitial();
                                    }

                                    @Override
                                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                                        interstitialAd = null;
                                        loadInterstitial();
                                    }
                                }
                        );
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError error) {
                        interstitialAd = null;
                    }
                }
        );
    }

    public class AndroidAdsBridge {
        @JavascriptInterface
        public void showInterstitial() {
            runOnUiThread(() -> {
                if (interstitialAd != null) {
                    InterstitialAd ad = interstitialAd;
                    interstitialAd = null;
                    ad.show(MainActivity.this);
                } else if (consentInformation != null && consentInformation.canRequestAds()) {
                    loadInterstitial();
                }
            });
        }

        @JavascriptInterface
        public void showPrivacyOptions() {
            runOnUiThread(() -> UserMessagingPlatform.showPrivacyOptionsForm(
                    MainActivity.this,
                    formError -> updatePrivacyOptionsUi()
            ));
        }

        @JavascriptInterface
        public void openPrivacyPolicy() {
            runOnUiThread(() -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL));
                startActivity(intent);
            });
        }
    }

    private void runDebugSmokeTestIfRequested() {
        if (!BuildConfig.DEBUG) return;

        String stage = getIntent().getStringExtra("shiftpaySmoke");
        if (stage == null || stage.isBlank()) return;

        String js;
        switch (stage) {
            case "stage1":
                js = "(function(){try{"
                        + "localStorage.removeItem('shiftpay_android_v1');"
                        + "localStorage.removeItem('monthly_ad_count');"
                        + "document.querySelector('#shiftBtn').click();"
                        + "const s=JSON.parse(localStorage.getItem('shiftpay_android_v1')||'null');"
                        + "return (s&&s.activeShift)?'PASS':'FAIL_NO_ACTIVE';"
                        + "}catch(e){return 'ERROR_'+e.message;}})()";
                break;
            case "stage2":
                js = "(function(){try{"
                        + "const before=JSON.parse(localStorage.getItem('shiftpay_android_v1')||'null');"
                        + "if(!before||!before.activeShift)return 'FAIL_NO_PERSISTED_ACTIVE';"
                        + "document.querySelector('#shiftBtn').click();"
                        + "document.querySelector('#breakMinutes').value='0';"
                        + "const form=document.querySelector('#endForm');"
                        + "const saveBtn=form.querySelector('button[value=\"save\"]');"
                        + "form.requestSubmit(saveBtn);"
                        + "const after=JSON.parse(localStorage.getItem('shiftpay_android_v1')||'null');"
                        + "return (after&&!after.activeShift&&after.shifts&&after.shifts.length>=1)"
                        + "?'PASS':'FAIL_NOT_SAVED';"
                        + "}catch(e){return 'ERROR_'+e.message;}})()";
                break;
            case "stage3":
                js = "(function(){try{"
                        + "const s=JSON.parse(localStorage.getItem('shiftpay_android_v1')||'null');"
                        + "return (s&&!s.activeShift&&s.shifts&&s.shifts.length>=1)"
                        + "?'PASS':'FAIL_HISTORY_NOT_PERSISTED';"
                        + "}catch(e){return 'ERROR_'+e.message;}})()";
                break;
            default:
                Log.e(SMOKE_TAG, stage.toUpperCase() + "_UNKNOWN");
                return;
        }

        final String stageName = stage.toUpperCase();
        webView.evaluateJavascript(js, result -> {
            if (result != null && result.contains("PASS")) {
                Log.i(SMOKE_TAG, stageName + "_PASS");
            } else {
                Log.e(SMOKE_TAG, stageName + "_" + result);
            }
        });
    }

    private String readAsset(String name) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                getAssets().open(name), StandardCharsets.UTF_8))) {
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line).append('\n');
            }
            return out.toString();
        } catch (Exception e) {
            return "<html><body style='background:#080b12;color:white;font-family:sans-serif;padding:24px'>"
                    + "<h2>ShiftPay</h2><p>Impossible de charger l'interface.</p></body></html>";
        }
    }

    @Override
    protected void onDestroy() {
        if (adView != null) adView.destroy();
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
