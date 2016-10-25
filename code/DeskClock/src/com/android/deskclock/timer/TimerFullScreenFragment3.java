package com.android.deskclock.timer;

import java.util.ArrayList;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.deskclock.DeskClockFragment;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.events.Events;

public class TimerFullScreenFragment3 extends DeskClockFragment implements
		OnClickListener, OnSharedPreferenceChangeListener {
	private String TAG=getClass().getSimpleName();
	private static final String TWO_DIGITS = "%02d";
	private static final String ONE_DIGIT = "%01d";
	private TextView mTimerText;
	private TextView mAddOneMinuteBtn;
	private ImageView mStopBtn;
	private SharedPreferences mPrefs;
	private ArrayList<TimerObj> mTimer=new ArrayList<TimerObj>();
	private TimerObj mAlertTimer;
	private NotificationManager mNotificationManager;
	private TimerAlertListener listener;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		initData();
		
	}
	
	private void initData(){
		mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		mNotificationManager = (NotificationManager)
                getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
		TimerObj.getTimersFromSharedPrefs(mPrefs, mTimer);
		if (mTimer!=null&&mTimer.size()>0) {
			mAlertTimer=mTimer.get(0);
			Log.d(TAG,"mAlertTimer.mState="+mAlertTimer.mState);
		}
		listener=(TimerAlertListener) getActivity();
	}
	private void initView(View view){
		mAddOneMinuteBtn=(TextView)view.findViewById(R.id.add_one);
		mTimerText=(TextView)view.findViewById(R.id.timer);
		mTimerText.setText(getTimeStr(mAlertTimer.mOriginalLength));
		mStopBtn=(ImageView)view.findViewById(R.id.alert_stop);
		mAddOneMinuteBtn.setOnClickListener(this);
		mStopBtn.setOnClickListener(this);
	}
	

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		View view=inflater.inflate(R.layout.timer_full_alert, null);
		initView(view);
		return view;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.alert_stop:
			onStopButtonPressed(mAlertTimer);
			break;
        case R.id.add_one:
        	onPlusOneButtonPressed(mAlertTimer);
        	break;
		default:
			break;
		}
	}
	
	 private void onPlusOneButtonPressed(TimerObj t) {
	        switch (t.mState) {
	            case TimerObj.STATE_RUNNING:
	                t.addTime(TimerObj.MINUTE_IN_MILLIS);
	                long timeLeft = t.updateTimeLeft(false);
	                //t.mView.setTime(timeLeft, false);
	                //t.mView.setLength(timeLeft);
	                updateTimersState(t, Timers.TIMER_UPDATE);

	                Events.sendTimerEvent(R.string.action_add_minute, R.string.label_deskclock);
	                break;
	            case TimerObj.STATE_TIMESUP:
	                // +1 min when the time is up will restart the timer with 1 minute left.
	                t.setState(TimerObj.STATE_RUNNING);
	                t.mStartTime = Utils.getTimeNow();
	                t.mTimeLeft = t.mOriginalLength = TimerObj.MINUTE_IN_MILLIS;
	                updateTimersState(t, Timers.RESET_TIMER);
	                Events.sendTimerEvent(R.string.action_add_minute, R.string.label_deskclock);

	                updateTimersState(t, Timers.START_TIMER);
	                Events.sendTimerEvent(R.string.action_start, R.string.label_deskclock);
	                listener.onTimerChanged();
	                cancelTimerNotification(t.mTimerId);
	                break;
	            case TimerObj.STATE_STOPPED:
	                t.setState(TimerObj.STATE_RESTART);
	                t.mTimeLeft = t.mOriginalLength = t.mSetupLength;
	                //t.mView.stop();
	                //t.mView.setTime(t.mTimeLeft, false);
	                //t.mView.set(t.mOriginalLength, t.mTimeLeft, false);
	                updateTimersState(t, Timers.RESET_TIMER);

	                Events.sendTimerEvent(R.string.action_reset, R.string.label_deskclock);
	                break;
	            default:
	                break;
	        }
	    }
	 
	 private void updateTimersState(TimerObj t, String action) {
		    Log.d(TAG, "updateTimersState mAlertTimer.mState"+mAlertTimer.mState);
	        if (Timers.DELETE_TIMER.equals(action)) {
	            LogUtils.e("~~ update timer state");
	            t.deleteFromSharedPref(mPrefs);
	        } else {
	            t.writeToSharedPref(mPrefs);
	        }
	        Intent i = new Intent();
	        i.setAction(action);
	        i.putExtra(Timers.TIMER_INTENT_EXTRA, t.mTimerId);
	        // Make sure the receiver is getting the intent ASAP.
	        i.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
	        getActivity().sendBroadcast(i);
	    }

	
	public String getTimeStr(long time) {
        long seconds, minutes, hours;
        String mHours,mMinutes,mSeconds;
        String format;
        seconds = time / 1000;
        minutes = seconds / 60;
        seconds = seconds - minutes * 60;
        hours = minutes / 60;
        minutes = minutes - hours * 60;
        if (hours > 999) {
            hours = 0;
        }
        // The time  can be between 0 and -1 seconds, but the "truncated" equivalent time of hours
        // and minutes and seconds could be zero, so since we do not show fractions of seconds
        // when counting down, do not show the minus sign.
        // TODO:does it matter that we do not look at showHundredths?
        if (hours >= 10) {
            format = TWO_DIGITS;
            mHours = String.format(format, hours);
        } else if (hours > 0) {
            format = TWO_DIGITS;
            mHours = String.format(format, hours);
        } else {
        	mHours=null;
        	
        }

        // Minutes are never empty and when hours are non-empty, must be two digits
        if (minutes >= 10 || hours > 0) {
            //format = (showNeg && hours == 0) ? NEG_TWO_DIGITS : TWO_DIGITS;
        	/// add 20160824 modify bug1004 @{
        	if(hours >0){
        		format =":"+TWO_DIGITS;
        	}else {
        		format = TWO_DIGITS;
			}
        	/// @}
            mMinutes = String.format(format, minutes);
        } else {
            format = TWO_DIGITS;
            mMinutes = String.format(format, minutes);
        }

        // Seconds are always two digits
        //mSeconds = String.format(TWO_DIGITS, seconds);
        /// add 20160824 modify bug1004 @{
        format=":" + TWO_DIGITS;
        mSeconds = String.format(format, seconds);
        /// @}
        
        StringBuffer sb=new StringBuffer();
        if (!TextUtils.isEmpty(mHours)) {
        	 sb.append(mHours);
		}
        if (!TextUtils.isEmpty(mMinutes)) {
       	 sb.append(mMinutes);
		}
        if (!TextUtils.isEmpty(mSeconds)) {
       	 sb.append(mSeconds);
		}
        return sb.toString();
	}
	
	 private void cancelTimerNotification(int timerId) {
	        mNotificationManager.cancel(timerId);
	    }
	
	 private void onStopButtonPressed(TimerObj t) {
	        switch (t.mState) {
	            case TimerObj.STATE_RUNNING:
	                // Stop timer and save the remaining time of the timer
	                t.setState(TimerObj.STATE_STOPPED);
	                //t.mView.pause();
	                t.updateTimeLeft(true);
	                updateTimersState(t, Timers.STOP_TIMER);

	                Events.sendTimerEvent(R.string.action_stop, R.string.label_deskclock);
	                break;
	            case TimerObj.STATE_STOPPED:
	                // Reset the remaining time and continue timer
	                t.setState(TimerObj.STATE_RUNNING);
	                t.mStartTime = Utils.getTimeNow() - (t.mOriginalLength - t.mTimeLeft);
	                //t.mView.start();
	                updateTimersState(t, Timers.START_TIMER);
	                Events.sendTimerEvent(R.string.action_start, R.string.label_deskclock);
	                break;
	            case TimerObj.STATE_TIMESUP:
	                if (t.mDeleteAfterUse) {
	                    cancelTimerNotification(t.mTimerId);
	                    // Tell receiver the timer was deleted.
	                    // It will stop all activity related to the
	                    // timer
	                    t.setState(TimerObj.STATE_DELETED);
	                    updateTimersState(t, Timers.DELETE_TIMER);
	                    Events.sendTimerEvent(R.string.action_delete, R.string.label_deskclock);
	                    listener.onTimerRemove();
	                } else {
	                    resetTimer(t);
	                }
	                break;
	            case TimerObj.STATE_RESTART:
	                t.setState(TimerObj.STATE_RUNNING);
	                t.mStartTime = Utils.getTimeNow() - (t.mOriginalLength - t.mTimeLeft);
	                //t.mView.start();
	                updateTimersState(t, Timers.START_TIMER);
	                Events.sendTimerEvent(R.string.action_start, R.string.label_deskclock);
	                break;
	            default:
	                break;
	        }
	    }
	 
	 private void resetTimer(TimerObj t) {
	        /// M: Safely return when the view is null, for the view had disappear from the screen.
	        //if (null == t.mView) {
	          //  return;
	       // }
	        t.setState(TimerObj.STATE_RESTART);
	        t.mTimeLeft = t.mOriginalLength = t.mSetupLength;
	        // when multiple timers are firing, some timers will be off-screen and they will not
	        // have Fragment instances unless user scrolls down further. t.mView is null in this case.
	        /*if (t.mView != null) {
	            t.mView.stop();
	            t.mView.setTime(t.mTimeLeft, false);
	            t.mView.set(t.mOriginalLength, t.mTimeLeft, false);
	        }*/
	        updateTimersState(t, Timers.RESET_TIMER);
	        Events.sendTimerEvent(R.string.action_reset, R.string.label_deskclock);
	    }
	 
	 interface TimerAlertListener{
	    	abstract public void onTimerRemove();
	    	abstract public void onTimerChanged();
	    }
	 

}
