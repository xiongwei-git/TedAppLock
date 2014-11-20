package com.ted.applock.activity;

import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import com.android.TedFramework.Activity.TActivity;
import com.android.TedFramework.util.CheckDoubleClick;
import com.android.TedFramework.util.ToastUtil;
import com.ted.applock.R;

public class MainActivity extends TActivity {
    private DrawerLayout mDrawerLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        initView();
    }

    private void initView(){
        mDrawerLayout = (DrawerLayout)findViewById(R.id.dl_main_layout_drawer_layout);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
    }

    public void menuItemOnclick(View view) {
        if (CheckDoubleClick.isFastDoubleClick()) return;
        ToastUtil.show(this,"点击了按钮！！！");
    }
}
