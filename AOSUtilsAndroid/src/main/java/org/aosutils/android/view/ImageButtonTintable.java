package org.aosutils.android.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.IdRes;
import android.util.AttributeSet;
import android.widget.ImageButton;

import org.aosutils.android.R;
import org.aosutils.android.utils.ViewUtils;

public class ImageButtonTintable extends ImageButton {
    Integer tintColor;

    public ImageButtonTintable(Context context) {
        super(context);
    }

    public ImageButtonTintable(Context context, AttributeSet attrs) {
        super(context, attrs);
        setTint(context, attrs);
    }

    public ImageButtonTintable(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setTint(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ImageButtonTintable(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setTint(context, attrs);
    }

    public void setTint(Integer color) {
        tintColor = color;
    }

    private void setTint(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ImageButtonTintable, 0, 0);
        try {
            int tintColor = ta.getColor(R.styleable.ImageButtonTintable_tint, -1);
            this.tintColor = tintColor == -1 ? null : tintColor;
        } finally {
            ta.recycle();
        }
    }

    @Override
    public void setImageResource(@IdRes int resId) {
        if (tintColor == null) {
            super.setImageResource(resId);
        }
        else {
            setImageDrawable(ViewUtils.getTintedDrawable(getContext(), resId, tintColor));
        }
    }
}
