package com.android.deskclock;

import java.util.Calendar;
import java.util.Date;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Paint.Style;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

public class ClockView extends View{
	
	private String TAG=getClass().getSimpleName();
	private Context context;
	private int defViewWidth=dipToPx(192);
	private int defViewHeight=dipToPx(192);
	private int xCenter=defViewWidth/2;
	private int yCenter=defViewHeight/2;
	private int bigCircleRadius=dipToPx(94);
	private int smallCircleRaius=dipToPx(83);
	private int bigCircleAlpha=alphaPercentToInt(20);
	private int smallCircleAlpha=alphaPercentToInt(10);
	private int bigCircleWidth=dipToPx(4);
	private int smallCircleWidth=dipToPx(1);
	private int defCircleColor=Color.WHITE;
	private int secondDotColor=0xffffb310;
	private int secondRadius=dipToPx(4);
	private int xSecondLoction=xCenter;
	private int ySecondLoction=yCenter-smallCircleRaius;
	private boolean isOnAttacted;
	private Calendar calendar=Calendar.getInstance();
	private Date date;
 	private Paint paint=new Paint();
 	private Handler handle=new Handler();
 	private String minuteText="00";
 	private String hourText="00";
 	
 	 private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
         @Override
         public void onReceive(Context context, Intent intent) {
             if (intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                 
             }
             onTimeChanged();
             invalidate();
         }
     };
 	
 	
 	Runnable secondListen=new Runnable() {
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			onTimeChanged();
			ClockView.this.postDelayed(secondListen,100);
		}
	};
	public ClockView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		init(context);
		onTimeChanged();
		
	}
	
	private void init(Context context){
	}
	
	private void drawBigCircle(Canvas canvas){
		Paint paint=new Paint();
	    paint.setAntiAlias(true);
	    paint.setColor(defCircleColor);
	    paint.setAlpha(bigCircleAlpha);
	    paint.setStyle(Style.STROKE);
	    paint.setStrokeWidth(bigCircleWidth);
		canvas.drawCircle(xCenter, yCenter, bigCircleRadius, paint);
		
	}
	
	private void drawSmallCircle(Canvas canvas){
		Paint paint=new Paint();
		paint.setAntiAlias(true);
		paint.setColor(defCircleColor);
		paint.setAlpha(smallCircleAlpha);
		paint.setStyle(Style.STROKE);
		paint.setStrokeWidth(smallCircleWidth);
		canvas.drawCircle(xCenter, yCenter, smallCircleRaius, paint);
	}
	
	private void drawSecondDot(Canvas canvas){
		Paint paint=new Paint();
		paint.setAntiAlias(true);
		paint.setColor(secondDotColor);
		paint.setStrokeWidth(secondRadius);
		canvas.drawCircle(xSecondLoction, ySecondLoction, secondRadius, paint);
	}
	
	private void drawHourText(Canvas canvas){
		Typeface type=Typeface.createFromAsset(getContext().getAssets(), "fonts/Akrobat-Regular.otf");
		Paint hourPaint=new Paint();
		hourPaint.setTypeface(type);
		hourPaint.setTextSize(dipToPx(53));
		FontMetrics fontMetrics=hourPaint.getFontMetrics();
		float textHeight=fontMetrics.bottom-fontMetrics.top;
		float textWidth=hourPaint.measureText(hourText);
		float xText=xCenter-textWidth/2;
		float yText=yCenter-8;
		hourPaint.setAntiAlias(true);
		hourPaint.setColor(secondDotColor);
		canvas.drawText(hourText, xText, yText, hourPaint);
	}
	
	private void drawMinuteText(Canvas canvas){
		Paint minutePaint=new Paint();
		Typeface type=Typeface.createFromAsset(getContext().getAssets(), "fonts/Akrobat-Regular.otf");
		minutePaint.setTypeface(type);
		minutePaint.setTextSize(dipToPx(53));
		FontMetrics fontMetrics=minutePaint.getFontMetrics();
		float textHeight=fontMetrics.bottom-fontMetrics.top;
		float textWidth=minutePaint.measureText(minuteText);
		float xText=xCenter-textWidth/2;
		float yText=yCenter+textHeight/2+8;
		minutePaint.setAntiAlias(true);
		minutePaint.setColor(Color.WHITE);
		canvas.drawText(minuteText, xText, yText, minutePaint);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		super.onDraw(canvas);
		drawBigCircle(canvas);
		drawSmallCircle(canvas);
		drawSecondDot(canvas);
		drawHourText(canvas);
		drawMinuteText(canvas);
		
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// TODO Auto-generated method stub
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		int mode=MeasureSpec.getMode(widthMeasureSpec);
		int width=MeasureSpec.getSize(widthMeasureSpec);
		int height=MeasureSpec.getSize(heightMeasureSpec);
		if (mode==MeasureSpec.EXACTLY) {
			setMeasuredDimension(width, height);
		}else {
			setMeasuredDimension(defViewWidth, defViewHeight);
		}
		xCenter=getWidth()/2;
		yCenter=getHeight()/2;
	}
	
	@Override
	protected void onAttachedToWindow() {
		// TODO Auto-generated method stub
		super.onAttachedToWindow();
		if (!isOnAttacted) {
			isOnAttacted=true;
			IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);

            getContext().registerReceiver(mIntentReceiver, filter, null, handle);
		}
		post(secondListen);
	}
	
	@Override
	protected void onDetachedFromWindow() {
		// TODO Auto-generated method stub
		super.onDetachedFromWindow();
		if (isOnAttacted) {
			getContext().unregisterReceiver(mIntentReceiver);
			removeCallbacks(secondListen);
			isOnAttacted = false;
		}
	}
	
	  /**
     * dip 转换成px
     * @param dip
     * @return
     */
    private int dipToPx(float dip) {
        float density = getContext().getResources().getDisplayMetrics().density;
        return (int)(dip * density + 0.5f * (dip >= 0 ? 1 : -1));
    }
    
    private int alphaPercentToInt(int percent){
    	int value=(int) (percent/(100*1.0f)*255);
    	return  value;
    }
    
    private void refleshSecond(){
    	long OneMinute=60*1000;
    	double pi=Math.PI;
    	long currentSecond=calendar.get(Calendar.SECOND);
    	long currentMilliSecond=currentSecond*1000+calendar.get(Calendar.MILLISECOND);
    	double percent=currentMilliSecond/(OneMinute*1.0d);
    	double angle=-pi/2+percent*2*pi;
    	if (angle>=3*pi/2) {
			angle=pi/2;
		}
    	xSecondLoction=(int) (xCenter+smallCircleRaius*Math.cos(angle));
    	ySecondLoction=(int) (yCenter+smallCircleRaius*Math.sin(angle));
    	invalidate();
    	
    }
    
    private void refleshMinuteAndHour(){
    	long minute=calendar.get(Calendar.MINUTE);
    	long hour=calendar.get(Calendar.HOUR);
    	if (minute<10) {
			minuteText="0"+minute;
		}else {
			minuteText=minute+"";
		}
    	if (hour<10) {
			hourText="0"+hour;
		}else {
			hourText=hour+"";
		}
    	invalidate();
    }
    
    private void onTimeChanged() {
       date=new Date();
       calendar.setTime(date);
       refleshSecond();
       refleshMinuteAndHour();
    }
    

}
