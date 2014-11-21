package com.ted.applock.activity;

import android.content.Intent;
import android.os.Bundle;
import com.android.TedFramework.Activity.TActivity;
import com.android.TedFramework.util.LogUtil;
import com.haibison.android.lockpattern.LockPatternActivity;

/**
 * Created by Ted on 2014/11/21.
 */
public class ConfimPwdActivity extends TActivity {
    private static final int REQ_CREATE_PATTERN = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createPattern();
    }

    private void createPattern(){
        Intent intent = new Intent(SplashPatternActivity.ACTION_CREATE_PATTERN, null,getApplicationContext(), SplashPatternActivity.class);
        startActivityForResult(intent, REQ_CREATE_PATTERN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQ_CREATE_PATTERN: {
                if (resultCode == RESULT_OK) {
                    char[] pattern = data.getCharArrayExtra(SplashPatternActivity.EXTRA_PATTERN);
                    //LogUtil.e(pattern.toString());
                    finish();
                }else {
                    finish();
                }
                break;
            }
        }

    }
}
