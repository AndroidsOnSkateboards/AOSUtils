package org.aosutils.android;

import android.app.Activity;
import android.os.AsyncTask;

public abstract class LoadingTask<A, B, C> extends AsyncTask<Object, Object, Object> {
	private Activity activity;
	private int loadingTextResId;
	
	public LoadingTask(Activity activity, int loadingTextResId) {
		this.activity = activity;
		this.loadingTextResId = loadingTextResId;
	}
	
	@Override
	protected void onPreExecute() {
		//loadingDialog = ProgressDialog.show(activity, "", activity.getResources().getString(loadingTextResId), true);
		
		// Use this method so that Activity controls the dialog (and can close it when the activity is closed)
		activity.showDialog(loadingTextResId);
	}
	
	@Override
	protected abstract Object doInBackground(Object... params);
	
	@Override
	protected void onPostExecute(Object result) {
		/*
		// Would be null if Activity was destroyed (eg. screen rotated) while loading 
		if (loadingDialog.getCurrentFocus() != null) {
			loadingDialog.dismiss();
		}
		*/
		
		activity.removeDialog(loadingTextResId);
	}
	
	public Activity getActivity() {
		return activity;
	}
	
	public void setLoadingTextResourceId(int resourceId) {
		this.loadingTextResId = resourceId;
	}
	
	protected void toastOnUi(final String message) {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				AOSUtilsCommon.toast(message, activity);
			}
		});
	}
	
	protected void alertOnUi(final String message) {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				AOSUtilsCommon.alert(null, message, activity);
			}
		});
	}
}