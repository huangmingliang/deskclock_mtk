package com.android.deskclock.timer;

import com.android.deskclock.CircleTimerView;
import com.android.deskclock.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class TimerListItem3 extends LinearLayout{


    CountingTimerView mTimerText;
    CircleTimerView mCircleView;
    ImageView mReset;
    ImageView mPause;
    RelativeLayout timer30m;
    RelativeLayout timer15m;
    RelativeLayout timer10m;
    RelativeLayout timer5m;
    

    long mTimerLength;

    public TimerListItem3(Context context) {
        this(context, null);
    }

    public TimerListItem3(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mTimerText = (CountingTimerView) findViewById(R.id.timer_time_text);
        mCircleView = (CircleTimerView) findViewById(R.id.timer_time);
        mReset = (ImageView) findViewById(R.id.timer_reset);
        mPause=(ImageView)findViewById(R.id.timer_pause);
        timer30m=(RelativeLayout)findViewById(R.id.timer30m);
        timer15m=(RelativeLayout)findViewById(R.id.timer15m);
        timer10m=(RelativeLayout)findViewById(R.id.timer10m);
        timer5m=(RelativeLayout)findViewById(R.id.timer5m);
        mCircleView.setTimerMode(true);
    }

    public void set(long timerLength, long timeLeft, boolean drawRed) {
        if (mCircleView == null) {
            mCircleView = (CircleTimerView) findViewById(R.id.timer_time);
            mCircleView.setTimerMode(true);
        }
        mTimerLength = timerLength;
        mCircleView.setIntervalTime(mTimerLength);
        mCircleView.setPassedTime(timerLength - timeLeft, drawRed);
        invalidate();
    }

    public void start() {
        mReset.setImageResource(R.drawable.reset_green);
        mPause.setImageResource(R.drawable.pause_green);
        mReset.setContentDescription(getResources().getString(R.string.timer_plus_one));
        mCircleView.startIntervalAnimation();
        mTimerText.setTimeStrTextColor(false, true);
        mTimerText.showTime(true);
        mCircleView.setVisibility(VISIBLE);
    }

    public void pause() {
        mReset.setImageResource(R.drawable.reset_green);
        mPause.setImageResource(R.drawable.start_green);
        mReset.setContentDescription(getResources().getString(R.string.timer_reset));
        mCircleView.pauseIntervalAnimation();
        mTimerText.setTimeStrTextColor(false, true);
        mTimerText.showTime(true);
        mCircleView.setVisibility(VISIBLE);
    }

    public void stop() {
        /// M: When timer stop, change +1 button to reset.
        mReset.setImageResource(R.drawable.ic_reset);
        mReset.setContentDescription(getResources().getString(R.string.timer_reset));
        mCircleView.stopIntervalAnimation();
        mTimerText.setTimeStrTextColor(false, true);
        mTimerText.showTime(true);
        mCircleView.setVisibility(VISIBLE);
    }

    public void timesUp() {
        mCircleView.abortIntervalAnimation();
        mTimerText.setTimeStrTextColor(true, true);
    }

    public void done() {
        mCircleView.stopIntervalAnimation();
        mCircleView.setVisibility(VISIBLE);
        mCircleView.invalidate();
        mTimerText.setTimeStrTextColor(true, false);
    }

    public void setLength(long timerLength) {
        mTimerLength = timerLength;
        mCircleView.setIntervalTime(mTimerLength);
        mCircleView.invalidate();
    }

    public void setTextBlink(boolean blink) {
        mTimerText.showTime(!blink);
    }

    public void setCircleBlink(boolean blink) {
        mCircleView.setVisibility(blink ? INVISIBLE : VISIBLE);
    }

    public void setResetAddButton(boolean isRunning, OnClickListener listener) {
        if (mReset == null) {
            mReset = (ImageView) findViewById(R.id.reset_add);
        }
        mReset.setImageResource(isRunning ? R.drawable.ic_plusone :
                R.drawable.ic_reset);
        mReset.setContentDescription(getResources().getString(
                isRunning ? R.string.timer_plus_one : R.string.timer_reset));
        mReset.setOnClickListener(listener);
    }

    public void setTime(long time, boolean forceUpdate) {
        if (mTimerText == null) {
            mTimerText = (CountingTimerView) findViewById(R.id.timer_time_text);
        }
        mTimerText.setTime(time, false, forceUpdate);
    }


}
