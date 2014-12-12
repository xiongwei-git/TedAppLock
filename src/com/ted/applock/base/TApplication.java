package com.ted.applock.base;

import android.app.Application;
import com.android.TedFramework.util.DeviceUtil;
import com.haibison.android.lockpattern.util.Settings;

/**
 * Created by Ted on 2014/12/2.
 */
public class TApplication extends Application{

    private static TApplication self = null;
    /**是否有密码记录在案*/
    private boolean bIsHasPwd = false;

    public static TApplication getInstance() {
        return self;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        self = this;
        DeviceUtil.initScreenParams(getResources());
        initPwdData();
    }

    /***
     * 初始化密码内容
     */
    private void initPwdData(){
        char[]mCurrentPattern = Settings.Security.getPattern(this);
        if(null == mCurrentPattern || mCurrentPattern.length == 0){
            bIsHasPwd = false;
        }else {
            bIsHasPwd = true;
        }
    }

    /***
     * 是否有密码记录在案
     * @return
     */
    public boolean isHasPwd() {
        return bIsHasPwd;
    }

    public void setHasPwd(boolean bIsHasPwd) {
        this.bIsHasPwd = bIsHasPwd;
    }
}
