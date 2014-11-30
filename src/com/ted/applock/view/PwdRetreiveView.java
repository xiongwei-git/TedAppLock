package com.ted.applock.view;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import com.android.TedFramework.util.DeviceUtil;
import com.ted.applock.R;

/**
 * Created by Ted on 2014/11/30.
 */
public class PwdRetreiveView extends RelativeLayout implements View.OnClickListener {
    private Context mContext;
    private PopupWindow mPopupWindow;
    private ImageView mIcon;
    private View mView;
    private View.OnClickListener onClickListener;

    @Override
    public void onClick(View v) {
        if(null != this.onClickListener){
            this.onClickListener.onClick(v);
        }
        if (null != this.mPopupWindow && this.mPopupWindow.isShowing()) {
            hideView();
            return;
        }
        showView();
    }


    public PwdRetreiveView(Context context) {
        super(context);
        init(context);
    }

    public PwdRetreiveView(Context context, AttributeSet AttributeSet) {
        super(context, AttributeSet);
        init(context);
    }

    public PwdRetreiveView(Context context, AttributeSet AttributeSet, int Int) {
        super(context, AttributeSet, Int);
        init(context);
    }

    private void init(Context context) {
        this.mContext = context;
        this.mIcon = new ImageView(context);
        this.mIcon.setImageResource(R.drawable.btn_appbar_more);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        int i = DeviceUtil.getPixelFromDip(mContext, 8.0f);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        layoutParams.rightMargin = i;
        addView(this.mIcon, layoutParams);
        setOnClickListener(this);
    }

    public void hideView() {
        if ((this.mPopupWindow == null) || (!this.mPopupWindow.isShowing())) return;
        this.mPopupWindow.dismiss();
    }

    public void initContentView(View View) {
        this.mView = View;
        this.mPopupWindow = new PopupWindow(View, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        this.mPopupWindow.setBackgroundDrawable(new BitmapDrawable());
        this.mPopupWindow.setOutsideTouchable(true);
        this.mPopupWindow.setFocusable(true);
        this.mPopupWindow.setAnimationStyle(R.style.AnimationPopupwindow);
    }

    public void showView() {
        if ((this.mPopupWindow != null) && (!this.mPopupWindow.isShowing())) {
            if (this.mView == null) return;
            int j = View.MeasureSpec.makeMeasureSpec(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
            this.mView.measure(j, j);
        }
        int i = this.mView.getMeasuredWidth();
        this.mPopupWindow.showAsDropDown(this.mIcon, -i + this.mIcon.getWidth(), 0);
    }


    public void setOnClickListener(View.OnClickListener onClickListener) {
        if (onClickListener instanceof PwdRetreiveView) {
            super.setOnClickListener(onClickListener);
            return;
        }
        this.onClickListener = onClickListener;
    }
}