package com.ted.applock.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.*;
import com.ted.applock.R;

/**
 * Created by Ted on 2014/11/20.
 */
public class MenuItemView extends RelativeLayout {
    private ImageView mLeftPic;
    private TextView mTitle;
    private LinearLayout mNewIconLayout;
    private String mTitltStr = "false";
    private boolean isHasIcon;
    private int mIconId;


    public MenuItemView(Context context) {
        super(context);
        initView(context);
    }

    public MenuItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Main_Menu_Item);
            mTitltStr = a.getString(R.styleable.Main_Menu_Item_title);
            isHasIcon = a.getBoolean(R.styleable.Main_Menu_Item_hasIcon, true);
            mIconId = a.getResourceId(R.styleable.Main_Menu_Item_icon, 0);
            a.recycle();
        } else {
            isHasIcon = true;
            mTitltStr = "";
            mIconId = 0;
        }
        updateView();
    }

    public MenuItemView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context);
    }

    private void updateView() {
        mTitle.setText(mTitltStr);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams)this.mTitle.getLayoutParams();
        if (isHasIcon && mIconId > 0) {
            mLeftPic.setVisibility(VISIBLE);
            mLeftPic.setImageResource(mIconId);
            layoutParams.leftMargin = (int)getContext().getResources().getDimension(R.dimen.menu_text_marginleft);
        } else {
            mLeftPic.setVisibility(GONE);
            layoutParams.leftMargin = (int)getContext().getResources().getDimension(R.dimen.menu_icon_marginleft);
        }
        mTitle.setLayoutParams(layoutParams);
    }

    private void initView(Context context) {
        View localView = View.inflate(context, R.layout.activity_menu_item_layout, this);
        this.mLeftPic = (ImageView) localView.findViewById(R.id.iv_menu_item_leftpic);
        this.mTitle = (TextView) localView.findViewById(R.id.tv_menu_item_direction);
        this.mNewIconLayout = (LinearLayout) localView.findViewById(R.id.ll_menu_new);
    }
}
