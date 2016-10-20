/*
 * Copyright (C) 2014 MediaTek Inc.
 * Modification based on code covered by the mentioned copyright
 * and/or permission notice(s).
 */
/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.deskclock.alarms;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import com.android.deskclock.HolsterCircleView;
import com.android.deskclock.HolsterUtil;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.SettingsActivity;
import com.android.deskclock.Utils;
import com.android.deskclock.events.Events;
import com.android.deskclock.provider.AlarmInstance;

public class AlarmActivity extends AppCompatActivity implements
		View.OnClickListener, View.OnTouchListener {

	private static final String LOGTAG = AlarmActivity.class.getSimpleName();

	public static final String KEY_AUTO_SILENCE = "auto_silence_";

	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			LogUtils.v(LOGTAG, "Received broadcast: %s", action);

			if (!mAlarmHandled) {
				switch (action) {
				case AlarmService.ALARM_SNOOZE_ACTION:
					snooze();
					break;
				case AlarmService.ALARM_DISMISS_ACTION:
					dismiss();
					break;
				case AlarmService.ALARM_DONE_ACTION:
					// / add lk for power off alarm timeout @{
					if (PowerOffAlarm.bootFromPoweroffAlarm()) {
						mAlarmHandled = true;
						sendBroadcast(new Intent(AlarmService.ALARM_DONE_ACTION));
						sendBroadcast(new Intent(
								AlarmService.NORMAL_BOOT_ACTION));
					}
					// / add lk for power off alarm timeout @}
					finish();
					break;
				// Add for holster
				case HOLSTER_ACTION:
					LogUtils.d(LOGTAG, "HOLSTER_ACTION = " + intent.getAction());
					if (HolsterUtil.isHallExists()) {
						int state = intent.getIntExtra(HOLSTER_ACTION_DATA_KEY,
								0);
						setHolsterState(state);
						if (null != mHolsterContainer && null != content) {
							if (mIsHolsterClosed) {
								mHolsterContainer.setVisibility(View.VISIBLE);
								content.setVisibility(View.GONE);
							} else {
								mHolsterContainer.setVisibility(View.GONE);
								content.setVisibility(View.VISIBLE);
							}
						}
					}
					break;
				default:
					LogUtils.i(LOGTAG, "Unknown broadcast: %s", action);
					break;
				}
			} else {
				LogUtils.v(LOGTAG, "Ignored broadcast: %s", action);
			}
		}
	};

	private final ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			LogUtils.i("Finished binding to AlarmService");
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			LogUtils.i("Disconnected from AlarmService");
		}
	};

	private AlarmInstance mAlarmInstance;
	private boolean mAlarmHandled;
	private String mVolumeBehavior;
	private int mCurrentHourColor;
	private boolean mReceiverRegistered;
	/** Whether the AlarmService is currently bound */
	private boolean mServiceBound;

	private AccessibilityManager mAccessibilityManager;

	private boolean mIsHolsterClosed = false;
	private static final String SP_COLOR_DATA = "data";
	private float mMoveDown = 0;
	private float mMoveUp = 0;
	private HolsterCircleView mBgView;
	private static final String HOLSTER_ACTION = "com.android.systemui.clockuevent";
	private static final String HOLSTER_ACTION_DATA_KEY = "state";

	private ViewGroup mHolsterContainer;
	private View mHolsterView;

	private RelativeLayout content;
	private TextClock digitalClock;
	private TextView label;
	private TextView later;
	private LinearLayout slide;

	private Context mContext;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mContext = this;
		final long instanceId = AlarmInstance.getId(getIntent().getData());
		mAlarmInstance = AlarmInstance.getInstance(getContentResolver(),
				instanceId);
		if (mAlarmInstance == null) {
			// The alarm was deleted before the activity got created, so just
			// finish()
			LogUtils.e(LOGTAG, "Error displaying alarm for intent: %s",
					getIntent());
			finish();
			return;
		} else if (mAlarmInstance.mAlarmState != AlarmInstance.FIRED_STATE) {
			LogUtils.i(LOGTAG, "Skip displaying alarm for instance: %s",
					mAlarmInstance);
			finish();
			return;
		}

		LogUtils.i(LOGTAG, "Displaying alarm for instance: %s", mAlarmInstance);

		// Get the volume/camera button behavior setting
		mVolumeBehavior = PreferenceManager.getDefaultSharedPreferences(this)
				.getString(SettingsActivity.KEY_VOLUME_BEHAVIOR,
						SettingsActivity.DEFAULT_VOLUME_BEHAVIOR);

		getWindow()
				.addFlags(
						WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
								| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
								| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
								| WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
								| WindowManager.LayoutParams.FLAG_FULLSCREEN);
		// /M: Don't show the wallpaper when the alert arrive. @{
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
		// /@}

		// Hide navigation bar to minimize accidental tap on Home key
		hideNavigationBar();

		// Close dialogs and window shade, so this is fully visible
		sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));

		// In order to allow tablets to freely rotate and phones to stick
		// with "nosensor" (use default device orientation) we have to have
		// the manifest start with an orientation of unspecified" and only limit
		// to "nosensor" for phones. Otherwise we get behavior like in b/8728671
		// where tablets start off in their default orientation and then are
		// able to freely rotate.
		if (!getResources().getBoolean(R.bool.config_rotateAlarmAlert)) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
		}

		mAccessibilityManager = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);

		mIsHolsterClosed = HolsterUtil.queryHallState();
		setContentView(R.layout.alarm_activity);
		mHolsterContainer = (ViewGroup) findViewById(R.id.holster_container);
		mHolsterView = LayoutInflater.from(this).inflate(
				R.layout.alarm_holster_activity, null);

		content = (RelativeLayout) findViewById(R.id.content);
		digitalClock = (TextClock) findViewById(R.id.digital_clock);
		label = (TextView) findViewById(R.id.label);
		later = (TextView) findViewById(R.id.later);
		slide = (LinearLayout) findViewById(R.id.slide);
		slide.setOnTouchListener(this);
		later.setOnClickListener(this);

		mCurrentHourColor = Utils.getCurrentHourColor();
		getWindow().setBackgroundDrawable(new ColorDrawable(mCurrentHourColor));
		mHolsterContainer.addView(mHolsterView);
		TextView alertTitleView = (TextView) mHolsterView
				.findViewById(R.id.holster_title);
		alertTitleView.setText(mAlarmInstance.getLabelOrDefault(this));
		label.setText(mAlarmInstance.getLabelOrDefault(this));
		mBgView = (HolsterCircleView) mHolsterView
				.findViewById(R.id.bg_content);

		Utils.setTimeFormat(this, digitalClock, getResources()
				.getDimensionPixelSize(R.dimen.main_ampm_font_size));
		getWindow().setBackgroundDrawable(new ColorDrawable(mCurrentHourColor));

		// Set the animators to their initial values.
		mHolsterContainer.setOnTouchListener(this);
		int sColor = getSharedPreferences(SP_COLOR_DATA, Context.MODE_PRIVATE)
				.getInt(SP_COLOR_DATA, mCurrentHourColor);
		mBgView.setCircleColor(sColor);
	}

	@Override
	protected void onStart() {
		super.onStart();

		// Bind to AlarmService
		bindService(new Intent(this, AlarmService.class), mConnection,
				Context.BIND_AUTO_CREATE);
		mServiceBound = true;
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (HolsterUtil.queryHallState()) {
			content.setVisibility(View.GONE);
			mHolsterContainer.setVisibility(View.VISIBLE);
		} else {
			mHolsterContainer.setVisibility(View.GONE);
			content.setVisibility(View.VISIBLE);
		}

		// Re-query for AlarmInstance in case the state has changed externally
		final long instanceId = AlarmInstance.getId(getIntent().getData());
		mAlarmInstance = AlarmInstance.getInstance(getContentResolver(),
				instanceId);

		if (mAlarmInstance == null) {
			LogUtils.i(LOGTAG, "No alarm instance for instanceId: %d",
					instanceId);
			finish();
			return;
		}

		// Verify that the alarm is still firing before showing the activity
		if (mAlarmInstance.mAlarmState != AlarmInstance.FIRED_STATE) {
			LogUtils.i(LOGTAG, "Skip displaying alarm for instance: %s",
					mAlarmInstance);
			finish();
			return;
		}

		if (!mReceiverRegistered) {
			// Register to get the alarm done/snooze/dismiss intent.
			final IntentFilter filter = new IntentFilter(
					AlarmService.ALARM_DONE_ACTION);
			filter.addAction(AlarmService.ALARM_SNOOZE_ACTION);
			filter.addAction(AlarmService.ALARM_DISMISS_ACTION);
			filter.addAction(HOLSTER_ACTION);
			registerReceiver(mReceiver, filter);
			mReceiverRegistered = true;
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		unbindAlarmService();

		// Skip if register didn't happen to avoid IllegalArgumentException
		if (mReceiverRegistered) {
			unregisterReceiver(mReceiver);
			mReceiverRegistered = false;
		}
	}

	@Override
	public boolean dispatchKeyEvent(@NonNull KeyEvent keyEvent) {
		// Do this in dispatch to intercept a few of the system keys.
		LogUtils.v(LOGTAG, "dispatchKeyEvent: %s", keyEvent);

		switch (keyEvent.getKeyCode()) {
		// Volume keys and camera keys dismiss the alarm.
		case KeyEvent.KEYCODE_POWER:
		case KeyEvent.KEYCODE_VOLUME_UP:
		case KeyEvent.KEYCODE_VOLUME_DOWN:
		case KeyEvent.KEYCODE_VOLUME_MUTE:
		case KeyEvent.KEYCODE_CAMERA:
		case KeyEvent.KEYCODE_FOCUS:
			if (!mAlarmHandled && keyEvent.getAction() == KeyEvent.ACTION_UP) {
				switch (mVolumeBehavior) {
				case SettingsActivity.VOLUME_BEHAVIOR_SNOOZE:
					snooze();
					break;
				case SettingsActivity.VOLUME_BEHAVIOR_DISMISS:
					dismiss();
					break;
				default:
					break;
				}
			}
			return true;
		default:
			return super.dispatchKeyEvent(keyEvent);
		}
	}

	@Override
	public void onBackPressed() {
		// Don't allow back to dismiss.
	}

	@Override
	public void onClick(View view) {
		if (mAlarmHandled) {
			LogUtils.v(LOGTAG, "onClick ignored: %s", view);
			return;
		}
		LogUtils.v(LOGTAG, "onClick: %s", view);
		switch (view.getId()) {
		case R.id.later:
			later.setTextColor(Color.GREEN);
			snooze();
			break;

		default:
			break;
		}
		// If in accessibility mode, allow snooze/dismiss by double tapping on
		// respective icons.
	}

	@Override
	public boolean onTouch(View view, MotionEvent motionEvent) {
		Log.d(LOGTAG, "onTouch");
		if (mAlarmHandled) {
			LogUtils.v(LOGTAG, "onTouch ignored: %s", motionEvent);
			return false;
		}
		float dissmissDistance = slide.getWidth();
		Log.d(LOGTAG, "dissmissDistance=" + dissmissDistance);
		final int[] contentLocation = { 0, 0 };
		content.getLocationOnScreen(contentLocation);

		final float x = motionEvent.getRawX() - contentLocation[0];
		final float y = motionEvent.getRawY() - contentLocation[1];

		switch (motionEvent.getActionMasked()) {
		case MotionEvent.ACTION_DOWN:
			LogUtils.v(LOGTAG, "onTouch started: %s", motionEvent);

			// Stop the pulse, allowing the last pulse to finish.
			mMoveDown = motionEvent.getX();
			Log.d(LOGTAG, "mMoveDown=" + mMoveDown);
			break;
		case MotionEvent.ACTION_UP:
			LogUtils.v(LOGTAG, "onTouch ended: %s", motionEvent);
			mMoveUp = motionEvent.getX();
			Log.d(LOGTAG, "mMoveUp=" + mMoveUp);
			Log.d(LOGTAG, "mIsHolsterClosed=" + mIsHolsterClosed);
			if ((mMoveUp - mMoveDown) >= dissmissDistance / 2) {
				slide.setBackgroundColor(Color.GREEN);
				dismiss();
			}
			break;
		case MotionEvent.ACTION_CANCEL:
			break;
		default:
			break;
		}
		return true;
	}

	@TargetApi(Build.VERSION_CODES.KITKAT)
	private void hideNavigationBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			getWindow().getDecorView().setSystemUiVisibility(
					View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
							| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
							| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
		}
	}

	/**
	 * Perform snooze animation and send snooze intent.
	 */
	private void snooze() {
		mAlarmHandled = true;
		LogUtils.v(LOGTAG, "Snoozed: %s", mAlarmInstance);

		// / add for power off alarm 20160830 @{
		if (PowerOffAlarm.bootFromPoweroffAlarm()) {
			AlarmStateManager.setSnoozeState(this, mAlarmInstance, false);
			Events.sendAlarmEvent(R.string.action_dismiss,
					R.string.label_deskclock);
			sendBroadcast(new Intent(AlarmService.NORMAL_BOOT_ACTION));
			finish();
		} else
		// / @}
		{
			final int accentColor = Utils.obtainStyledColor(this,
					R.attr.colorAccent, Color.RED);
			final int snoozeMinutes = AlarmStateManager.getSnoozedMinutes(this);
			final String infoText = getResources().getQuantityString(
					R.plurals.alarm_alert_snooze_duration, snoozeMinutes,
					snoozeMinutes);
			final String accessibilityText = getResources().getQuantityString(
					R.plurals.alarm_alert_snooze_set, snoozeMinutes,
					snoozeMinutes);
			Toast.makeText(mContext, accessibilityText, Toast.LENGTH_LONG).show();
			finish();
			AlarmStateManager
					.setSnoozeState(this, mAlarmInstance, false /* showToast */);

			Events.sendAlarmEvent(R.string.action_dismiss,
					R.string.label_deskclock);

			// Unbind here, otherwise alarm will keep ringing until activity
			// finishes.
			unbindAlarmService();
		}
	}

	/**
	 * Perform dismiss animation and send dismiss intent.
	 */
	private void dismiss() {
		mAlarmHandled = true;
		LogUtils.v(LOGTAG, "Dismissed: %s", mAlarmInstance);

		// / add for power off alarm 20160830 @{
		if (PowerOffAlarm.bootFromPoweroffAlarm()) {
			AlarmStateManager.setDismissState(this, mAlarmInstance);
			Events.sendAlarmEvent(R.string.action_dismiss,
					R.string.label_deskclock);
			sendBroadcast(new Intent(AlarmService.NORMAL_BOOT_ACTION));
			finish();
		} else
		// / @}
		{
			if (mIsHolsterClosed) {
				finish();
			} else {
				Toast.makeText(mContext, R.string.alarm_alert_off_text,
						Toast.LENGTH_LONG).show();
			}
			finish();
			AlarmStateManager.setDismissState(this, mAlarmInstance);
			Events.sendAlarmEvent(R.string.action_dismiss,
					R.string.label_deskclock);

			// Unbind here, otherwise alarm will keep ringing until activity
			// finishes.
			unbindAlarmService();
		}
	}

	/**
	 * Unbind AlarmService if bound.
	 */
	private void unbindAlarmService() {
		if (mServiceBound) {
			unbindService(mConnection);
			mServiceBound = false;
		}
	}

	/**
	 * Add: Set the holster state
	 */
	private boolean setHolsterState(int receiverEvent) {
		if (1 == receiverEvent) {
			mIsHolsterClosed = true;
		} else if (0 == receiverEvent) {
			mIsHolsterClosed = false;
		}
		return mIsHolsterClosed;
	}
	
	private String getAlarmSilentAfterStr(long id){
		if (id == 0) {
			return "10";
		}
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(mContext);
		return prefs.getString(KEY_AUTO_SILENCE+id, "10");
	}
}
