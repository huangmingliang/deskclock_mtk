package com.android.deskclock.timer;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.NotificationCompat;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.android.deskclock.CircleTimerView;
import com.android.deskclock.CircleTimerView.CursorMoveListener;
import com.android.deskclock.DeskClock;
import com.android.deskclock.DeskClockFragment;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.timer.NewTimePickerFG.OnTimeSaveListener;

public class TimerFragment3 extends DeskClockFragment implements
        OnSharedPreferenceChangeListener, OnClickListener, CursorMoveListener
        , FullScreenBackListener {

    private String TAG = getClass().getSimpleName();
    private String jyyTAG = "jyyTag--->";
    public static final long ANIMATION_TIME_MILLIS = DateUtils.SECOND_IN_MILLIS / 3;
    public static final String TIME_OBJ = "time_obj";
    // Transitions are available only in API 19+
    private boolean mTicking = false;
    private SharedPreferences mPrefs;
    private Bundle mViewState = null;

    private FrameLayout mTimerView;
    private CircleTimerView mCircleView;
    private CountingTimerView mCountingView;
    private Button timerLabel;
    private RelativeLayout timer30m;
    private RelativeLayout timer15m;
    private RelativeLayout timer10m;
    private RelativeLayout timer5m;
    private ImageView timerReset;
    private ImageView timerPause;
    private boolean isFragmentReset = true;
    private Context context;
    private TimerObj timerObj;
    private LinearLayout timeGroup;
    private RelativeLayout counting_shape;//counting的点击范围

    final private Runnable mClockTick = new Runnable() {

        @Override
        public void run() {
            long timeLeft = 0;
            if (timerObj.mState == TimerObj.STATE_RUNNING) {
                timeLeft = timerObj.updateTimeLeft(false);
                mCountingView.setTime(timeLeft, false, false);
                mCircleView.setTimerMode(true);
            }
            if (timeLeft > 0) {
                mTimerView.postDelayed(mClockTick, 20);
            } else {
                mTicking = false;
                timerObj.mState = TimerObj.STATE_STOPPED;
                resetClock();
            }

        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        LogUtils.d(TAG, "onCreate!!!");
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        LogUtils.d(TAG, "onCreateView!!!");
        View view = inflater.inflate(R.layout.timer_list_item_3, container,
                false);
        mTimerView = (FrameLayout) view.findViewById(R.id.timer_view);
        mCircleView = (CircleTimerView) view.findViewById(R.id.timer_time);
        mCountingView = (CountingTimerView) view
                .findViewById(R.id.timer_time_text);
        counting_shape = (RelativeLayout) view.findViewById(R.id.CountingTimer_shape);
        timer30m = (RelativeLayout) view.findViewById(R.id.timer30m);
        timer15m = (RelativeLayout) view.findViewById(R.id.timer15m);
        timer10m = (RelativeLayout) view.findViewById(R.id.timer10m);
        timer5m = (RelativeLayout) view.findViewById(R.id.timer5m);
        timerReset = (ImageView) view.findViewById(R.id.timer_reset);
        timerPause = (ImageView) view.findViewById(R.id.timer_pause);
        timeGroup = (LinearLayout) view.findViewById(R.id.timeGroup);
        timerLabel = (Button) view.findViewById(R.id.timer_label);
        mCircleView.setCircleTimeViewListener(this);

        timer30m.setOnClickListener(this);
        timer15m.setOnClickListener(this);
        timer10m.setOnClickListener(this);
        timer5m.setOnClickListener(this);
        timerLabel.setOnClickListener(this);
        timerPause.setOnClickListener(this);
        timerReset.setOnClickListener(this);
        counting_shape.setOnClickListener(this);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated");
        context = getActivity();
        mCircleView.diliveryActivityObj(getActivity());
        mViewState = savedInstanceState;
        Log.d(TAG, "mViewState=" + mViewState);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        restoreTimerClock();
        Log.e("加载完毕", "gogo");
    }

    @Override
    public void onResume() {
        LogUtils.d(TAG, "onResume!!!");
        super.onResume();

        final DeskClock activity = (DeskClock) getActivity();
        if (activity.getSelectedTab() == DeskClock.ALARM_TAB_INDEX) {
            setFabAppearance();
            // setLeftRightButtonAppearance();
        }
        mPrefs.registerOnSharedPreferenceChangeListener(this);

        if (mPrefs.getBoolean(Timers.REFRESH_UI_WITH_LATEST_DATA, false)) {
            // Clear the flag indicating the adapter is out of sync with the
            // database.
            mPrefs.edit().putBoolean(Timers.REFRESH_UI_WITH_LATEST_DATA, false)
                    .apply();
        }
        if (context == null) {
            LogUtils.d(TAG, "on Resume , context is null");
        } else {
            LogUtils.d(TAG, "on Resume , context is not null");
            if (BACK_FROM_ALRM) {
                quickSetup(60 * 1000);
                startClockTicks();
                showAddOneNotification();
                BACK_FROM_ALRM = false;
                LogUtils.d(TAG, "onResume , startTicks and showNotification");
            }
        }
    }

    private void showAddOneNotification(){
        Notification.Builder builder = new Notification.Builder(context);
        builder.setSmallIcon(R.drawable.alarm_notify)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentTitle(context.getResources().getString(R.string.timer_notification_label))
                .setContentText(context.getResources().getStringArray(R.array.timer_notifications)[0])
                .setShowWhen(true);
        NotificationManagerCompat.from(context).notify(timerObj.mTimerId, builder.build());
        LogUtils.d(TAG, "sendNotification!!!");
    }

    private void cancelAddOneNotification(){
        NotificationManagerCompat.from(context).cancel(timerObj.mTimerId);
    }

    @Override
    public void onPause() {
        LogUtils.d(TAG, "onPause!!!");
        super.onPause();

        if (getActivity() instanceof DeskClock) {
            ((DeskClock) getActivity()).unregisterPageChangedListener(this);
        }
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        // / M: Don't save timer now, in case old data are writed to
        // sharePreference @{
    }

    @Override
    public void onDestroy() {
        LogUtils.d(TAG, "onDestroy!!!");

        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState");
        outState.putParcelable(TIME_OBJ, timerObj);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mViewState = null;
    }

    @Override
    public void onPageChanged(int page) {
    }

    // Starts the ticks that animate the timers.

    private void updateTimerState(TimerObj t, String action) {
        updateTimerState(t, action, true);
    }

    /**
     * @param update indicates whether to call updateNextTimesup in TimerReceiver.
     *               This is false only for label changes.
     */
    private void updateTimerState(TimerObj t, String action, boolean update) {
        Log.d(TAG, "updateTimerState action:" + action + " TimerObj id:"
                + t.mTimerId);
        t.writeToSharedPref(mPrefs);
        final Intent i = new Intent();
        i.setAction(action);
        i.putExtra(Timers.TIMER_INTENT_EXTRA, t.mTimerId);
        i.putExtra(Timers.UPDATE_NEXT_TIMESUP, update);
        // Make sure the receiver is getting the intent ASAP.
        i.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        getActivity().sendBroadcast(i);
    }

    public void set(long timerLength, long timeLeft, boolean drawRed) {
        // mCircleView.setTimerMode(true);
        mCircleView.setIntervalTime(timerLength);
        mCircleView.setPassedTime(timerLength - timeLeft, drawRed);
        mCircleView.invalidate();
    }

    @Override
    public void onFabClick(View view) {
        LogUtils.d(TAG, timerReset.isEnabled() + "什么结果");
        startClockTicks();
    }

    /**
     *
     */
    public void labelClick() {
        LayoutInflater inflate = LayoutInflater.from(context);
        View diaglogView = inflate.inflate(R.layout.alarm_label_dialog, null);
        Button bt_ok = (Button) diaglogView.findViewById(R.id.btn_ok);
        Button bt_cancel = (Button) diaglogView.findViewById(R.id.btn_cancel);
        final EditText et = (EditText) diaglogView
                .findViewById(R.id.label_input);
        Builder builder = new AlertDialog.Builder(context);
        final AlertDialog alert = builder.setView(diaglogView).create();
        alert.show();
        bt_ok.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                CharSequence content = et.getText();
                timerLabel.setText(content);
                alert.dismiss();
            }
        });

        bt_cancel.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                alert.dismiss();
            }
        });

    }

    @Override
    public void setFabAppearance() {
        final DeskClock activity = (DeskClock) getActivity();
        // / M: Activity maybe null when configuration change
        if (activity == null || mFab == null) {
            return;
        }

        if (activity.getSelectedTab() != DeskClock.TIMER_TAB_INDEX) {
            mFab.setVisibility(View.VISIBLE);
            return;
        }
        if (isFragmentReset) {
            mFab.setVisibility(View.VISIBLE);
            mFab.setImageResource(R.drawable.start_green);
            mFab.setContentDescription(getString(R.string.timer_start));
            timeGroup.setAlpha(1.0f);
            timerReset.setVisibility(View.GONE);
            timerPause.setVisibility(View.GONE);
        } else {
            mFab.setVisibility(View.GONE);
            timerReset.setVisibility(View.VISIBLE);
            timerPause.setVisibility(View.VISIBLE);
            if (mTicking) {
                timerPause.setImageResource(R.drawable.pause_green);
            } else {
                timerPause.setImageResource(R.drawable.start_green);
            }

        }

    }



    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (prefs.equals(mPrefs)) {
            if (key.equals(Timers.REFRESH_UI_WITH_LATEST_DATA)
                    && prefs.getBoolean(key, false)) {
                // Clear the flag forcing a refresh of the adapter to reflect
                // external changes.
                mPrefs.edit().putBoolean(key, false).apply();
                setFabAppearance();
            }
        }
    }

    public void setLabel(TimerObj timer, String label) {

    }

    @Override
    public void onClick(View arg0) {
        switch (arg0.getId()) {
            case R.id.timer30m:
                quickSetup(30 * 60 * 1000);
                break;
            case R.id.timer15m:
                quickSetup(15 * 60 * 1000);
                break;
            case R.id.timer10m:
                quickSetup(10 * 60 * 1000);
                break;
            case R.id.timer5m:
                quickSetup(5 * 60 * 1000);
                break;
            case R.id.timer_pause:
                stopOrRestartClockTicks();
                break;
            case R.id.timer_reset:
                cancelAddOneNotification();
                resetClock();
                break;
            case R.id.timer_label:
                labelClick();
                break;
            case R.id.CountingTimer_shape:
                timer_textClick();
                break;
            default:
                break;
        }
    }

    /**
     * 按下数字控件之后的操作：
     * 弹出NewTimePickerFG进行时间选择
     */
    private void timer_textClick() {
        Log.d(jyyTAG, jyyTAG + "timer_textClick!");
        FragmentManager manager = getFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        NewTimePickerFG fragment = new NewTimePickerFG(new OnTimeSaveListener() {

            @Override
            public void onDateSet(long finalValue) {
                quickSetup(finalValue);
            }
        });
        transaction.add(fragment, "haha");
        transaction.commit();
    }

    private void quickSetup(long timeLength) {
        LogUtils.d(TAG, "quickSetup!!!");
        if (context == null) {
            LogUtils.d(TAG, "quickSetup---context is null");
        }
        timerObj = new TimerObj(timeLength, context);
        mCountingView.setTime(timerObj.mTimeLeft, false, true);
        set(timerObj.mOriginalLength, timerObj.mTimeLeft, false);
        setFabAppearance();
    }

    private void restoreTimerClock() {
        if (mViewState == null) {
            Log.d(TAG, "mViewState==null||timerObj==null");
            return;
        }
        timerObj = (TimerObj) mViewState.get(TIME_OBJ);
        Log.d(TAG, "isFragmentReset=" + isFragmentReset);
        if (timerObj == null) {
            setFabAppearance();
            return;
        }
        isFragmentReset = false;
        mCountingView.setTime(timerObj.mTimeLeft, false, true);
        if (timerObj.mState == TimerObj.STATE_RUNNING) {
            startClockTicks();
        } else if (timerObj.mState == TimerObj.STATE_STOPPED) {
            set(timerObj.mOriginalLength, timerObj.mTimeLeft, false);
            setFabAppearance();
        } else {
            isFragmentReset = true;
            set(timerObj.mOriginalLength, timerObj.mTimeLeft, false);
            setFabAppearance();
        }
    }

    private void startClockTicks() {
        if (timerObj == null || timerObj.mTimeLeft <= 0) {
            return;
        }
        set(timerObj.mOriginalLength, timerObj.mTimeLeft, false);
        timerObj = new TimerObj(timerObj.mTimeLeft, context);
        timerObj.setState(TimerObj.STATE_RUNNING);
        timerObj.mDeleteAfterUse = true;
        updateTimerState(timerObj, Timers.START_TIMER);
        mTimerView.postDelayed(mClockTick, 20);
        mCircleView.startIntervalAnimation();
        mTicking = true;
        isFragmentReset = false;
        setFabAppearance();
        counting_shape.setEnabled(false);
        timerLabel.setEnabled(false);
        disableTimerSetup(!isFragmentReset);
        Log.d(jyyTAG, jyyTAG + "startClockTicks");
    }

    // Stops the ticks that animate the timers.
    private void stopOrRestartClockTicks() {
        if (mTicking) {
            Log.d(jyyTAG, jyyTAG + "stopOrRestartClockTicks-->" + mTicking);
            mTimerView.removeCallbacks(mClockTick);
            mTicking = false;
            timerObj.updateTimeLeft(true);
            timerObj.mState = TimerObj.STATE_STOPPED;
            updateTimerState(timerObj, Timers.STOP_TIMER);

            timerPause.setImageResource(R.drawable.start_green);
            mCircleView.pauseIntervalAnimation();
        } else {
            Log.d(jyyTAG, jyyTAG + "stopOrRestartClockTicks-->" + mTicking);
            timerObj.refleshTimeLeft(timerObj.mTimeLeft);
            startClockTicks();
        }
    }

    private void disableTimerSetup(boolean isDisable) {
        timer30m.setEnabled(!isDisable);
        timer15m.setEnabled(!isDisable);
        timer10m.setEnabled(!isDisable);
        timer5m.setEnabled(!isDisable);
        if (isDisable) {
            timeGroup.setAlpha(0.0f);
        }
    }

    private void resetClock() {
        if (!mTicking) {
            resetClockImp();
        }else {
            stopOrRestartClockTicks();
            resetClockImp();
        }
    }

    private void resetClockImp() {
        timerObj = new TimerObj(0, context);
        mTimerView.removeCallbacks(mClockTick);
        mCountingView.setTime(0, false, true);
        set(0, 0, false);
        isFragmentReset = true;
        setFabAppearance();
        counting_shape.setEnabled(true);
        timerLabel.setEnabled(true);
        disableTimerSetup(!isFragmentReset);
        Intent intent = new Intent();
        intent.setAction(Timers.RESTART_TIMER);
        intent.putExtra(Timers.TIMER_INTENT_EXTRA, timerObj.mTimerId);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        Log.d(jyyTAG, jyyTAG + "resetClock-->" + mTicking);
        context.sendBroadcast(intent);
    }

    @Override
    public void onCursorMove(long setupTime) {
        quickSetup(setupTime);
    }

    public static boolean BACK_FROM_ALRM = false;

    @Override
    public void backAddOneMin() {
        LogUtils.d(TAG, "backAddOneMin---");
        BACK_FROM_ALRM = true;
    }

    @Override
    public void backNoThing() {
        LogUtils.d(TAG, "backNoThing---");
        BACK_FROM_ALRM = false;
    }
}
