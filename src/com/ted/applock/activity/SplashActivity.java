package com.ted.applock.activity;

import android.content.Intent;
import android.os.Bundle;
import com.android.TedFramework.Activity.TActivity;
import com.ted.applock.R;
import com.ted.applock.other.Constants;

/**
 * Created by Ted on 2014/11/20.
 */
public class SplashActivity extends TActivity{


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_layout);
        initConfimData();
    }

    private void initConfimData(){
        Bundle bundle = new Bundle();
        bundle.putBoolean(Constants.KEY_IS_HAS_PWD,false);
        Intent intent = new Intent(this,ConfimPwdActivity.class);
        intent.putExtras(bundle);
        startActivity(intent);
        //finish();
    }




}
