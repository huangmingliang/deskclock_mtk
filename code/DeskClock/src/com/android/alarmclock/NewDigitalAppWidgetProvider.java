/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.alarmclock;

import java.util.Locale;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.TextClock;

import com.android.deskclock.DeskClock;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.alarms.AlarmStateManager;
import com.android.deskclock.worldclock.Cities;
import com.android.deskclock.worldclock.CitiesActivity;

public class NewDigitalAppWidgetProvider extends AppWidgetProvider {
	private static final String TAG = "NewDigitalAppWidgetProvider";

	/**
	 * Intent to be used for checking if a world clock's date has changed. Must
	 * be every fifteen minutes because not all time zones are hour-locked.
	 **/
	public static final String ACTION_ON_QUARTER_HOUR = "com.android.deskclock.ON_QUARTER_HOUR";

	public static final String ACTION_APPWIDGET_ENABLED = "android.appwidget.action.APPWIDGET_ENABLED";
	
	public static final String ACTION_APP_SERVICE_UPDATE = "com.android.deskclock.SERVICE_UPDATE_WIDGET";
	
	/// Use the action because the time of widget used the bitmap to instance of the TextClock, need it to update the bitmap
	private String ACTION_APP_WIDGET_UPDATE = "android.appwidget.action.APPWIDGET_UPDATE";
	
	private String ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";

	// Lazily creating this intent to use with the AlarmManager
	private PendingIntent mPendingIntent;
	// Lazily creating this name to use with the AppWidgetManager
	private ComponentName mComponentName;
	
	private TextClock mTextClock;

	public NewDigitalAppWidgetProvider() {
	}

	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);
		startAlarmOnQuarterHour(context);
	}

	@Override
	public void onDisabled(Context context) {
		super.onDisabled(context);
		cancelAlarmOnQuarterHour(context);
		context.stopService(new Intent(context, NewAppWidgetService.class));
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		// if (DigitalAppWidgetService.LOGGING) {
		Log.i(TAG, "onReceive: " + action);
		// }
		// super.onReceive(context, intent);
		if (ACTION_ON_QUARTER_HOUR.equals(action)
				|| Intent.ACTION_DATE_CHANGED.equals(action)
//				|| Intent.ACTION_TIMEZONE_CHANGED.equals(action)
//				|| Intent.ACTION_TIME_CHANGED.equals(action)
				|| Intent.ACTION_LOCALE_CHANGED.equals(action)
				|| ACTION_APP_WIDGET_UPDATE.equals(action)
				|| ACTION_APPWIDGET_ENABLED.equals(action)
				|| ACTION_APP_SERVICE_UPDATE.equals(action)) {
			AppWidgetManager appWidgetManager = AppWidgetManager
					.getInstance(context);
			if (appWidgetManager != null) {
				int[] appWidgetIds = appWidgetManager
						.getAppWidgetIds(getComponentName(context));
				for (int appWidgetId : appWidgetIds) {
					appWidgetManager.notifyAppWidgetViewDataChanged(
							appWidgetId, R.id.digital_appwidget_listview);
					RemoteViews widget = new RemoteViews(
							context.getPackageName(),
							R.layout.new_digital_appwidget);
					refreshAlarm(context, widget);
					appWidgetManager.partiallyUpdateAppWidget(appWidgetId,
							widget);
				}
			}
			if (!ACTION_ON_QUARTER_HOUR.equals(action)) {
				cancelAlarmOnQuarterHour(context);
			}
			startAlarmOnQuarterHour(context);
		} else if (isNextAlarmChangedAction(action)
				|| Intent.ACTION_SCREEN_ON.equals(action)) {
			// Refresh the next alarm
			Log.i(TAG, "refreshAlarm: ");
			AppWidgetManager appWidgetManager = AppWidgetManager
					.getInstance(context);
			if (appWidgetManager != null) {
				int[] appWidgetIds = appWidgetManager
						.getAppWidgetIds(getComponentName(context));
				for (int appWidgetId : appWidgetIds) {
					RemoteViews widget = new RemoteViews(
							context.getPackageName(),
							R.layout.new_digital_appwidget);
					refreshAlarm(context, widget);
					appWidgetManager.partiallyUpdateAppWidget(appWidgetId,
							widget);
				}
			}
		} else if (Cities.WORLDCLOCK_UPDATE_INTENT.equals(action)) {
			// Refresh the world cities list
			AppWidgetManager appWidgetManager = AppWidgetManager
					.getInstance(context);
			if (appWidgetManager != null) {
				int[] appWidgetIds = appWidgetManager
						.getAppWidgetIds(getComponentName(context));
				for (int appWidgetId : appWidgetIds) {
					RemoteViews widget = new RemoteViews(
							context.getPackageName(),
							R.layout.new_digital_appwidget);
					refreshAlarm(context, widget);
					appWidgetManager.notifyAppWidgetViewDataChanged(
							appWidgetId, R.id.digital_appwidget_listview);
				}
			}
		}else if(ACTION_BOOT_COMPLETED.equals(action)){
			context.startService(new Intent(context, NewAppWidgetService.class));
		}
		super.onReceive(context, intent);
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		context.startService(new Intent(context, NewAppWidgetService.class));
		// if (DigitalAppWidgetService.LOGGING) {
		Log.i(TAG, "onUpdate");
		// }
		for (int appWidgetId : appWidgetIds) {
			float ratio = WidgetUtils.getScaleRatio(context, null, appWidgetId);
			updateClock(context, appWidgetManager, appWidgetId, ratio);
		}
		startAlarmOnQuarterHour(context);
		// modify to use service to receive the time tick broadcast
		//register(context);
		super.onUpdate(context, appWidgetManager, appWidgetIds);
	}

	@Override
	public void onAppWidgetOptionsChanged(Context context,
			AppWidgetManager appWidgetManager, int appWidgetId,
			Bundle newOptions) {
		// scale the fonts of the clock to fit inside the new size
		float ratio = WidgetUtils.getScaleRatio(context, newOptions,
				appWidgetId);
		AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
		updateClock(context, widgetManager, appWidgetId, ratio);
	}

	/**
	 * Determine whether action received corresponds to a "next alarm" changed
	 * action depending on the SDK version.
	 */
	private boolean isNextAlarmChangedAction(String action) {
		final String nextAlarmIntentAction;
		if (Utils.isLOrLater()) {
			nextAlarmIntentAction = AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED;
		} else {
			nextAlarmIntentAction = AlarmStateManager.SYSTEM_ALARM_CHANGE_ACTION;
		}
		return nextAlarmIntentAction.equals(action);
	}

	private void updateClock(Context context,
			AppWidgetManager appWidgetManager, int appWidgetId, float ratio) {
		RemoteViews widget = new RemoteViews(context.getPackageName(),
				R.layout.new_digital_appwidget);
		Log.i(TAG, "updateClock: ");
		// Launch clock when clicking on the time in the widget only if not a
		// lock screen widget
		Bundle newOptions = appWidgetManager.getAppWidgetOptions(appWidgetId);
		if (newOptions != null
				&& newOptions.getInt(
						AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, -1) != AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD) {
			widget.setOnClickPendingIntent(R.id.digital_appwidget,
					PendingIntent.getActivity(context, 0, new Intent(context,
							DeskClock.class), 0));
		}

		// Setup alarm text clock's format and font sizes
		refreshAlarm(context, widget);

		// Set today's date format
		String ee = DateFormat.getBestDateTimePattern(Locale.getDefault(),
				"eeee");
		String mm = DateFormat.getBestDateTimePattern(Locale.getDefault(),
				"MMMMd");
		// CharSequence dateFormat = DateFormat.getBestDateTimePattern(
		// Locale.getDefault(),
		// context.getString(R.string.abbrev_wday_month_day_no_year));
		widget.setCharSequence(R.id.week, "setFormat12Hour", ee);
		widget.setCharSequence(R.id.week, "setFormat24Hour", ee);
		widget.setCharSequence(R.id.date, "setFormat12Hour", mm);
		widget.setCharSequence(R.id.date, "setFormat24Hour", mm);

		// Set up R.id.digital_appwidget_listview to use a remote views adapter
		// That remote views adapter connects to a RemoteViewsService through
		// intent.
		final Intent intent = new Intent(context, DigitalAppWidgetService.class);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
		widget.setRemoteAdapter(R.id.digital_appwidget_listview, intent);

		// Set up the click on any world clock to start the Cities Activity
		// TODO: Should this be in the options guard above?
		widget.setPendingIntentTemplate(R.id.digital_appwidget_listview,
				PendingIntent.getActivity(context, 0, new Intent(context,
						CitiesActivity.class), 0));

		// Refresh the widget
		appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId,
				R.id.digital_appwidget_listview);
		appWidgetManager.updateAppWidget(appWidgetId, widget);
	}

	protected void refreshAlarm(Context context, RemoteViews widget) {
		// final String nextAlarm = Utils.getNextAlarm(context);
		// if (!TextUtils.isEmpty(nextAlarm)) {
		// widget.setTextViewText(R.id.nextAlarm, context.getString(
		// R.string.control_set_alarm_with_existing, nextAlarm));
		// widget.setViewVisibility(R.id.nextAlarm, View.VISIBLE);
		// if (DigitalAppWidgetService.LOGGING) {
		// Log.v(TAG, "DigitalWidget sets next alarm string to "
		// + nextAlarm);
		// }
		// } else {
		// widget.setViewVisibility(R.id.nextAlarm, View.GONE);
		// if (DigitalAppWidgetService.LOGGING) {
		// Log.v(TAG, "DigitalWidget sets next alarm string to null");
		// }
		// }
		if(null == mTextClock){
			mTextClock = new TextClock(context);
		}
		if (null != widget) {
//			TextClock textClock = new TextClock(context);
//			textClock.setFormat12Hour(Utils
//					.get12ModeFormat(dip2px(context, 40f)));
//			textClock.setFormat24Hour(Utils.get24ModeFormat());
//			widget.setImageViewBitmap(R.id.the_clock,
//					buildUpdate(textClock.getText().toString() + "", context));
//			textClock = null;
			
			/// modify am/pm font size 20160906 @{
//			mTextClock.setFormat12Hour(Utils
//					.get12ModeFormat(dip2px(context, 40f)));
			mTextClock.setFormat12Hour("aa");
			String ampm = mTextClock.getText().toString() + "";
			mTextClock.setFormat12Hour("h:mm");
			String time = mTextClock.getText().toString() + "";
			mTextClock.setFormat24Hour(Utils.get24ModeFormat());
			widget.setImageViewBitmap(R.id.the_clock,
					buildUpdate(time, ampm, context));
			/// modify am/pm font size 20160906 @}
		}
	}

	/**
	 * Start an alarm that fires on the next quarter hour to update the world
	 * clock city day when the local time or the world city crosses midnight.
	 * 
	 * @param context
	 *            The context in which the PendingIntent should perform the
	 *            broadcast.
	 */
	private void startAlarmOnQuarterHour(Context context) {
		if (context != null) {
			long onQuarterHour = Utils.getAlarmOnQuarterHour();
			PendingIntent quarterlyIntent = getOnQuarterHourPendingIntent(context);
			AlarmManager alarmManager = ((AlarmManager) context
					.getSystemService(Context.ALARM_SERVICE));
			if (Utils.isKitKatOrLater()) {
				alarmManager.setExact(AlarmManager.RTC, onQuarterHour,
						quarterlyIntent);
			} else {
				alarmManager.set(AlarmManager.RTC, onQuarterHour,
						quarterlyIntent);
			}
			if (DigitalAppWidgetService.LOGGING) {
				Log.v(TAG, "startAlarmOnQuarterHour " + context.toString());
			}
		}
	}

	/**
	 * Remove the alarm for the quarter hour update.
	 * 
	 * @param context
	 *            The context in which the PendingIntent was started to perform
	 *            the broadcast.
	 */
	public void cancelAlarmOnQuarterHour(Context context) {
		if (context != null) {
			PendingIntent quarterlyIntent = getOnQuarterHourPendingIntent(context);
			if (DigitalAppWidgetService.LOGGING) {
				Log.v(TAG, "cancelAlarmOnQuarterHour " + context.toString());
			}
			((AlarmManager) context.getSystemService(Context.ALARM_SERVICE))
					.cancel(quarterlyIntent);
		}
	}

	/**
	 * Create the pending intent that is broadcast on the quarter hour.
	 * 
	 * @param context
	 *            The Context in which this PendingIntent should perform the
	 *            broadcast.
	 * @return a pending intent with an intent unique to
	 *         DigitalAppWidgetProvider
	 */
	private PendingIntent getOnQuarterHourPendingIntent(Context context) {
		if (mPendingIntent == null) {
			mPendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(
					ACTION_ON_QUARTER_HOUR), PendingIntent.FLAG_CANCEL_CURRENT);
		}
		return mPendingIntent;
	}

	/**
	 * Create the component name for this class
	 * 
	 * @param context
	 *            The Context in which the widgets for this component are
	 *            created
	 * @return the ComponentName unique to DigitalAppWidgetProvider
	 */
	private ComponentName getComponentName(Context context) {
		if (mComponentName == null) {
			mComponentName = new ComponentName(context, getClass());
		}
		return mComponentName;
	}

	private static int dip2px(Context context, float dipValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dipValue * scale + 0.5f);
	}

	private static Bitmap buildUpdate(String time, String ampm, Context context) {
		int width = dip2px(context, 256f);
		int height = dip2px(context, 52f);
		Bitmap myBitmap = Bitmap.createBitmap(width, height,
				Bitmap.Config.ARGB_8888);
		Canvas myCanvas = new Canvas(myBitmap);
		Paint paint = new Paint();
		Typeface tf = Typeface.createFromAsset(context.getAssets(),
				"fonts/ItelNumber.ttf");
		paint.setAntiAlias(true);
		paint.setAlpha(255);
		paint.setSubpixelText(true);
		paint.setTypeface(tf);
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(Color.WHITE);
		paint.setShadowLayer(4, 1, 1, 0x05000000);
		
		/// modify am/pm font size 20160906 @{
		/// am/pm is on right
		if(drawAmPmOnRight()){
			paint.setTextSize(dip2px(context, 40f));
			myCanvas.drawText(time, dip2px(context, 10), dip2px(context, 40f),
					paint);
			float x = paint.measureText(time);
			paint.setTextSize(20f);
			myCanvas.drawText(ampm, x+25, dip2px(context, 40f),
					paint);
		}
		/// am/pm is on left
		else
		{
			float x = paint.measureText(time);
			paint.setTextSize(20f);
			myCanvas.drawText(ampm, dip2px(context, 10), dip2px(context, 40f),
					paint);
			paint.setTextSize(dip2px(context, 40f));
			myCanvas.drawText(time, x+25, dip2px(context, 40f),
					paint);
		}
		/// modify am/pm font size 20160906 @}
		return myBitmap;
	}

	private void register(Context context) {
		IntentFilter filter = new IntentFilter(Intent.ACTION_TIME_TICK);
		context.getApplicationContext().registerReceiver(mTimeChangeReceiver,
				filter);
	}

	private BroadcastReceiver mTimeChangeReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (Intent.ACTION_TIME_TICK.equals(action)) {
				AppWidgetManager appWidgetManager = AppWidgetManager
						.getInstance(context);
				if (appWidgetManager != null) {
					int[] appWidgetIds = appWidgetManager
							.getAppWidgetIds(getComponentName(context));
					for (int appWidgetId : appWidgetIds) {
						appWidgetManager.notifyAppWidgetViewDataChanged(
								appWidgetId, R.id.digital_appwidget_listview);
						RemoteViews widget = new RemoteViews(
								context.getPackageName(),
								R.layout.new_digital_appwidget);
						refreshAlarm(context, widget);
						appWidgetManager.partiallyUpdateAppWidget(appWidgetId,
								widget);
					}
				}
			}
		}
	};
	
	private static boolean drawAmPmOnRight(){
		String county = Resources.getSystem().getConfiguration().locale.getCountry();
		switch (county) {
		case "CN":
		case "TW":
		return false;	
		default:
	    return true;
		}
	}
}
