package io.fullstack.oauth;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.facebook.react.bridge.ReactContext;
import com.github.scribejava.core.model.OAuth1AccessToken;

import java.lang.reflect.Method;
import java.util.Set;

import im.delight.android.webview.AdvancedWebView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OAuthManagerDialogFragment extends DialogFragment implements AdvancedWebView.Listener {

  private static final int WEBVIEW_TAG = 100001;
  private static final int WIDGET_TAG  = 100002;

    private static final String TAG = "OauthFragment";
    private OAuthManagerFragmentController mController;

    private ReactContext mReactContext;
    private AdvancedWebView mWebView;
    private ProgressBar mProgressBar;

    public static final OAuthManagerDialogFragment newInstance(
      final ReactContext reactContext,
      OAuthManagerFragmentController controller
    ) {
      Bundle args = new Bundle();
      OAuthManagerDialogFragment frag =
        new OAuthManagerDialogFragment(reactContext, controller);
      return frag;
    }

    public OAuthManagerDialogFragment(
      final ReactContext reactContext,
      OAuthManagerFragmentController controller
    ) {
      this.mController = controller;
      this.mReactContext = reactContext;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final Context context = mReactContext;
        LayoutParams rootViewLayoutParams = this.getFullscreenLayoutParams(context);

        RelativeLayout rootView = new RelativeLayout(context);

        mProgressBar = new ProgressBar(context);
        int dp50 = convertDpToPixel(50f, context);
        RelativeLayout.LayoutParams progressParams = new RelativeLayout.LayoutParams(dp50, dp50);
        progressParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        mProgressBar.setLayoutParams(progressParams);
        mProgressBar.setIndeterminate(true);

        getDialog().setCanceledOnTouchOutside(true);
        rootView.setLayoutParams(rootViewLayoutParams);

        Log.d(TAG, "Creating webview");
        mWebView = new AdvancedWebView(context);
        mWebView.setDesktopMode(false);
//        mWebView.setId(WEBVIEW_TAG);
        mWebView.setListener(this, this);
        // mWebView.setCookiesEnabled(false);
        mWebView.setVisibility(View.VISIBLE);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setDomStorageEnabled(true);
        // mWebView.getSettings().setUserAgentString("Mozilla/5.0 Google");
        mWebView.getSettings().setUserAgentString("Mozilla/5.0 (Linux; Android 6.0.1; SM-G920V Build/MMB29K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.98 Mobile Safari/537.36");
        // String userAgent = new WebView(mReactContext.getApplicationContext()).getSettings().getUserAgentString();
        // mWebView.getSettings().setUserAgentString(userAgent);

        LayoutParams layoutParams = this.getFullscreenLayoutParams(context);
        rootView.addView(mWebView, layoutParams);
        rootView.addView(mProgressBar,progressParams);

        // LinearLayout pframe = new LinearLayout(context);
        // pframe.setId(WIDGET_TAG);
        // pframe.setOrientation(LinearLayout.VERTICAL);
        // pframe.setVisibility(View.GONE);
        // pframe.setGravity(Gravity.CENTER);
        // pframe.setLayoutParams(layoutParams);

        // rootView.addView(pframe, layoutParams);

        this.setupWebView(mWebView);
        mController.getRequestTokenUrlAndLoad(mWebView);

        Log.d(TAG, "Loading view...");
        return rootView;
    }

    //  get full screen layout instead old function
        private LayoutParams getFullscreenLayoutParams(Context context) {
          int x, y, orientation = context.getResources().getConfiguration().orientation;
          WindowManager wm = ((WindowManager) 
              context.getSystemService(Context.WINDOW_SERVICE));
          Display display = wm.getDefaultDisplay();
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
              Point screenSize = new Point();
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                  display.getRealSize(screenSize);
                  x = screenSize.x;
                  y = screenSize.y;
              } else {
                  display.getSize(screenSize);
                  x = screenSize.x;
                  y = screenSize.y;
              }
          } else {
              x = display.getWidth();
              y = display.getHeight();
          }
      
          int width = getWidth(x, y, orientation);
          int height = getHeight(x, y, orientation);
          Log.w(TAG, "width: " + width + " height: " + height + " resolution: " + height /width);

      // return new LayoutParams(width, height - convertDpToPixel(50f,context));
      return new LayoutParams(width, height);
      }
    
      private int getWidth(int x, int y, int orientation) {
          return orientation == Configuration.ORIENTATION_PORTRAIT ? x : y;
      }
      
      private int getHeight(int x, int y, int orientation) {
          return orientation == Configuration.ORIENTATION_PORTRAIT ? y : x;
      }
    

    private void setupWebView(AdvancedWebView webView) {
      webView.setWebViewClient(new WebViewClient() {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
          return interceptUrl(view, url, true);
        }

          @Override
          public void onPageFinished(WebView view, String url) {
              super.onPageFinished(view, url);
              mProgressBar.setVisibility(View.GONE);
          }

          @Override
        public void onReceivedError(WebView view, int code, String desc, String failingUrl) {
          Log.i(TAG, "onReceivedError: " + failingUrl);
          super.onReceivedError(view, code, desc, failingUrl);
          onError(desc);
        }

        private boolean interceptUrl(WebView view, String url, boolean loadUrl) {
          Log.i(TAG, "interceptUrl called with url: " + url);

          // url would be http://localhost/twitter?denied=xxx when it's canceled
          Pattern p = Pattern.compile("\\S*denied\\S*");
          Matcher m = p.matcher(url);
          if(m.matches()){
            Log.i(TAG, "authentication is canceled");
            return false;
          }

          if (isCallbackUri(url, mController.getCallbackUrl())) {
            mController.getAccessToken(mWebView, url);
            return true;
          }

          if (loadUrl) {
            view.loadUrl(url);
          }

          return false;
        }
      });
    }

    public void setComplete(final OAuth1AccessToken accessToken) {
      Log.d(TAG, "Completed: " + accessToken);
    }


//    @Override
//    public void onDismiss(final DialogInterface dialog) {
//        super.onDismiss(dialog);
//        Log.d(TAG, "Dismissing dialog");
//    }


    // @Override
    // void onCancel(DialogInterface dialog) {
    //   Log.d(TAG, "onCancel called for dialog");
    //   onError("Cancelled");
    // }

    @SuppressLint("NewApi")
    @Override
    public void onResume() {
        super.onResume();
        mWebView.onResume();
        Log.d(TAG, "onResume called");
    }

    @SuppressLint("NewApi")
    @Override
    public void onPause() {
      Log.d(TAG, "onPause called");
        mWebView.onPause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mWebView.onDestroy();
        this.mController = null;
        // ...
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        mWebView.onActivityResult(requestCode, resultCode, intent);

        Log.d(TAG, "onActivityResult: " + requestCode);
        // ...
    }

    @Override
    public void onPageStarted(String url, Bitmap favicon) {
      Log.d(TAG, "onPageStarted " + url);
    }

    @Override
    public void onPageFinished(String url) {
      Log.d(TAG, "onPageFinished: " + url);
      // mController.onComplete(url);
    }

    @Override
    public void onPageError(int errorCode, String description, String failingUrl) {
      Log.e(TAG, "onPageError: " + failingUrl);
      mController.onError(errorCode, description, failingUrl);
    }

    @Override
    public void onDownloadRequested(String url, String suggestedFilename, String mimeType, long contentLength, String contentDisposition, String userAgent) { }

    @Override
    public void onExternalPageRequest(String url) {
      Log.d(TAG, "onExternalPageRequest: " + url);
    }

    private void onError(String msg) {
      Log.e(TAG, "Error: " + msg);
    }

    static boolean isCallbackUri(String uri, String callbackUrl) {
      Uri u = null;
      Uri r = null;
      try {
        u = Uri.parse(uri);
        r = Uri.parse(callbackUrl);
      } catch (NullPointerException e) {
        return false;
      }

      if (u == null || r == null) return false;

      boolean rOpaque = r.isOpaque();
      boolean uOpaque = u.isOpaque();
      if (uOpaque != rOpaque) return false;

      if (rOpaque) return TextUtils.equals(uri, callbackUrl);
      if (!TextUtils.equals(r.getScheme(), u.getScheme())) return false;
      if (u.getPort() != r.getPort()) return false;
      if (!TextUtils.isEmpty(r.getPath()) && !TextUtils.equals(r.getPath(), u.getPath())) return false;

      Set<String> paramKeys = r.getQueryParameterNames();
      for (String key : paramKeys) {
        if (!TextUtils.equals(r.getQueryParameter(key), u.getQueryParameter(key))) return false;
      }

      String frag = r.getFragment();
      if (!TextUtils.isEmpty(frag) && !TextUtils.equals(frag, u.getFragment())) return false;
      return true;
    }

    public static int convertDpToPixel(float dp, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return (int)px;
    }
}
