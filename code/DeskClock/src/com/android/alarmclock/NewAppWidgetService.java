package com.android.alarmclock;

import com.android.deskclock.worldclock.Cities;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

public class NewAppWidgetService extends Service {

	private String ACTION_TIME_SET = "android.intent.action.TIME_SET";
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_TIME_TICK);
		filter.addAction(Intent.ACTION_TIME_CHANGED);
		filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
		filter.addAction(Intent.ACTION_LOCALE_CHANGED);
		filter.addAction(Cities.WORLDCLOCK_UPDATE_INTENT);
		filter.addAction(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_DATE_CHANGED);
		filter.addAction(ACTION_TIME_SET);
		registerReceiver(mAppWidgetReceiver, filter);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(mAppWidgetReceiver);
		super.onDestroy();
	}

	private BroadcastReceiver mAppWidgetReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			context.sendBroadcast(new Intent(NewDigitalAppWidgetProvider.ACTION_APP_SERVICE_UPDATE));
		}
	};
}
