package com.ted.applock.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.TedFramework.util.ToastUtil;
import com.haibison.android.lockpattern.util.IEncrypter;
import com.haibison.android.lockpattern.util.InvalidEncrypterException;
import com.haibison.android.lockpattern.util.LoadingDialog;
import com.haibison.android.lockpattern.util.Settings;
import com.haibison.android.lockpattern.util.Settings.Display;
import com.haibison.android.lockpattern.util.Settings.Security;
import com.haibison.android.lockpattern.widget.LockPatternUtils;
import com.haibison.android.lockpattern.widget.LockPatternView;
import com.haibison.android.lockpattern.widget.LockPatternView.Cell;
import com.haibison.android.lockpattern.widget.LockPatternView.DisplayMode;
import com.ted.applock.R;
import com.ted.applock.base.TApplication;
import com.ted.applock.view.PwdRetreiveView;
import java.util.Arrays;
import java.util.List;

import static com.haibison.android.lockpattern.util.Settings.Display.*;
import static com.haibison.android.lockpattern.util.Settings.Security.METADATA_AUTO_SAVE_PATTERN;
import static com.haibison.android.lockpattern.util.Settings.Security.METADATA_ENCRYPTER_CLASS;

public class CreatePwdActivity extends Activity {
    /**通知做动画的消息*/
    private final int MSG_TO_DO_ANIMANATION = 0x001;

    /***
     * 密码方式，包括图形解锁和PIN码
     */
    private enum SecurityOption{
        PATTERN,PIN
    }

    /**当前密码的状态，三种 创建，对比，认证*/
    private enum SecurityState{
        CREATE,COMPARE,VERIFY
    }

    /**
     * Delay time to reload the lock pattern view after a wrong pattern.
     */
    private static final long DELAY_TIME_TO_RELOAD_LOCK_PATTERN_VIEW = DateUtils.SECOND_IN_MILLIS;
    private int mMaxRetries, mMinWiredDots, mRetryCount = 0, mCaptchaWiredDots;
    private boolean mAutoSave, mStealthMode;
    private long mLastClickTime = 0l;
    private IEncrypter mEncrypter;
    private SecurityOption mSEOption;
    private SecurityState mSEState;
    /**是否是第一次创建密码*/
    private boolean bIsFirstCreate;
    private char[] mCurrentPattern;
    private TextView mTextInfo;
    private PwdRetreiveView mMoreHelpView;
    private RelativeLayout mLogoView;
    private LockPatternView mLockPatternView;

    /***
     *
     */
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case MSG_TO_DO_ANIMANATION:
                    doWelcomeAnimanation();
                    break;
                default:
                    break;
            }
        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        /**设置主题*/
        super.onCreate(savedInstanceState);
        /**获取当前密码状态*/
        loadSecurityStatus();
        /**加载配置属性*/
        loadSettings();
        /**初始化视图*/
        initContentView();
        if (mSEOption == SecurityOption.PATTERN) {
            updatePatternView();
        }
        /**如果是创建密码状态，就做欢迎动画*/
        mHandler.sendEmptyMessageDelayed(MSG_TO_DO_ANIMANATION,1500);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            /**只有在创建密码时做"再点一次退出"的提示，其他的都是直接退出*/
            if(mSEState.equals(SecurityState.CREATE)){
                if(System.currentTimeMillis() - mLastClickTime > 2000){
                    ToastUtil.show(this,getResources().getString(R.string.press_again));
                    mLastClickTime = System.currentTimeMillis();
                    return true;
                }else {
                    finish();
                    return true;
                }
            }

        }

        return super.onKeyDown(keyCode, event);
    }

    /**
     * Loads settings, either from manifest or {@link com.haibison.android.lockpattern.util.Settings}.
     */
    private void loadSettings() {
        mMinWiredDots = Display.getMinWiredDots(this);
        mMaxRetries = Display.getMaxRetries(this);
        mAutoSave = Security.isAutoSavePattern(this);
        mCaptchaWiredDots = Display.getCaptchaWiredDots(this);
        mStealthMode = Display.isStealthMode(this);
         /**
         * Encrypter.
         */
        char[] encrypterClass;
        encrypterClass = Security.getEncrypterClass(this);

        if (encrypterClass != null) {
            try {
                mEncrypter = (IEncrypter) Class.forName(new String(encrypterClass), false, getClassLoader()).newInstance();
            } catch (Throwable t) {
                throw new InvalidEncrypterException();
            }
        }
    }

    private void initContentView() {

        /**设置视图*/
        setContentView(R.layout.splash_pattern_activity_layout);

        mTextInfo = (TextView) findViewById(R.id.tv_pwd_tips);
        mLockPatternView = (LockPatternView) findViewById(R.id.lp_pwd_lock_pattern_view);
        mLogoView = (RelativeLayout)findViewById(R.id.welcome_logo);
        mMoreHelpView = (PwdRetreiveView)findViewById(R.id.menu_view);
        View mMoreView = this.getLayoutInflater().inflate(R.layout.popwindow_guide,null);
        mMoreHelpView.initContentView(mMoreView);

        switch (getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) {
            case Configuration.SCREENLAYOUT_SIZE_LARGE:
            case Configuration.SCREENLAYOUT_SIZE_XLARGE: {
                final int size = getResources().getDimensionPixelSize(R.dimen.lockpatternview_size);
                LayoutParams lp = mLockPatternView.getLayoutParams();
                lp.width = size;
                lp.height = size;
                mLockPatternView.setLayoutParams(lp);
                break;
            }
        }

        /**是否有触觉反馈*/
        boolean hapticFeedbackEnabled = false;
        try {
            hapticFeedbackEnabled = android.provider.Settings.System.getInt(getContentResolver(),android.provider.Settings.System.HAPTIC_FEEDBACK_ENABLED, 0) != 0;
        } catch (Throwable t) {

        }
        mLockPatternView.setTactileFeedbackEnabled(hapticFeedbackEnabled);
        mLockPatternView.setInStealthMode(mStealthMode);
        mLockPatternView.setOnPatternListener(mLockPatternViewListener);
    }

    /**初始化*/
    private void loadSecurityStatus(){
        mSEOption = SecurityOption.PATTERN;
        mSEState = SecurityState.CREATE;
        bIsFirstCreate = true;
    }

    /**执行动画*/
    private void doWelcomeAnimanation(){
        Animation animation = AnimationUtils.loadAnimation(this,R.anim.guide_logo_animation);
        animation.setAnimationListener(mLogoAnimationListener);
        mLogoView.startAnimation(animation);
    }

    private void updatePINView(){

    }

    /**刷新显示密码图案视图*/
    private void updatePatternView(){
        if (mSEState.equals(SecurityState.CREATE)) {
            mTextInfo.setText(R.string.pattern_set_first);
        }else if (mSEState.equals(SecurityState.VERIFY)) {
            mTextInfo.setText(R.string.pattern_confirm);
        }
    }

    private void doComparePattern(final List<Cell> pattern) {
        if (pattern == null)
            return;
        new LoadingDialog<Void, Void, Boolean>(this, false) {

            @Override
            protected Boolean doInBackground(Void... params) {
                if (mSEState.equals(SecurityState.COMPARE)) {
                    char[] currentPattern = mCurrentPattern.clone();
                    if (currentPattern == null)currentPattern = Security.getPattern(CreatePwdActivity.this);
                    if (currentPattern != null) {
                        if (mEncrypter != null) {
                            return pattern.equals(mEncrypter.decrypt(CreatePwdActivity.this, currentPattern));
                        }else{
                            return Arrays.equals(currentPattern,LockPatternUtils.patternToSha1(pattern).toCharArray());
                        }
                    }
                }else if (mSEState.equals(SecurityState.VERIFY)) {
                    return Arrays.equals(mCurrentPattern,LockPatternUtils.patternToSha1(pattern).toCharArray());
                }
                return false;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                if (result)
                    createPatternOk(mCurrentPattern);
                else {
                    if(bIsFirstCreate){
                        mSEState = SecurityState.CREATE;
                        mCurrentPattern = null;
                        mLockPatternView.setDisplayMode(DisplayMode.Wrong);
                        mTextInfo.setText(R.string.pattern_error);
                        mLockPatternView.postDelayed(mLockPatternViewReloader,DELAY_TIME_TO_RELOAD_LOCK_PATTERN_VIEW);
                    }else {
                        mRetryCount++;
                        if (mRetryCount >= mMaxRetries){
                            comparePatternError();
                        }else {
                            mLockPatternView.setDisplayMode(DisplayMode.Wrong);
                            mTextInfo.setText(R.string.pattern_error);
                            mLockPatternView.postDelayed(mLockPatternViewReloader,DELAY_TIME_TO_RELOAD_LOCK_PATTERN_VIEW);
                        }
                    }
                }
            }

        }.execute();
    }

    /**
     * Checks and creates the pattern.
     *
     * @param pattern the current pattern of lock pattern view.
     */
    private void doCheckAndCreatePattern(final List<Cell> pattern) {
        if (pattern.size() < mMinWiredDots) {
            mLockPatternView.setDisplayMode(DisplayMode.Wrong);
            mTextInfo.setText(getResources().getString(R.string.pattern_error_tip, mMinWiredDots));
            mLockPatternView.postDelayed(mLockPatternViewReloader,DELAY_TIME_TO_RELOAD_LOCK_PATTERN_VIEW);
            return;
        }

        if (null != mCurrentPattern && mCurrentPattern.length > 0) {

            new LoadingDialog<Void, Void, Boolean>(this, false) {

                @Override
                protected Boolean doInBackground(Void... params) {
                    if (mEncrypter != null){
                        return pattern.equals(mEncrypter.decrypt(CreatePwdActivity.this, mCurrentPattern));
                    }else
                        return Arrays.equals(mCurrentPattern,LockPatternUtils.patternToSha1(pattern).toCharArray());
                }

                @Override
                protected void onPostExecute(Boolean result) {
                    super.onPostExecute(result);

                    if (result) {
                        mTextInfo.setText(R.string.pattern_continue);
                    } else {
                        mTextInfo.setText(R.string.pattern_confirm);
                        mLockPatternView.setDisplayMode(DisplayMode.Wrong);
                        mLockPatternView.postDelayed(mLockPatternViewReloader, DELAY_TIME_TO_RELOAD_LOCK_PATTERN_VIEW);
                    }
                }

            }.execute();
        } else {
            new LoadingDialog<Void, Void, char[]>(this, false) {

                @Override
                protected char[] doInBackground(Void... params) {
                    return mEncrypter != null ? mEncrypter.encrypt(CreatePwdActivity.this, pattern)
                            : LockPatternUtils.patternToSha1(pattern).toCharArray();
                }

                @Override
                protected void onPostExecute(char[] result) {
                    super.onPostExecute(result);
                    mCurrentPattern = result.clone();
                    //mTextInfo.setText(R.string.pattern_continue);
                    mLockPatternView.postDelayed(mLockPatternViewReloader,DELAY_TIME_TO_RELOAD_LOCK_PATTERN_VIEW);
                }

            }.execute();
        }
    }

    /***
     * 创建密码成功
     * @param pattern
     */
    private void createPatternOk(char[] pattern) {
        //ToastUtil.show(getApplicationContext(),"已经成功创建了密码"+String.valueOf(pattern));
        Settings.Security.setPattern(this, pattern);
        TApplication.getInstance().setHasPwd(true);
        Intent intent = new Intent(this,MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void comparePatternError() {

        finish();
    }

    private final LockPatternView.OnPatternListener mLockPatternViewListener = new LockPatternView.OnPatternListener() {

        @Override
        public void onPatternStart() {
            mLockPatternView.removeCallbacks(mLockPatternViewReloader);
            mLockPatternView.setDisplayMode(DisplayMode.Correct);
            if (mSEState.equals(SecurityState.CREATE)) {
                mTextInfo.setText(R.string.pattern_release_finger);
            } else if (mSEState.equals(SecurityState.VERIFY)) {
                mTextInfo.setText(R.string.pattern_confirm);
            }
        }

        @Override
        public void onPatternDetected(List<Cell> pattern) {
            if (mSEState.equals(SecurityState.CREATE)) {
                doCheckAndCreatePattern(pattern);
            }else if (mSEState.equals(SecurityState.VERIFY)) {
                if (!DisplayMode.Animate.equals(mLockPatternView.getDisplayMode())) {
                    doComparePattern(pattern);
                }
            }
        }

        @Override
        public void onPatternCleared() {
            mLockPatternView.removeCallbacks(mLockPatternViewReloader);

            if (mSEState.equals(SecurityState.CREATE)) {
                mLockPatternView.setDisplayMode(DisplayMode.Correct);
                if(null != mCurrentPattern && mCurrentPattern.length > 0){
                    mSEState = SecurityState.VERIFY;
                    mTextInfo.setText(R.string.pattern_confirm);
                }else {
                    mTextInfo.setText(R.string.lockpattern_recording_intro_header);
                }
            }else if (mSEState.equals(SecurityState.VERIFY)) {
                mTextInfo.setText(R.string.pattern_confirm);
            }
        }

        @Override
        public void onPatternCellAdded(List<Cell> pattern) {

        }
    };

    private final Runnable mLockPatternViewReloader = new Runnable() {

        @Override
        public void run() {
            mLockPatternView.clearPattern();
            mLockPatternViewListener.onPatternCleared();
        }
    };




    /*************************动画**************************************/
    private Animation.AnimationListener mLogoAnimationListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            Animation scaleAnimation = AnimationUtils.loadAnimation(CreatePwdActivity.this,R.anim.in_am);
            scaleAnimation.setAnimationListener(mPatternViewAnimationListener);
            mLockPatternView.startAnimation(scaleAnimation);

        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    };

    private Animation.AnimationListener mPatternViewAnimationListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
            mLockPatternView.setVisibility(View.VISIBLE);
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            Animation alphaAnimation = AnimationUtils.loadAnimation(CreatePwdActivity.this,R.anim.guide_first_step_title_anim);
            alphaAnimation.setAnimationListener(mTipsTitleAnimationListener);
            mTextInfo.startAnimation(alphaAnimation);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    };

    private Animation.AnimationListener mTipsTitleAnimationListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
            mTextInfo.setVisibility(View.VISIBLE);
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            Animation mMoreViewAnimation = AnimationUtils.loadAnimation(CreatePwdActivity.this,R.anim.icon_animation_top);
            mMoreViewAnimation.setAnimationListener(mMoreViewAnimationListener);
            mMoreHelpView.startAnimation(mMoreViewAnimation);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    };

    private Animation.AnimationListener mMoreViewAnimationListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
            mMoreHelpView.setVisibility(View.VISIBLE);
        }

        @Override
        public void onAnimationEnd(Animation animation) {
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    };

}
