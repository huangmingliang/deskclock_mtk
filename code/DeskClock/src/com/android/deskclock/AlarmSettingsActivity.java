package com.android.deskclock;

import java.io.File;
import java.util.Calendar;
import java.util.HashSet;

import com.android.deskclock.LabelDialogFragment3.LabelDialogListener;
import com.android.deskclock.alarms.AlarmStateManager;
import com.android.deskclock.alarms.PowerOffAlarm;
import com.android.deskclock.events.Events;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.DaysOfWeek;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.TimePicker.OnTimeChangedListener;

public class AlarmSettingsActivity extends Activity implements OnClickListener,
		OnItemClickListener, LabelDialogListener {

	public static final String SYSTEM_SETTINGS_ALARM_ALERT = "content://settings/system/alarm_alert";
	private static final int REQUEST_CODE_RINGTONE = 1;
	private static final int REQUEST_CODE_PERMISSIONS = 2;

	private String TAG = getClass().getSimpleName();
	private Context mContext;
	private Alarm mAlarm;
	private CompoundButton[] dayButtons = new CompoundButton[7];
	private int[] mDayOrder;
	private LinearLayout content;
	private TimePicker mTimePicker;
	private LinearLayout mDayButtons;
	private TextView mRingtong;
	private TextView mSilentAfter;
	private TextView mLabel;
	private RelativeLayout mSilentRL, mRingtongRL, mLabelRL;
	private ImageButton cancel, save;
	private TextView delete;
	private String[] values;
	private String[] entries;
	private String value;
	private String entry;
	private boolean isAdd = false;
	private ListView mSilentList;
	private TextView mCancelBtn;
	private PopupWindow window;
	private View mWindowView;
	private int mChosedSilentPosition = -1;
	private SilentAfterDialogFragment dialog = new SilentAfterDialogFragment();
	private LabelDialogFragment3 mLabelDialog = new LabelDialogFragment3();
	private final int[] DAY_ORDER = new int[] { Calendar.SUNDAY,
			Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
			Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_alarm_settings);
		mContext = this;
		entries = mContext.getResources().getStringArray(
				R.array.auto_silence_entries);
		values = mContext.getResources().getStringArray(
				R.array.auto_silence_values);
		entry = entries[0];
		value = values[0];
		mLabelDialog.setLabelDialogListener(this);
		Intent intent = getIntent();
		mAlarm = intent.getParcelableExtra("alarm");
		if (mAlarm == null) {
			mAlarm = new Alarm();
			isAdd = true;
		}
		initView();

	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		setDayOrder();
		initData();

	}

	private void initData() {

		mTimePicker.setIs24HourView(DateFormat.is24HourFormat(mContext));
		initTimePicker(mAlarm);
		initDayButton(mAlarm);
		initRingtong(mAlarm);
		initSilentAfter(mAlarm);
		initDeleteButton(mAlarm);
		initLabel(mAlarm);
		initWindowView();
	}

	private void initView() {
		content = (LinearLayout) findViewById(R.id.main);
		mTimePicker = (TimePicker) findViewById(R.id.timePicker);
		mDayButtons = (LinearLayout) findViewById(R.id.dayBtns);
		mRingtong = (TextView) findViewById(R.id.choose_ringtone);
		mRingtongRL = (RelativeLayout) findViewById(R.id.ringtong_rl);
		mSilentRL = (RelativeLayout) findViewById(R.id.silent_rl);
		mLabelRL = (RelativeLayout) findViewById(R.id.label_rl);
		mSilentAfter = (TextView) findViewById(R.id.silent_after);
		mLabel = (TextView) findViewById(R.id.label);
		cancel = (ImageButton) findViewById(R.id.alarm_cancel);
		save = (ImageButton) findViewById(R.id.alarm_ok);
		delete = (TextView) findViewById(R.id.delete_alarm);
		cancel.setOnClickListener(this);
		save.setOnClickListener(this);
		initDayBtnView();

	}

	private void initDeleteButton(final Alarm mAlarm) {
		if (!isAdd) {
			delete.setVisibility(View.VISIBLE);
		}
		delete.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				asyncDeleteAlarm(mAlarm);
			}
		});
	}

	private void initDayBtnView() {
		LayoutInflater inflater = LayoutInflater.from(mContext);
		for (int i = 0; i < 7; i++) {
			final CompoundButton dayButton = (CompoundButton) inflater.inflate(
					R.layout.day_button_3, mDayButtons, false /* attachToRoot */);
			final int firstDay = Utils.getZeroIndexedFirstDayOfWeek(mContext);
			dayButton.setText(Utils.getShortWeekday(i, firstDay));
			dayButton.setContentDescription(Utils.getLongWeekday(i, firstDay));
			mDayButtons.addView(dayButton);
			dayButtons[i] = dayButton;
		}
	}

	private void initTimePicker(Alarm alarm) {
		mTimePicker.setIs24HourView(DateFormat.is24HourFormat(mContext));
		mTimePicker.setCurrentHour(mAlarm.hour);
		mTimePicker.setCurrentMinute(mAlarm.minutes);
		mTimePicker.setOnTimeChangedListener(new OnTimeChangedListener() {

			@Override
			public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
				// TODO Auto-generated method stub
				Log.d(TAG, "hour=" + hourOfDay + " minute=" + minute);
				mAlarm.hour = hourOfDay;
				mAlarm.minutes = minute;
			}
		});
	}

	private void initDayButton(Alarm alarm) {
		if (alarm == null) {
			return;
		}
		updateDaysOfWeekButtons(alarm);
	}

	private void initSilentAfter(final Alarm alarm) {
		if (alarm == null) {
			mSilentAfter.setText(entry);
		} else {
			Log.d(TAG, "entry=" + getAlarmSilentAfterEntry(alarm));
			mSilentAfter.setText(getAlarmSilentAfterEntry(alarm));
		}
		mSilentRL.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				// dialog.show(getFragmentManager(), "SilentAfter");
				createSilentAfterWindow();
			}
		});
	}

	private void initLabel(final Alarm alarm) {
		if (alarm.label != null && alarm.label.length() != 0) {
			mLabel.setText(alarm.label + "  ");
			mLabel.setVisibility(View.VISIBLE);
			mLabel.setContentDescription(mContext.getResources().getString(
					R.string.label_description)
					+ " " + alarm.label);
		} else {
			mLabel.setVisibility(View.GONE);
		}
		mLabelRL.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mLabelDialog.show(getFragmentManager(), "label");
			}
		});
	}

	private void initRingtong(final Alarm alarm) {
		final String ringtone;
		if (Alarm.NO_RINGTONE_URI.equals(alarm.alert)) {
			ringtone = mContext.getResources().getString(
					R.string.silent_alarm_summary);
		} else {
			if (!isRingtoneExisted(mContext, alarm.alert.toString())) {
				alarm.alert = RingtoneManager.getActualDefaultRingtoneUri(
						mContext, RingtoneManager.TYPE_ALARM);
				// / M: The RingtoneManager may return null alert. @{
				if (alarm.alert == null) {
					alarm.alert = Uri.parse(SYSTEM_SETTINGS_ALARM_ALERT);
				}
				// / @}
				LogUtils.v("ringtone not exist, use default ringtone");
			}
			ringtone = getRingToneTitle(alarm.alert);
		}
		mRingtong.setText(ringtone);
		mRingtong.setContentDescription(mContext.getResources().getString(
				R.string.ringtone_description)
				+ " " + ringtone);
		mRingtongRL.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				launchRingTonePicker(alarm);
			}
		});
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			switch (requestCode) {
			case REQUEST_CODE_RINGTONE:
				saveRingtoneUri(data);
				break;
			default:
				LogUtils.w("Unhandled request code in onActivityResult: "
						+ requestCode);
			}
		}
	}

	private void saveRingtoneUri(Intent intent) {
		Uri uri = intent
				.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
		if (uri == null) {
			uri = Alarm.NO_RINGTONE_URI;
		}
		// / M: if the alarm to change ringtone is null, then do nothing @{
		if (null == mAlarm) {
			LogUtils.w("saveRingtoneUri the alarm to change ringtone is null");
			return;
		}
		// / @}
		mAlarm.alert = uri;
		if (!AlarmUtils.hasPermissionToDisplayRingtoneTitle(mContext, uri)) {
			final String[] perms = { Manifest.permission.READ_EXTERNAL_STORAGE };
			requestPermissions(perms, REQUEST_CODE_PERMISSIONS);
		} else {
			// / M: Permissions already granted, save the ringtone
		}
	}

	private void updateDaysOfWeekButtons(final Alarm alarm) {
		HashSet<Integer> setDays = alarm.daysOfWeek.getSetDays();
		for (int i = 0; i < 7; i++) {
			if (setDays.contains(mDayOrder[i])) {
				turnOnDayOfWeek(i);
			} else {
				turnOffDayOfWeek(i);
			}
		}

		for (int i = 0; i < 7; i++) {
			final int buttonIndex = i;

			dayButtons[i].setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					final boolean isActivated = dayButtons[buttonIndex]
							.isActivated();
					alarm.daysOfWeek.setDaysOfWeek(!isActivated,
							mDayOrder[buttonIndex]);

					if (!isActivated) {
						turnOnDayOfWeek(buttonIndex);
					} else {
						turnOffDayOfWeek(buttonIndex);
					}
				}
			});
		}

	}

	private void turnOffDayOfWeek(int dayIndex) {
		final CompoundButton dayButton = dayButtons[dayIndex];
		dayButton.setActivated(false);
		dayButton.setChecked(false);
		dayButton.setTextColor(getResources().getColor(R.color.clock_white));
	}

	private void turnOnDayOfWeek(int dayIndex) {
		final CompoundButton dayButton = dayButtons[dayIndex];
		dayButton.setActivated(true);
		dayButton.setChecked(true);
		dayButton.setTextColor(Utils.getCurrentHourColor());
	}

	private void setDayOrder() {
		// Value from preferences corresponds to Calendar.<WEEKDAY> value
		// -1 in order to correspond to DAY_ORDER indexing
		final int startDay = Utils.getZeroIndexedFirstDayOfWeek(mContext);
		mDayOrder = new int[DaysOfWeek.DAYS_IN_A_WEEK];

		for (int i = 0; i < DaysOfWeek.DAYS_IN_A_WEEK; ++i) {
			mDayOrder[i] = DAY_ORDER[(startDay + i) % 7];
		}

	}

	public static boolean isRingtoneExisted(Context ctx, String ringtone) {
		boolean result = false;
		if (ringtone != null) {
			if (ringtone.contains("internal")) {
				return true;
			}
			String path = PowerOffAlarm.getRingtonePath(ctx, ringtone);
			if (!TextUtils.isEmpty(path)) {
				result = new File(path).exists();
			}
			LogUtils.v("isRingtoneExisted: " + result + " ,ringtone: "
					+ ringtone + " ,Path: " + path);
		}
		return result;
	}

	/**
	 * Does a read-through cache for ringtone titles.
	 *
	 * @param uri
	 *            The uri of the ringtone.
	 * @return The ringtone title. {@literal null} if no matching ringtone
	 *         found.
	 */
	private String getRingToneTitle(Uri uri) {
		// Try the cache first
		String title = null;
		if (title == null) {
			// If the user cannot read the ringtone file, insert our own name
			// rather than the
			// ugly one returned by Ringtone.getTitle().
			if (!AlarmUtils.hasPermissionToDisplayRingtoneTitle(mContext, uri)) {
				title = getString(R.string.custom_ringtone);
			} else {
				// This is slow because a media player is created during
				// Ringtone object creation.
				final Ringtone ringTone = RingtoneManager.getRingtone(mContext,
						uri);
				if (ringTone == null) {
					LogUtils.i("No ringtone for uri %s", uri.toString());
					return null;
				}
				title = ringTone.getTitle(mContext);
			}
		}
		return title;
	}

	private void launchRingTonePicker(Alarm alarm) {
		Uri oldRingtone = Alarm.NO_RINGTONE_URI.equals(alarm.alert) ? null
				: alarm.alert;
		final Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
				oldRingtone);
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,
				RingtoneManager.TYPE_ALARM);
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false);
		startActivityForResult(intent, REQUEST_CODE_RINGTONE);
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.alarm_cancel:
			finish();
			break;
		case R.id.alarm_ok:
			saveAndReture();
			break;
		case R.id.btn_cancel:
			dismissWindow();
			break;
		default:
			break;
		}
	}

	private void saveAndReture() {
		Intent intent = new Intent();
		intent.putExtra("alarm", mAlarm);
		intent.putExtra("silent_after", value);
		setResult(Activity.RESULT_OK, intent);
		finish();
	}

	private String getAlarmSilentAfterEntry(Alarm alarm) {
		if (alarm == null) {
			return "";
		}
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(mContext);
		String value = prefs.getString(AlarmClockFragment.KEY_AUTO_SILENCE
				+ alarm.id, "10");
		String[] values = mContext.getResources().getStringArray(
				R.array.auto_silence_values);
		String[] entries = mContext.getResources().getStringArray(
				R.array.auto_silence_entries);
		int index = 0;
		for (int i = 0; i < values.length; i++) {
			if (values[i].equals(value)) {
				index = i;
				break;
			}
		}
		return entries[index];
	}

	private void asyncDeleteAlarm(final Alarm alarm) {
		mContext.getApplicationContext();
		final AsyncTask<Void, Void, Void> deleteTask = new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... parameters) {
				// Activity may be closed at this point , make sure data is
				// still valid
				if (mContext != null && alarm != null) {
					Events.sendAlarmEvent(R.string.action_delete,
							R.string.label_deskclock);

					ContentResolver cr = mContext.getContentResolver();
					AlarmStateManager.deleteAllInstances(mContext, alarm.id);
					Alarm.deleteAlarm(cr, alarm.id);
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				// TODO Auto-generated method stub
				super.onPostExecute(result);
				Toast toast = Toast.makeText(mContext, R.string.alarm_deleted,
						Toast.LENGTH_LONG);
				ToastMaster.setToast(toast);
				toast.show();
				finish();

			}
		};

		deleteTask.execute();
	}

	private void refleshSilentAfter(String entry) {
		mSilentAfter.setText(entry);
	}

	private void refleshLabel(String label) {
		mLabel.setText(label);
	}

	@Override
	public void onLabelChange(String label) {
		// TODO Auto-generated method stub
		mAlarm.label = label;
		refleshLabel(label);
	}
	
	private void initWindowView(){
		mWindowView = LayoutInflater.from(mContext).inflate(
				R.layout.silent_after, null);
		mSilentList = (ListView) mWindowView.findViewById(R.id.silent_after_list);
		mCancelBtn = (TextView) mWindowView.findViewById(R.id.btn_cancel);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(mContext,
				R.layout.silent_after_item, R.id.silent_time, entries);
		mSilentList.setAdapter(adapter);
		mCancelBtn.setOnClickListener(this);
		mSilentList.setOnItemClickListener(this);
	}

	private void createSilentAfterWindow() {
		
		window = new PopupWindow(mWindowView, LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT);
		window.setTouchable(true);
		window.setOutsideTouchable(false);
		//window.setFocusable(true);
		ColorDrawable dw = new ColorDrawable(0x00000000);
	        //设置SelectPicPopupWindow弹出窗体的背景
	    window.setBackgroundDrawable(dw);
	    backgroundAlpha(0.5f);//0.0-1.0
		window.showAtLocation(content, Gravity.BOTTOM, 0, 0);
		window.setOnDismissListener(new OnDismissListener() {
			
			@Override
			public void onDismiss() {
				// TODO Auto-generated method stub
				 backgroundAlpha(1f);
			}
		});
	}
	
	private void dismissWindow(){
		if (window==null) {
			return;
		}
		if (window.isShowing()) {
			window.dismiss();
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		// TODO Auto-generated method stub
		mChosedSilentPosition =mSilentList.getCheckedItemPosition();
		if (mChosedSilentPosition!=-1) {
			this.value = values[mChosedSilentPosition];
			this.entry = entries[mChosedSilentPosition];
			refleshSilentAfter(entry);
			window.dismiss();
		}
	}
	
	public void backgroundAlpha(float bgAlpha)
    {
        WindowManager.LayoutParams lp = this.getWindow().getAttributes();
        lp.alpha = bgAlpha;
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        this.getWindow().setAttributes(lp);
    }

	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		dismissWindow();
	}

}
