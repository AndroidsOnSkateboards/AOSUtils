package org.aosutils.android.loadingtask;

import org.aosutils.android.AOSUtilsCommon;
import org.aosutils.android.R;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;

public abstract class LoadingTask<A, B, C> extends AsyncTask<Object, Object, Object> {
	private Activity loadingTaskActivity;
	private int loadingTextResId;
	
	public LoadingTask(Activity loadingTaskActivity) {
		this(loadingTaskActivity, R.string.dialog_Loading);
	}
	public LoadingTask(Activity loadingTaskActivity, int loadingTextResId) {
		this.loadingTaskActivity = loadingTaskActivity;
		this.loadingTextResId = loadingTextResId;
	}
	
	protected static ProgressDialog showDialog(int loadingTextResourceId, Activity loadingTaskActivity) {
		return ProgressDialog.show(loadingTaskActivity, "", loadingTaskActivity.getResources().getString(loadingTextResourceId), true);
	}
	protected static void removeDialog(int loadingTextResourceId, Activity loadingTaskActivity) {
		loadingTaskActivity.removeDialog(loadingTextResourceId);
	}
	
	@Override
	protected void onPreExecute() {
		//loadingDialog = ProgressDialog.show(activity, "", activity.getResources().getString(loadingTextResId), true);
		
		// Use this method so that Activity controls the dialog (and can close it when the activity is closed)
		if (! (loadingTaskActivity instanceof AbstractLoadingTaskActivity) ) {
			String message = this.getClass().getSimpleName() + " needs to be passed an Activity that implements " + AbstractLoadingTaskActivity.class.getSimpleName() + ". This is to ensure that onPause() and onCreateDialog() properly handle the Dialog. You may have your Activity extend " + LoadingTaskActivity.class.getSimpleName() + " or " + LoadingTaskPreferenceActivity.class.getSimpleName() + " or create your own implementation and have your Activity extend that.";
			Log.e("ERROR", message);
			System.exit(1);
		}
		loadingTaskActivity.showDialog(loadingTextResId);
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
		
		removeDialog(loadingTextResId, loadingTaskActivity);
	}
	
	public Activity getActivity() {
		return loadingTaskActivity;
	}
	
	public void setLoadingTextResourceId(int resourceId) {
		this.loadingTextResId = resourceId;
	}
	
	protected void toastOnUi(final String message) {
		loadingTaskActivity.runOnUiThread(new Runnable() {
			public void run() {
				AOSUtilsCommon.toast(message, loadingTaskActivity);
			}
		});
	}
	
	protected void alertOnUi(final String message) {
		loadingTaskActivity.runOnUiThread(new Runnable() {
			public void run() {
				AOSUtilsCommon.alert(null, message, loadingTaskActivity);
			}
		});
	}
}