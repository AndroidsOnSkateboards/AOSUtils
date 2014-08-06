package org.aosutils.android.loadingtask;

import android.app.Activity;
import android.app.Dialog;

public abstract class LoadingTaskActivity extends Activity implements AbstractLoadingTaskActivity {
	private int loadingTextResourceId = -1;
	
	@Override
	public void onPause() {
		LoadingTask.removeDialog(loadingTextResourceId, this);
		super.onPause();
	}
	
	@Override
	public Dialog onCreateDialog(int loadingTextResourceId) {
		this.loadingTextResourceId = loadingTextResourceId;
		return LoadingTask.showDialog(loadingTextResourceId, this);
	}
}
