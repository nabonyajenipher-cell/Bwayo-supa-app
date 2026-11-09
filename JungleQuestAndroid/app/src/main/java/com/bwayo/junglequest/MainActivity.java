package com.bwayo.junglequest;
import android.app.Activity;import android.os.Bundle;import android.webkit.WebView;import android.webkit.WebSettings;import android.webkit.WebViewClient;import android.view.WindowManager;
public class MainActivity extends Activity { public void onCreate(Bundle b){super.onCreate(b);getWindow().setFlags(1024,1024);WebView w=new WebView(this);WebSettings s=w.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(true);w.setWebViewClient(new WebViewClient());setContentView(w);w.loadUrl("file:///android_asset/index.html");} }
