/*
 *   Copyright 2012 Hai Bison
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.ted.applock.activity;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.TextView;
import com.android.TedFramework.util.LogUtil;
import com.haibison.android.lockpattern.BuildConfig;
import com.haibison.android.lockpattern.util.IEncrypter;
import com.haibison.android.lockpattern.util.InvalidEncrypterException;
import com.haibison.android.lockpattern.util.LoadingDialog;
import com.haibison.android.lockpattern.util.Settings.Display;
import com.haibison.android.lockpattern.util.Settings.Security;
import com.haibison.android.lockpattern.util.UI;
import com.haibison.android.lockpattern.widget.LockPatternUtils;
import com.haibison.android.lockpattern.widget.LockPatternView;
import com.haibison.android.lockpattern.widget.LockPatternView.Cell;
import com.haibison.android.lockpattern.widget.LockPatternView.DisplayMode;
import com.ted.applock.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.haibison.android.lockpattern.util.Settings.Display.*;
import static com.haibison.android.lockpattern.util.Settings.Security.METADATA_AUTO_SAVE_PATTERN;
import static com.haibison.android.lockpattern.util.Settings.Security.METADATA_ENCRYPTER_CLASS;

/**
 * Main activity for this library.
 * <p>
 * You can deliver result to {@link android.app.PendingIntent}'s and/ or
 * {@link android.os.ResultReceiver} too. See {@link #EXTRA_PENDING_INTENT_OK},
 * {@link #EXTRA_PENDING_INTENT_CANCELLED} and {@link #EXTRA_RESULT_RECEIVER}
 * for more details.
 * </p>
 * <p/>
 * <h1>NOTES</h1>
 * <ul>
 * <li>
 * You must use one of built-in actions when calling this activity. They start
 * with {@code ACTION_*}. Otherwise the library might behave strangely (we don't
 * cover those cases).</li>
 * <li>You must use one of the themes that this library supports. They start
 * with {@code R.style.Alp_42447968_Theme_*}. The reason is the themes contain
 * resources that the library needs.</li>
 * <li>With {@link #ACTION_COMPARE_PATTERN}, there are <b><i>4 possible result
 * codes</i></b>: {@link android.app.Activity#RESULT_OK}, {@link android.app.Activity#RESULT_CANCELED},
 * {@link #RESULT_FAILED} and {@link #RESULT_FORGOT_PATTERN}.</li>
 * <li>With {@link #ACTION_VERIFY_CAPTCHA}, there are <b><i>3 possible result
 * codes</i></b>: {@link android.app.Activity#RESULT_OK}, {@link android.app.Activity#RESULT_CANCELED},
 * and {@link #RESULT_FAILED}.</li>
 * </ul>
 *
 * @author Hai Bison
 * @since v1.0
 */
public class SplashPatternActivity extends Activity {

    private static final String CLASSNAME = SplashPatternActivity.class.getName();

    /**
     * Use this action to create new pattern. You can provide an
     * {@link com.haibison.android.lockpattern.util.IEncrypter} with
     * {@link com.haibison.android.lockpattern.util.Settings.Security#setEncrypterClass(android.content.Context, Class)} to
     * improve security.
     * <p/>
     * If the user created a pattern, {@link android.app.Activity#RESULT_OK} returns with
     * the pattern ({@link #EXTRA_PATTERN}). Otherwise
     * {@link android.app.Activity#RESULT_CANCELED} returns.
     *
     * @see #EXTRA_PENDING_INTENT_OK
     * @see #EXTRA_PENDING_INTENT_CANCELLED
     * @since v2.4 beta
     */
    public static final String ACTION_CREATE_PATTERN = CLASSNAME
            + ".create_pattern";

    /**
     * Use this action to compare pattern. You provide the pattern to be
     * compared with {@link #EXTRA_PATTERN}.
     * <p/>
     * If you enabled feature auto-save pattern before (with
     * {@link com.haibison.android.lockpattern.util.Settings.Security#setAutoSavePattern(android.content.Context, boolean)} ),
     * then you don't need {@link #EXTRA_PATTERN} at this time. But if you use
     * this extra, its priority is higher than the one stored in shared
     * preferences.
     * <p/>
     * You can use {@link #EXTRA_PENDING_INTENT_FORGOT_PATTERN} to help your
     * users in case they forgot the patterns.
     * <p/>
     * If the user passes, {@link android.app.Activity#RESULT_OK} returns. If not,
     * {@link #RESULT_FAILED} returns.
     * <p/>
     * If the user cancels the task, {@link android.app.Activity#RESULT_CANCELED} returns.
     * <p/>
     * In any case, there will have extra {@link #EXTRA_RETRY_COUNT} available
     * in the intent result.
     *
     * @see #EXTRA_PATTERN
     * @see #EXTRA_PENDING_INTENT_OK
     * @see #EXTRA_PENDING_INTENT_CANCELLED
     * @see #RESULT_FAILED
     * @see #EXTRA_RETRY_COUNT
     * @since v2.4 beta
     */
    public static final String ACTION_COMPARE_PATTERN = CLASSNAME
            + ".compare_pattern";

    /**
     * Use this action to let the activity generate a random pattern and ask the
     * user to re-draw it to verify.
     * <p/>
     * The default length of the auto-generated pattern is {@code 4}. You can
     * change it with
     * {@link com.haibison.android.lockpattern.util.Settings.Display#setCaptchaWiredDots(android.content.Context, int)}.
     *
     * @since v2.7 beta
     */
    public static final String ACTION_VERIFY_CAPTCHA = CLASSNAME
            + ".verify_captcha";

    /**
     * If you use {@link #ACTION_COMPARE_PATTERN} and the user fails to "login"
     * after a number of tries, this activity will finish with this result code.
     *
     * @see #ACTION_COMPARE_PATTERN
     * @see #EXTRA_RETRY_COUNT
     */
    public static final int RESULT_FAILED = RESULT_FIRST_USER + 1;

    /**
     * If you use {@link #ACTION_COMPARE_PATTERN} and the user forgot his/ her
     * pattern and decided to ask for your help with recovering the pattern (
     * {@link #EXTRA_PENDING_INTENT_FORGOT_PATTERN}), this activity will finish
     * with this result code.
     *
     * @see #ACTION_COMPARE_PATTERN
     * @see #EXTRA_RETRY_COUNT
     * @see #EXTRA_PENDING_INTENT_FORGOT_PATTERN
     * @since v2.8 beta
     */
    public static final int RESULT_FORGOT_PATTERN = RESULT_FIRST_USER + 2;

    /**
     * For actions {@link #ACTION_COMPARE_PATTERN} and
     * {@link #ACTION_VERIFY_CAPTCHA}, this key holds the number of tries that
     * the user attempted to verify the input pattern.
     */
    public static final String EXTRA_RETRY_COUNT = CLASSNAME + ".retry_count";

    /**
     * Sets value of this key to a theme in {@code R.style.Alp_42447968_Theme_*}
     * . Default is the one you set in your {@code AndroidManifest.xml}. Note
     * that theme {@link com.haibison.android.lockpattern.R.style#Alp_42447968_Theme_Light_DarkActionBar} is
     * available in API 4+, but it only works in API 14+.
     *
     * @since v1.5.3 beta
     */
    public static final String EXTRA_THEME = CLASSNAME + ".theme";

    /**
     * Key to hold the pattern. It must be a {@code char[]} array.
     * <p/>
     * <ul>
     * <li>If you use encrypter, it should be an encrypted array.</li>
     * <li>If you don't use encrypter, it should be the SHA-1 value of the
     * actual pattern. You can generate the value by
     * {@link com.haibison.android.lockpattern.widget.LockPatternUtils#patternToSha1(java.util.List)}.</li>
     * </ul>
     *
     * @since v2 beta
     */
    public static final String EXTRA_PATTERN = CLASSNAME + ".pattern";

    /**
     * You can provide an {@link android.os.ResultReceiver} with this key. The activity
     * will notify your receiver the same result code and intent data as you
     * will receive them in {@link #onActivityResult(int, int, android.content.Intent)}.
     *
     * @since v2.4 beta
     */
    public static final String EXTRA_RESULT_RECEIVER = CLASSNAME
            + ".result_receiver";

    /**
     * Put a {@link android.app.PendingIntent} into this key. It will be sent before
     * {@link android.app.Activity#RESULT_OK} will be returning. If you were calling this
     * activity with {@link #ACTION_CREATE_PATTERN}, key {@link #EXTRA_PATTERN}
     * will be attached to the original intent which the pending intent holds.
     * <p/>
     * <h1>Notes</h1>
     * <ul>
     * <li>If you're going to use an activity, you don't need
     * {@link android.content.Intent#FLAG_ACTIVITY_NEW_TASK} for the intent, since the library
     * will call it inside {@link SplashPatternActivity} .</li>
     * </ul>
     */
    public static final String EXTRA_PENDING_INTENT_OK = CLASSNAME
            + ".pending_intent_ok";

    /**
     * Put a {@link android.app.PendingIntent} into this key. It will be sent before
     * {@link android.app.Activity#RESULT_CANCELED} will be returning.
     * <p/>
     * <h1>Notes</h1>
     * <ul>
     * <li>If you're going to use an activity, you don't need
     * {@link android.content.Intent#FLAG_ACTIVITY_NEW_TASK} for the intent, since the library
     * will call it inside {@link SplashPatternActivity} .</li>
     * </ul>
     */
    public static final String EXTRA_PENDING_INTENT_CANCELLED = CLASSNAME
            + ".pending_intent_cancelled";

    /**
     * You put a {@link android.app.PendingIntent} into this extra. The library will show a
     * button <i>"Forgot pattern?"</i> and call your intent later when the user
     * taps it.
     * <p/>
     * <h1>Notes</h1>
     * <ul>
     * <li>If you use an activity, you don't need
     * {@link android.content.Intent#FLAG_ACTIVITY_NEW_TASK} for the intent, since the library
     * will call it inside {@link SplashPatternActivity} .</li>
     * <li>{@link SplashPatternActivity} will finish with
     * {@link #RESULT_FORGOT_PATTERN} <i><b>after</b> making a call</i> to start
     * your pending intent.</li>
     * <li>It is your responsibility to make sure the Intent is good. The
     * library doesn't cover any errors when calling your intent.</li>
     * </ul>
     *
     * @author Thanks to Yan Cheng Cheok for his idea.
     * @see #ACTION_COMPARE_PATTERN
     * @since v2.8 beta
     */
    public static final String EXTRA_PENDING_INTENT_FORGOT_PATTERN = CLASSNAME
            + ".pending_intent_forgot_pattern";

    /**
     * Helper enum for button OK commands. (Because we use only one "OK" button
     * for different commands).
     *
     * @author Hai Bison
     */
    private static enum ButtonOkCommand {
        CONTINUE, FORGOT_PATTERN, DONE
    }// ButtonOkCommand

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

    /*
     * FIELDS
     */
    private int mMaxRetries, mMinWiredDots, mRetryCount = 0, mCaptchaWiredDots;
    private boolean mAutoSave, mStealthMode;
    private IEncrypter mEncrypter;
    private ButtonOkCommand mBtnOkCmd;
    private Intent mIntentResult;
    private SecurityOption mSEOption;
    private SecurityState mSEState;
    private char[] mCurrentPattern;

    /*
     * CONTROLS
     */
    private TextView mTextInfo;
    private LockPatternView mLockPatternView;
    private View mFooter;
    private Button mBtnCancel;
    private Button mBtnConfirm;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        LogUtil.d("ClassName = " + CLASSNAME);
        /**设置主题*/
        if (getIntent().hasExtra(EXTRA_THEME)){
            setTheme(getIntent().getIntExtra(EXTRA_THEME,com.haibison.android.lockpattern.R.style.Alp_42447968_Theme_Dark));
        }
        super.onCreate(savedInstanceState);
        /**加载配置属性*/
        loadSettings();
        /**初始化视图*/
        initContentView();
        /**获取当前密码状态*/
        loadSecurityStatus();

        if (mSEOption == SecurityOption.PATTERN) {
            updatePatternView();
        }else {
            updatePINView();
        }

//        mIntentResult = new Intent();
//        setResult(RESULT_CANCELED, mIntentResult);


    }// onCreate()



    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (BuildConfig.DEBUG)
            Log.d(CLASSNAME, "onConfigurationChanged()");
        super.onConfigurationChanged(newConfig);
        initContentView();
    }// onConfigurationChanged()

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK
                && ACTION_COMPARE_PATTERN.equals(getIntent().getAction())) {
            /*
             * Use this hook instead of onBackPressed(), because onBackPressed()
             * is not available in API 4.
             */
            finishWithNegativeResult(RESULT_CANCELED);
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }// onKeyDown()

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        /*
         * Support canceling dialog on touching outside in APIs < 11.
         *
         * This piece of code is copied from android.view.Window. You can find
         * it by searching for methods shouldCloseOnTouch() and isOutOfBounds().
         */
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB
                && event.getAction() == MotionEvent.ACTION_DOWN
                && getWindow().peekDecorView() != null) {
            final int x = (int) event.getX();
            final int y = (int) event.getY();
            final int slop = ViewConfiguration.get(this)
                    .getScaledWindowTouchSlop();
            final View decorView = getWindow().getDecorView();
            boolean isOutOfBounds = (x < -slop) || (y < -slop)
                    || (x > (decorView.getWidth() + slop))
                    || (y > (decorView.getHeight() + slop));
            if (isOutOfBounds) {
                finishWithNegativeResult(RESULT_CANCELED);
                return true;
            }
        }// if

        return super.onTouchEvent(event);
    }// onTouchEvent()

    /**
     * Loads settings, either from manifest or {@link com.haibison.android.lockpattern.util.Settings}.
     */
    private void loadSettings() {
        Bundle metaData = null;
        try {
            metaData = getPackageManager().getActivityInfo(getComponentName(),
                    PackageManager.GET_META_DATA).metaData;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        if (metaData != null && metaData.containsKey(METADATA_MIN_WIRED_DOTS))
            mMinWiredDots = Display.validateMinWiredDots(this,
                    metaData.getInt(METADATA_MIN_WIRED_DOTS));
        else
            mMinWiredDots = Display.getMinWiredDots(this);

        if (metaData != null && metaData.containsKey(METADATA_MAX_RETRIES))
            mMaxRetries = Display.validateMaxRetries(this,
                    metaData.getInt(METADATA_MAX_RETRIES));
        else
            mMaxRetries = Display.getMaxRetries(this);

        if (metaData != null
                && metaData.containsKey(METADATA_AUTO_SAVE_PATTERN))
            mAutoSave = metaData.getBoolean(METADATA_AUTO_SAVE_PATTERN);
        else
            mAutoSave = Security.isAutoSavePattern(this);

        if (metaData != null
                && metaData.containsKey(METADATA_CAPTCHA_WIRED_DOTS))
            mCaptchaWiredDots = Display.validateCaptchaWiredDots(this,
                    metaData.getInt(METADATA_CAPTCHA_WIRED_DOTS));
        else
            mCaptchaWiredDots = Display.getCaptchaWiredDots(this);

        if (metaData != null && metaData.containsKey(METADATA_STEALTH_MODE))
            mStealthMode = metaData.getBoolean(METADATA_STEALTH_MODE);
        else
            mStealthMode = Display.isStealthMode(this);

        /*
         * Encrypter.
         */
        char[] encrypterClass;
        if (metaData != null && metaData.containsKey(METADATA_ENCRYPTER_CLASS))
            encrypterClass = metaData.getString(METADATA_ENCRYPTER_CLASS)
                    .toCharArray();
        else
            encrypterClass = Security.getEncrypterClass(this);

        if (encrypterClass != null) {
            try {
                mEncrypter = (IEncrypter) Class.forName(
                        new String(encrypterClass), false, getClassLoader())
                        .newInstance();
            } catch (Throwable t) {
                throw new InvalidEncrypterException();
            }
        }
    }// loadSettings()

    /**
     * Initializes UI...
     */
    private void initContentView() {
        /*
         * Save all controls' state to restore later.
         */
        DisplayMode lastDisplayMode = mLockPatternView != null ? mLockPatternView.getDisplayMode() : null;
        List<Cell> lastPattern = mLockPatternView != null ? mLockPatternView.getPattern() : null;
        /**设置视图*/
        setContentView(R.layout.splash_pattern_activity_layout);
        UI.adjustDialogSizeForLargeScreens(getWindow());

        mTextInfo = (TextView) findViewById(R.id.tv_pwd_tips);
        mLockPatternView = (LockPatternView) findViewById(R.id.lp_lock_pattern_view);
        mFooter = findViewById(R.id.ll_pwd_footer_layout);
        mBtnCancel = (Button) findViewById(R.id.bt_pwd_footer_cancel);
        mBtnConfirm = (Button) findViewById(R.id.bt_pwd_footer_confirm);

        /*
         * LOCK PATTERN VIEW
         */

        switch (getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) {
            case Configuration.SCREENLAYOUT_SIZE_LARGE:
            case Configuration.SCREENLAYOUT_SIZE_XLARGE: {
                final int size = getResources().getDimensionPixelSize(com.haibison.android.lockpattern.R.dimen.alp_42447968_lockpatternview_size);
                LayoutParams lp = mLockPatternView.getLayoutParams();
                lp.width = size;
                lp.height = size;
                mLockPatternView.setLayoutParams(lp);

                break;
            }// LARGE / XLARGE
        }

        /**是否有触觉反馈*/
        boolean hapticFeedbackEnabled = false;
        try {
            hapticFeedbackEnabled = android.provider.Settings.System.getInt(getContentResolver(),
                    android.provider.Settings.System.HAPTIC_FEEDBACK_ENABLED, 0) != 0;
        } catch (Throwable t) {
            /*
             * Ignore it.
             */
        }
        mLockPatternView.setTactileFeedbackEnabled(hapticFeedbackEnabled);

        mLockPatternView.setInStealthMode(mStealthMode && !ACTION_VERIFY_CAPTCHA.equals(getIntent().getAction()));
        mLockPatternView.setOnPatternListener(mLockPatternViewListener);
        if (lastPattern != null && lastDisplayMode != null
                && !ACTION_VERIFY_CAPTCHA.equals(getIntent().getAction())){
            mLockPatternView.setPattern(lastDisplayMode, lastPattern);
        }

    }// initContentView()

    /**初始化*/
    private void loadSecurityStatus(){
        mSEOption = SecurityOption.PATTERN;
        mSEState = SecurityState.CREATE;
    }

    private void updatePINView(){

    }

    private void updatePatternView(){
        if (mSEState.equals(SecurityState.CREATE)) {
            //mBtnCancel.setOnClickListener(mBtnCancelOnClickListener);
            //mBtnConfirm.setOnClickListener(mBtnConfirmOnClickListener);
           // mBtnCancel.setVisibility(View.VISIBLE);
            //mFooter.setVisibility(View.VISIBLE);
            mTextInfo.setText(R.string.pattern_set_first);
//            if (mBtnOkCmd == null){
//                mBtnOkCmd = ButtonOkCommand.CONTINUE;
//            }
//            switch (mBtnOkCmd) {
//                case CONTINUE:
//                    mBtnConfirm.setText(R.string.continue_string);
//                    break;
//                case DONE:
//                    mBtnConfirm.setText(R.string.confirm);
//                    break;
//                default:
//                    break;
//            }
//            mBtnConfirm.setEnabled(false);
        }
        else if (mSEState.equals(SecurityState.COMPARE)) {
            mTextInfo.setText(R.string.pick_pattern);
        }
        else if (mSEState.equals(SecurityState.VERIFY)) {
            mTextInfo.setText(R.string.pattern_confirm);

            /*
             * NOTE: EXTRA_PATTERN should hold a char[] array. In this case we
             * use it as a temporary variable to hold a list of Cell.
             */

            final ArrayList<Cell> pattern;
            if (getIntent().hasExtra(EXTRA_PATTERN)){
                pattern = getIntent().getParcelableArrayListExtra(EXTRA_PATTERN);
            }else{
                getIntent().putParcelableArrayListExtra(EXTRA_PATTERN,pattern = LockPatternUtils.genCaptchaPattern(mCaptchaWiredDots));
            }


            mLockPatternView.setPattern(DisplayMode.Animate, pattern);
        }// ACTION_VERIFY_CAPTCHA
    }


    /**
     * Compares {@code pattern} to the given pattern (
     * {@link #ACTION_COMPARE_PATTERN}) or to the generated "CAPTCHA" pattern (
     * {@link #ACTION_VERIFY_CAPTCHA}). Then finishes the activity if they
     * match.
     *
     * @param pattern the pattern to be compared.
     */
    private void doComparePattern(final List<Cell> pattern) {
        if (pattern == null)
            return;

        /*
         * Use a LoadingDialog because decrypting pattern might take time...
         */

        new LoadingDialog<Void, Void, Boolean>(this, false) {

            @Override
            protected Boolean doInBackground(Void... params) {
                if (ACTION_COMPARE_PATTERN.equals(getIntent().getAction())) {
                    char[] currentPattern = getIntent().getCharArrayExtra(
                            EXTRA_PATTERN);
                    if (currentPattern == null)
                        currentPattern = Security
                                .getPattern(SplashPatternActivity.this);
                    if (currentPattern != null) {
                        if (mEncrypter != null)
                            return pattern.equals(mEncrypter.decrypt(
                                    SplashPatternActivity.this, currentPattern));
                        else
                            return Arrays.equals(currentPattern,
                                    LockPatternUtils.patternToSha1(pattern)
                                            .toCharArray());
                    }
                }// ACTION_COMPARE_PATTERN
                else if (ACTION_VERIFY_CAPTCHA.equals(getIntent().getAction())) {
                    return pattern.equals(getIntent()
                            .getParcelableArrayListExtra(EXTRA_PATTERN));
                }// ACTION_VERIFY_CAPTCHA

                return false;
            }// doInBackground()

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);

                if (result)
                    finishWithResultOk(null);
                else {
                    mRetryCount++;
                    mIntentResult.putExtra(EXTRA_RETRY_COUNT, mRetryCount);

                    if (mRetryCount >= mMaxRetries)
                        finishWithNegativeResult(RESULT_FAILED);
                    else {
                        mLockPatternView.setDisplayMode(DisplayMode.Wrong);
                        mTextInfo.setText(R.string.pattern_error);
                        mLockPatternView.postDelayed(mLockPatternViewReloader,
                                DELAY_TIME_TO_RELOAD_LOCK_PATTERN_VIEW);
                    }
                }
            }// onPostExecute()

        }.execute();
    }// doComparePattern()

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
            /*
             * Use a LoadingDialog because decrypting pattern might take time...
             */
            new LoadingDialog<Void, Void, Boolean>(this, false) {

                @Override
                protected Boolean doInBackground(Void... params) {
                    if (mEncrypter != null){
                        return pattern.equals(mEncrypter.decrypt(SplashPatternActivity.this, mCurrentPattern));
                    }else
                        return Arrays.equals(mCurrentPattern,LockPatternUtils.patternToSha1(pattern).toCharArray());
                }

                @Override
                protected void onPostExecute(Boolean result) {
                    super.onPostExecute(result);

                    if (result) {
                        mTextInfo.setText(R.string.pattern_continue);
                        //mBtnConfirm.setEnabled(true);
                    } else {
                        mTextInfo.setText(R.string.pattern_confirm);
                        mLockPatternView.setDisplayMode(DisplayMode.Wrong);
                        mLockPatternView.postDelayed(mLockPatternViewReloader, DELAY_TIME_TO_RELOAD_LOCK_PATTERN_VIEW);
                    }
                }

            }.execute();
        } else {
            /*
             * Use a LoadingDialog because encrypting pattern might take time...
             */
            new LoadingDialog<Void, Void, char[]>(this, false) {

                @Override
                protected char[] doInBackground(Void... params) {
                    return mEncrypter != null ? mEncrypter.encrypt(SplashPatternActivity.this, pattern)
                            : LockPatternUtils.patternToSha1(pattern).toCharArray();
                }

                @Override
                protected void onPostExecute(char[] result) {
                    super.onPostExecute(result);
                    getIntent().putExtra(EXTRA_PATTERN, result);
                    mCurrentPattern = result.clone();
                    mTextInfo.setText(R.string.pattern_continue);
                    //mBtnConfirm.setEnabled(true);
                }

            }.execute();
        }
    }

    /**
     * Finishes activity with {@link android.app.Activity#RESULT_OK}.
     *
     * @param pattern the pattern, if this is in mode creating pattern. In any
     *                cases, it can be set to {@code null}.
     */
    private void finishWithResultOk(char[] pattern) {
        if (ACTION_CREATE_PATTERN.equals(getIntent().getAction()))
            mIntentResult.putExtra(EXTRA_PATTERN, pattern);
        else {
            /*
             * If the user was "logging in", minimum try count can not be zero.
             */
            mIntentResult.putExtra(EXTRA_RETRY_COUNT, mRetryCount + 1);
        }

        setResult(RESULT_OK, mIntentResult);

        /*
         * ResultReceiver
         */
        ResultReceiver receiver = getIntent().getParcelableExtra(
                EXTRA_RESULT_RECEIVER);
        if (receiver != null) {
            Bundle bundle = new Bundle();
            if (ACTION_CREATE_PATTERN.equals(getIntent().getAction()))
                bundle.putCharArray(EXTRA_PATTERN, pattern);
            else {
                /*
                 * If the user was "logging in", minimum try count can not be
                 * zero.
                 */
                bundle.putInt(EXTRA_RETRY_COUNT, mRetryCount + 1);
            }
            receiver.send(RESULT_OK, bundle);
        }

        /*
         * PendingIntent
         */
        PendingIntent pi = getIntent().getParcelableExtra(
                EXTRA_PENDING_INTENT_OK);
        if (pi != null) {
            try {
                pi.send(this, RESULT_OK, mIntentResult);
            } catch (Throwable t) {
                Log.e(CLASSNAME, "Error sending PendingIntent: " + pi, t);
            }
        }

        finish();
    }

    /**
     * Finishes the activity with negative result (
     * {@link android.app.Activity#RESULT_CANCELED}, {@link #RESULT_FAILED} or
     * {@link #RESULT_FORGOT_PATTERN}).
     */
    private void finishWithNegativeResult(int resultCode) {
        if (ACTION_COMPARE_PATTERN.equals(getIntent().getAction()))
            mIntentResult.putExtra(EXTRA_RETRY_COUNT, mRetryCount);

        setResult(resultCode, mIntentResult);

        /*
         * ResultReceiver
         */
        ResultReceiver receiver = getIntent().getParcelableExtra(
                EXTRA_RESULT_RECEIVER);
        if (receiver != null) {
            Bundle resultBundle = null;
            if (ACTION_COMPARE_PATTERN.equals(getIntent().getAction())) {
                resultBundle = new Bundle();
                resultBundle.putInt(EXTRA_RETRY_COUNT, mRetryCount);
            }
            receiver.send(resultCode, resultBundle);
        }

        /*
         * PendingIntent
         */
        PendingIntent pi = getIntent().getParcelableExtra(
                EXTRA_PENDING_INTENT_CANCELLED);
        if (pi != null) {
            try {
                pi.send(this, resultCode, mIntentResult);
            } catch (Throwable t) {
                Log.e(CLASSNAME, "Error sending PendingIntent: " + pi, t);
            }
        }

        finish();
    }

    private final LockPatternView.OnPatternListener mLockPatternViewListener = new LockPatternView.OnPatternListener() {

        @Override
        public void onPatternStart() {
            mLockPatternView.removeCallbacks(mLockPatternViewReloader);
            mLockPatternView.setDisplayMode(DisplayMode.Correct);
            if (mSEState.equals(SecurityState.CREATE)) {
                mTextInfo.setText(R.string.pattern_release_finger);
                //mBtnConfirm.setEnabled(false);
            }else if (mSEState.equals(SecurityState.COMPARE)) {
                mTextInfo.setText(R.string.pick_pattern);
            }
            else if (mSEState.equals(SecurityState.VERIFY)) {
                mTextInfo.setText(R.string.pattern_confirm);
            }
        }

        @Override
        public void onPatternDetected(List<Cell> pattern) {
            if (mSEState.equals(SecurityState.CREATE)) {
                doCheckAndCreatePattern(pattern);
            }else if (mSEState.equals(SecurityState.COMPARE)) {
                doComparePattern(pattern);
            }else if (mSEState.equals(SecurityState.VERIFY)) {
                if (!DisplayMode.Animate.equals(mLockPatternView
                        .getDisplayMode()))
                    doComparePattern(pattern);
            }
        }

        @Override
        public void onPatternCleared() {
            mLockPatternView.removeCallbacks(mLockPatternViewReloader);

            if (mSEState.equals(SecurityState.CREATE)) {
                mLockPatternView.setDisplayMode(DisplayMode.Correct);
                mBtnConfirm.setEnabled(false);
                if (mBtnOkCmd == ButtonOkCommand.CONTINUE) {
                    getIntent().removeExtra(EXTRA_PATTERN);
                    mTextInfo.setText(R.string.lockpattern_recording_intro_header);
                } else
                    mTextInfo.setText(R.string.pattern_confirm);
            }else if (mSEState.equals(SecurityState.COMPARE)) {
                mLockPatternView.setDisplayMode(DisplayMode.Correct);
                mTextInfo.setText(R.string.pick_pattern);
            }else if (mSEState.equals(SecurityState.VERIFY)) {
                mTextInfo.setText(R.string.pattern_confirm);
                List<Cell> pattern = getIntent().getParcelableArrayListExtra(EXTRA_PATTERN);
                mLockPatternView.setPattern(DisplayMode.Animate, pattern);
            }
        }

        @Override
        public void onPatternCellAdded(List<Cell> pattern) {

        }
    };

    private final View.OnClickListener mBtnCancelOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            finishWithNegativeResult(RESULT_CANCELED);
        }// onClick()
    };

    private final View.OnClickListener mBtnConfirmOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (mSEState.equals(SecurityState.CREATE)) {
                if (mBtnOkCmd == ButtonOkCommand.CONTINUE) {
                    mBtnOkCmd = ButtonOkCommand.DONE;
                    mLockPatternView.clearPattern();
                    mTextInfo.setText(R.string.pattern_confirm);
                    mBtnConfirm.setText(com.haibison.android.lockpattern.R.string.alp_42447968_cmd_confirm);
                    mBtnConfirm.setEnabled(false);
                } else {
                    final char[] pattern = getIntent().getCharArrayExtra(
                            EXTRA_PATTERN);
                    if (mAutoSave)
                        Security.setPattern(SplashPatternActivity.this,
                                pattern);
                    finishWithResultOk(pattern);
                }
            }// ACTION_CREATE_PATTERN
            else if (mSEState.equals(SecurityState.COMPARE)) {
                /*
                 * We don't need to verify the extra. First, this button is only
                 * visible if there is this extra in the intent. Second, it is
                 * the responsibility of the caller to make sure the extra is
                 * good.
                 */
                PendingIntent pi = null;
                try {
                    pi = getIntent().getParcelableExtra(
                            EXTRA_PENDING_INTENT_FORGOT_PATTERN);
                    pi.send();
                } catch (Throwable t) {
                    Log.e(CLASSNAME, "Error sending pending intent: " + pi, t);
                }
                finishWithNegativeResult(RESULT_FORGOT_PATTERN);
            }
        }
    };

    private final Runnable mLockPatternViewReloader = new Runnable() {

        @Override
        public void run() {
            mLockPatternView.clearPattern();
            mLockPatternViewListener.onPatternCleared();
        }
    };

}
