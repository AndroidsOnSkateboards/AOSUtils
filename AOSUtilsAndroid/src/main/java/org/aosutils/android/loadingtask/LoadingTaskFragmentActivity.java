package org.aosutils.android.loadingtask;

import android.app.Dialog;
import android.support.v4.app.FragmentActivity;

public abstract class LoadingTaskFragmentActivity extends FragmentActivity implements AbstractLoadingTaskActivity {
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
