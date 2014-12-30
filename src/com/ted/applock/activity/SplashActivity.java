package com.ted.applock.activity;

import android.content.Intent;
import android.os.Bundle;
import com.android.TedFramework.Activity.TActivity;
import com.ted.applock.R;
import com.ted.applock.base.TApplication;
import com.ted.applock.other.Constants;

/**
 * Created by Ted on 2014/11/20.
 */
public class SplashActivity extends TActivity{


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(TApplication.getInstance().isHasPwd()){
            /***/
            setContentView(R.layout.activity_splash_pattern_unlock);
        }else {
            /***/
            callCreatePwdPage();
        }
    }



    /***
     * 调用创建密码页面
     */
    private void callCreatePwdPage(){
        Intent intent = new Intent(this,CreatePwdActivity.class);
        startActivity(intent);
        finish();
    }



}
