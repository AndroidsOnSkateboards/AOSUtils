package org.aosutils.android;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;

public abstract class LoadingTaskActivity extends Activity {
	private int loadingTextResourceId = -1;
	
	@Override 
	public void onPause() {
		removeDialog(loadingTextResourceId);
		super.onPause();
	}
	
	@Override
	public Dialog onCreateDialog(int loadingTextResourceId) {
		this.loadingTextResourceId = loadingTextResourceId;
		return ProgressDialog.show(this, "", getResources().getString(loadingTextResourceId), true);
	}
}
