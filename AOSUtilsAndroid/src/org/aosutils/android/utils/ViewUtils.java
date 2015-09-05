package org.aosutils.android.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;

public class ViewUtils {
	@SuppressLint("NewApi")
	private static class Lollipop {
		private static Lollipop _lollipop;
		
		private static Lollipop getInstance() {
			if (_lollipop == null) {
				_lollipop = new Lollipop();
			}
			
			return _lollipop;
		}
		
		private Drawable getDrawable(int resourceId, Context context) {
			return context.getDrawable(resourceId);
		}
	}
	
	@SuppressLint("NewApi")
	private static class Jellybean {
		private static Jellybean _jellybean;
		
		private static Jellybean getInstance() {
			if (_jellybean == null) {
				_jellybean = new Jellybean();
			}
			
			return _jellybean;
		}
		
		public void setBackground(Drawable drawable, View view) {
			view.setBackground(drawable);
		}
		
		public void removeOnGlobalLayoutListener(ViewTreeObserver observer, OnGlobalLayoutListener victim) {
			observer.removeOnGlobalLayoutListener(victim);
		}
	}
	
	@SuppressWarnings("deprecation")
	public static Drawable getDrawable(int resourceId, Context context) {
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
			return context.getResources().getDrawable(resourceId);
		}
		else {
			return Lollipop.getInstance().getDrawable(resourceId, context);
		}		
	}
	
	@SuppressWarnings("deprecation")
	public static void setBackground(Drawable drawable, View view) {
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
			view.setBackgroundDrawable(drawable);
		}
		else {
			Jellybean.getInstance().setBackground(drawable, view);
		}		
	}
	
	@SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    public static void addOnMeasuredListener(final View view, final Runnable listener) {
        if (view.getMeasuredHeight() != 0) {
            // Already measured, Run now
            listener.run();
        }
        else {
            // Not measured yet, set up a listener to run once the view is measured
            view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    listener.run();

                    // Remove listener
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                        view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    }
                    else {
                    	Jellybean.getInstance().removeOnGlobalLayoutListener(view.getViewTreeObserver(), this);
                    }
                }
            });
        }
    }
}
