package com.android.alarmclock;

import com.android.deskclock.worldclock.Cities;
import com.android.internal.content.NativeLibraryHelper.Handle;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

public class NewAppWidgetService extends Service {

	private String TAG="NewAppWidgetService";
	private String ACTION_TIME_SET = "android.intent.action.TIME_SET";
	private int ACTION_APP_SERVICE_UPDATE=100;
	private HandlerThread handlerThread;
	private Context mContext;
	private Handler handler;
	
	private void initHandler(){
		handlerThread=new HandlerThread("receiver");
		handlerThread.start();
		handler=new Handler(handlerThread.getLooper()){

			@Override
			public void handleMessage(Message msg) {
				// TODO Auto-generated method stub
				super.handleMessage(msg);
				if (msg.what==ACTION_APP_SERVICE_UPDATE) {
					Log.i(TAG, "handler-->sendBroadcast");
					mContext.sendBroadcast(new Intent(NewDigitalAppWidgetProvider.ACTION_APP_SERVICE_UPDATE));
				}
			}
			
		};
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		initHandler();
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
			mContext=context;
			handler.sendEmptyMessage(ACTION_APP_SERVICE_UPDATE);
			//context.sendBroadcast(new Intent(NewDigitalAppWidgetProvider.ACTION_APP_SERVICE_UPDATE));
		}
	};
	
	
}
