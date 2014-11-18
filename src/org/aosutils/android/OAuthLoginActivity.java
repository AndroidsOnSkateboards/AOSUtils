package org.aosutils.android;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class OAuthLoginActivity extends Activity {
	public static String URL = "Url";
	public static String PARAMS_TO_RETURN = "ParamsToReturn";
	
	Activity activity;
	LinearLayout contentView;
	
	WebView webView;
	LinearLayout loadingView;
	
	/** Called when the activity is first created. */	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout ll = new LinearLayout(this);
        setContentView(ll);
        contentView = ll;
        
        activity = this;
        
        loadingView = new LinearLayout(this);
        ProgressBar progressBar = new ProgressBar(this);
        loadingView.addView(progressBar);
        TextView textView = new TextView(this);
        textView.setText(R.string.dialog_Loading);
        textView.setGravity(Gravity.CENTER);
        textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT));
        loadingView.addView(textView);
        ll.addView(loadingView);
        
        webView = new WebView(this);
        webView.getSettings().setSavePassword(false);
        webView.getSettings().setSaveFormData(false);
        
        Editor editor = AOSUtilsCommon.getDefaultSharedPreferences(activity).edit();
		editor.putBoolean(activity.getString(R.string.aosutils_pref_CookiesStored), true);
		editor.commit();
        
        Bundle extras = this.getIntent().getExtras();
        if (extras != null) {
        	openPageForRedirect(extras.getString(URL), extras.getStringArray(PARAMS_TO_RETURN));
        }
    }
    
    private void openPageForRedirect(String url, final String[] paramsToReturn) {
    	webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
            	// Hide loading and show page (on first page load)
            	if (contentView.equals(loadingView.getParent())) {
            		contentView.removeView(loadingView);
            		contentView.addView(webView);
            		
            		webView.requestFocus(View.FOCUS_DOWN);
            	}
            	
            	Uri uri = Uri.parse(url);
            	
            	boolean allParamsReturned = true;
            	for (String paramToReturn : paramsToReturn) {
            		if (uri.getQueryParameter(paramToReturn) == null) {
            			allParamsReturned = false;
            		}
            	}
            	
            	if (allParamsReturned) {
            		Intent dataToReturn = new Intent();
            		
            		for (String paramToReturn : paramsToReturn) {
            			dataToReturn.putExtra(paramToReturn, uri.getQueryParameter(paramToReturn));
            		}
            		
            		setResult(RESULT_OK, dataToReturn);
            		finish();
            	}
            }
        });
    	
        webView.loadUrl(url);
    }
    
}
