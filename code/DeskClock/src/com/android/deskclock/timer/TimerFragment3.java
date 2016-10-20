package com.android.deskclock.timer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.telecom.Log;
import android.text.format.DateUtils;
import android.transition.AutoTransition;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.android.deskclock.AnimatorUtils;
import com.android.deskclock.CircleTimerView;
import com.android.deskclock.CircleTimerView.CursorMoveListener;
import com.android.deskclock.DeskClock;
import com.android.deskclock.DeskClockFragment;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.TimerSetupView;
import com.android.deskclock.Utils;
import com.android.deskclock.VerticalViewPager;
import com.android.deskclock.events.Events;

public class TimerFragment3 extends DeskClockFragment implements OnSharedPreferenceChangeListener,OnClickListener,CursorMoveListener{

	private String TAG=getClass().getSimpleName();
    public static final long ANIMATION_TIME_MILLIS = DateUtils.SECOND_IN_MILLIS / 3;
    public static final String TIME_OBJ="time_obj";
    // Transitions are available only in API 19+
    private static final boolean USE_TRANSITION_FRAMEWORK =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    private boolean mTicking = false;
    private Transition mDeleteTransition;
    private SharedPreferences mPrefs;
    private Bundle mViewState = null;
    private NotificationManager mNotificationManager;
    
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
    private boolean isFragmentReset=true;
    private Context context;
    private TimerObj3 timerObj3;
    
    final private Runnable mClockTick=new Runnable() {
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			Log.d(TAG, "ticking...");
			long timeLeft=0;
			if (timerObj3.mState==TimerObj.STATE_RUNNING) {
				 timeLeft = timerObj3.updateTimeLeft(false);
				 mCountingView.setTime(timeLeft, false, false);
				 mCircleView.setTimerMode(true);
			}
			if (timeLeft>0) {
				mTimerView.postDelayed(mClockTick, 20);
			}else {
				mTicking=false;
				timerObj3.mState=TimerObj.STATE_STOPPED;
				resetClock();
			}
			
		}
	};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.timer_list_item_3, container, false);
        mTimerView=(FrameLayout)view.findViewById(R.id.timer_view);
        mCircleView=(CircleTimerView)view.findViewById(R.id.timer_time);
        mCountingView=(CountingTimerView)view.findViewById(R.id.timer_time_text);
        timer30m=(RelativeLayout)view.findViewById(R.id.timer30m);
        timer15m=(RelativeLayout)view.findViewById(R.id.timer15m);
        timer10m=(RelativeLayout)view.findViewById(R.id.timer10m);
        timer5m=(RelativeLayout)view.findViewById(R.id.timer5m);
        timerReset=(ImageView)view.findViewById(R.id.timer_reset);
        timerPause=(ImageView)view.findViewById(R.id.timer_pause);
        timerLabel=(Button)view.findViewById(R.id.timer_label);
        mCircleView.setCircleTimeViewListener(this);
        timer30m.setOnClickListener(this);
        timer15m.setOnClickListener(this);
        timer10m.setOnClickListener(this);
        timer5m.setOnClickListener(this);
        timerLabel.setOnClickListener(this);
        timerPause.setOnClickListener(this);
        timerReset.setOnClickListener(this);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG,"onActivityCreated");
        context = getActivity();
        mCircleView.diliveryActivityObj(getActivity());
        mViewState=savedInstanceState;
        Log.d(TAG, "mViewState="+mViewState);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mNotificationManager = (NotificationManager) context.getSystemService(Context
                .NOTIFICATION_SERVICE);
        restoreTimerClock();
    }

    @Override
    public void onResume() {
        super.onResume();
        final DeskClock activity = (DeskClock) getActivity();
        if (activity.getSelectedTab() == DeskClock.ALARM_TAB_INDEX) {
            setFabAppearance();
            //setLeftRightButtonAppearance();
        }
        mPrefs.registerOnSharedPreferenceChangeListener(this);

        if (mPrefs.getBoolean(Timers.REFRESH_UI_WITH_LATEST_DATA, false)) {
            // Clear the flag indicating the adapter is out of sync with the database.
            mPrefs.edit().putBoolean(Timers.REFRESH_UI_WITH_LATEST_DATA, false).apply();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() instanceof DeskClock) {
            ((DeskClock) getActivity()).unregisterPageChangedListener(this);
        }
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        /// M: Don't save timer now, in case old data are writed to sharePreference @{
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG,"onSaveInstanceState");
        outState.putParcelable(TIME_OBJ, timerObj3);
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
     * @param update indicates whether to call updateNextTimesup in TimerReceiver. This is false
     *               only for label changes.
     */
    private void updateTimerState(TimerObj t, String action, boolean update) {
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
        //mCircleView.setTimerMode(true);
        mCircleView.setIntervalTime(timerLength);
        mCircleView.setPassedTime(timerLength - timeLeft, drawRed);
        mCircleView.invalidate();
    }
    
    @Override
    public void onFabClick(View view) {
    	startClockTicks();
    	
    }

    @Override
    public void setFabAppearance() { 
    	final DeskClock activity = (DeskClock) getActivity();
    /// M: Activity maybe null when configuration change
    if (activity == null || mFab == null) {
        return;
    }

    if (activity.getSelectedTab() != DeskClock.TIMER_TAB_INDEX) {
        mFab.setVisibility(View.VISIBLE);
        return;
    }
    Log.d(TAG,"isFragmentReset="+isFragmentReset);
    if (isFragmentReset) {
    	mFab.setVisibility(View.VISIBLE);
    	mFab.setImageResource(R.drawable.start_green);
        mFab.setContentDescription(getString(R.string.timer_start));
        timerReset.setVisibility(View.GONE);
 		timerPause.setVisibility(View.GONE);
	}else {
		mFab.setVisibility(View.GONE);
		timerReset.setVisibility(View.VISIBLE);
		timerPause.setVisibility(View.VISIBLE);
		if (mTicking) {
			 timerPause.setImageResource(R.drawable.pause_green);
		}else {
			timerPause.setImageResource(R.drawable.start_green);
		}
		
	}
        
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (prefs.equals(mPrefs)) {
            if (key.equals(Timers.REFRESH_UI_WITH_LATEST_DATA) && prefs.getBoolean(key, false)) {
                // Clear the flag forcing a refresh of the adapter to reflect external changes.
                mPrefs.edit().putBoolean(key, false).apply();
                setFabAppearance();
            }
        }
    }

    public void setLabel(TimerObj timer, String label) {
    	
    }

	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		switch (arg0.getId()) {
		case R.id.timer30m:
			quickSetup(30*60*1000);
			break;
		case R.id.timer15m:
			quickSetup(15*60*1000);
			break;
		case R.id.timer10m:
			quickSetup(10*60*1000);
			break;
		case R.id.timer5m:
			quickSetup(5*60*1000);
			break;
		case R.id.timer_pause:
			stopOrRestartClockTicks();
			break;
		case R.id.timer_reset:
			resetClock();
			break;
		default:
			break;
		}
	}
	
	private void quickSetup(long timeLength){
		timerObj3=new TimerObj3(timeLength, context);
		mCountingView.setTime(timerObj3.mTimeLeft, false, true);
		set(timerObj3.mOriginalLength, timerObj3.mTimeLeft, false);
		setFabAppearance();
	}
	
	private void restoreTimerClock(){
		if (mViewState==null) {
			Log.d(TAG,"mViewState==null||timerObj3==null");
			return;
		}
		timerObj3=(TimerObj3) mViewState.get(TIME_OBJ);
		Log.d(TAG, "isFragmentReset="+isFragmentReset);
		if (timerObj3==null) {
			setFabAppearance();
			return;
		}
		isFragmentReset=false;
		mCountingView.setTime(timerObj3.mTimeLeft, false, true);
		if (timerObj3.mState==TimerObj3.STATE_RUNNING) {
			startClockTicks();
		}else if (timerObj3.mState==TimerObj3.STATE_STOPPED) {
			set(timerObj3.mOriginalLength, timerObj3.mTimeLeft, false);
			setFabAppearance();
		}else {
			isFragmentReset=true;
			set(timerObj3.mOriginalLength, timerObj3.mTimeLeft, false);
			setFabAppearance();
		}
	}
	 private void startClockTicks() {
		 if (timerObj3==null||timerObj3.mTimeLeft<=0) {
			return;
		}
		    set(timerObj3.mOriginalLength, timerObj3.mTimeLeft, false);
		    timerObj3=new TimerObj3(timerObj3.mTimeLeft, context);
		    timerObj3.setState(TimerObj3.STATE_RUNNING);
	        mTimerView.postDelayed(mClockTick, 20);
	        mCircleView.startIntervalAnimation();
	        mTicking = true;
	        isFragmentReset=false;
	        setFabAppearance();
	        disableTimerSetup(!isFragmentReset);
	    }

	    // Stops the ticks that animate the timers.
	   private void stopOrRestartClockTicks() {
	        if (mTicking) {
	            mTimerView.removeCallbacks(mClockTick);
	            mTicking = false;
	            timerObj3.mState=TimerObj3.STATE_STOPPED;
	            timerPause.setImageResource(R.drawable.start_green);
	            mCircleView.pauseIntervalAnimation();
	        }else {
	        	timerObj3.refleshTimeLeft(timerObj3.mTimeLeft);
				startClockTicks();
			}
	    }
	   
	   private void disableTimerSetup(boolean isDisable){
			timer30m.setEnabled(!isDisable);
			timer15m.setEnabled(!isDisable);
			timer10m.setEnabled(!isDisable);
			timer5m.setEnabled(!isDisable);
	   }
	    
	    
	  private void resetClock(){
		  if (!mTicking) {
			timerObj3=new TimerObj3(0, context);
			mTimerView.removeCallbacks(mClockTick);
			mCountingView.setTime(0, false, true);
			set(0, 0, false);
			mTicking=false;
		    isFragmentReset=true;
		    setFabAppearance();
		}
		  disableTimerSetup(!isFragmentReset);
	  }

	@Override
	public void onCursorMove(long setupTime) {
		// TODO Auto-generated method stub
		quickSetup(setupTime);
	}

}
