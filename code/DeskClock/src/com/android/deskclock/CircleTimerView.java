package com.android.deskclock;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.telecom.Log;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;

import com.android.deskclock.R.id;
import com.android.deskclock.stopwatch.Stopwatches;

/**
 * Class to draw a circle for timers and stopwatches.
 * These two usages require two different animation modes:
 * Timer counts down. In this mode the animation is counter-clockwise and stops at 0.
 * Stopwatch counts up. In this mode the animation is clockwise and will run until stopped.
 */
public class CircleTimerView extends View{
	
    private String TAG=getClass().getSimpleName();
    private int mAccentColor;
    private int mWhiteColor;
    private long mIntervalTime = 0;
    private long mIntervalStartTime = -1;
    private long mMarkerTime = -1;
    private long mCurrentIntervalTime = 0;
    private long mAccumulatedTime = 0;
    private boolean mPaused = false;
    private boolean mAnimate = false;
    private float mStrokeSize =dipToPx(4.5f);
    private float mDotRadius = 6;
    private float mMarkerStrokeSize = 2;
    private final Paint mPaint = new Paint();
    private final Paint mFill = new Paint();
    private final RectF mArcRect = new RectF();
    private float mRadiusOffset;   // amount to remove from radius to account for markers on circle
    private float mScreenDensity;
    private int defViewWidth=dipToPx(208);
	private int defViewHeight=dipToPx(208);
	private int xCenter=defViewWidth/2;
	private int yCenter=defViewHeight/2;
	private float radius=dipToPx(94);
	private int bigCircleAlpha=alphaPercentToInt(20);
	private Drawable cursorImage,cursorImage2;
	private float cursorImageAngle=0;
	private long ONE_MINUTE_IN_MILLISECOND=60*1000;
	private boolean isLessOneMinute=true;
	private int timeLeftColor=0xff27B25F;
	private int cursorWidth;
	private int cursorHeight;
	private float xDown,yDown;
	private float cursorLeft,cursorTop,cursorRight,cursorBottom;
	private boolean isMovingCursor;
	private DeskClock deskClock;
	private int lastArea;
	private int newArea;
	private int minutes;
	private CursorMoveListener cursorMoveListener;
	

    // Stopwatch mode is the default.
    private boolean mTimerMode = false;

    @SuppressWarnings("unused")
    public CircleTimerView(Context context) {
        this(context, null);
    }

    public CircleTimerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray array=context.obtainStyledAttributes(attrs,R.styleable.CircleView);
        mTimerMode=array.getBoolean(R.styleable.CircleView_timer_modle,false);
        array.recycle();
        init(context);
    }

    public void setIntervalTime(long t) {
        mIntervalTime = t;
        postInvalidate();
    }

    public void setMarkerTime(long t) {
        mMarkerTime = t;
        postInvalidate();
    }

    public void reset() {
        mIntervalStartTime = -1;
        mMarkerTime = -1;
        postInvalidate();
    }
    public void startIntervalAnimation() {
        mIntervalStartTime = Utils.getTimeNow();
        mAnimate = true;
        invalidate();
        mPaused = false;
    }
    public void stopIntervalAnimation() {
        mAnimate = false;
        mIntervalStartTime = -1;
        mAccumulatedTime = 0;
    }

    public boolean isAnimating() {
        return (mIntervalStartTime != -1);
    }

    public void pauseIntervalAnimation() {
        mAnimate = false;
        mAccumulatedTime += Utils.getTimeNow() - mIntervalStartTime;
        mPaused = true;
    }

    public void abortIntervalAnimation() {
        mAnimate = false;
    }

    public void setPassedTime(long time, boolean drawRed) {
        // The onDraw() method checks if mIntervalStartTime has been set before drawing any red.
        // Without drawRed, mIntervalStartTime should not be set here at all, and would remain at -1
        // when the state is reconfigured after exiting and re-entering the application.
        // If the timer is currently running, this drawRed will not be set, and will have no effect
        // because mIntervalStartTime will be set when the thread next runs.
        // When the timer is not running, mIntervalStartTime will not be set upon loading the state,
        // and no red will be drawn, so drawRed is used to force onDraw() to draw the red portion,
        // despite the timer not running.
        mCurrentIntervalTime = mAccumulatedTime = time;
        if (drawRed) {
            mIntervalStartTime = Utils.getTimeNow();
        }
        postInvalidate();
    }
    
    
    private void caculateCursorAngle(){
    	float angle=0;
    	float percent=0;
    	long timeLeft=mIntervalTime-mCurrentIntervalTime;
    	if (timeLeft>ONE_MINUTE_IN_MILLISECOND) {
			isLessOneMinute=false;
			percent=(timeLeft%ONE_MINUTE_IN_MILLISECOND)/(ONE_MINUTE_IN_MILLISECOND*1.0f);
		    angle=percent*360;
		}else if (timeLeft<ONE_MINUTE_IN_MILLISECOND&&timeLeft>0) {
			isLessOneMinute=true;
			percent=timeLeft/(ONE_MINUTE_IN_MILLISECOND*1.0f);
			angle=percent*360;
		}else if(timeLeft<=0){
			isLessOneMinute=true;
			stopIntervalAnimation();
			angle=0;
		}
    	cursorImageAngle=angle;

    }



    private void init(Context c) {

        Resources resources = c.getResources();
        mStrokeSize = resources.getDimension(R.dimen.circletimer_circle_size);
        float dotDiameter = resources.getDimension(R.dimen.circletimer_dot_size);
        mMarkerStrokeSize = resources.getDimension(R.dimen.circletimer_marker_size);
        mRadiusOffset = Utils.calculateRadiusOffset(
                mStrokeSize, dotDiameter, mMarkerStrokeSize);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mWhiteColor = resources.getColor(R.color.clock_white);
        mAccentColor = Utils.obtainStyledColor(c, R.attr.colorAccent, Color.RED);
        mScreenDensity = resources.getDisplayMetrics().density;
        mFill.setAntiAlias(true);
        mFill.setStyle(Paint.Style.FILL);
        mFill.setColor(mAccentColor);
        mDotRadius = dotDiameter / 2f;
        
        cursorImage=resources.getDrawable(R.drawable.timer_cursor);
        cursorImage2=resources.getDrawable(R.drawable.timer_cursor2);
    }

    public void setTimerMode(boolean mode) {
        mTimerMode = mode;
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
    public void onDraw(Canvas canvas) {
        mPaint.setStrokeWidth(mStrokeSize);
        Log.d(TAG, "mIntervalStartTime="+mIntervalStartTime);
        if (mIntervalStartTime == -1) {
            // just draw a complete white circle, no red arc needed
            mPaint.setColor(mWhiteColor);
            mPaint.setAlpha(bigCircleAlpha);
            canvas.drawCircle (xCenter, yCenter, radius, mPaint);
            if (mTimerMode) {
                //drawRedDot(canvas, 0f, xCenter, yCenter, radius);
            	 mArcRect.top = yCenter - radius;
                 mArcRect.bottom = yCenter + radius;
                 mArcRect.left =  xCenter - radius;
                 mArcRect.right = xCenter + radius;
                if (!isMovingCursor) {
                	caculateCursorAngle();
				}else {
					if (minutes>0) {
						isLessOneMinute=false;
					}else {
						isLessOneMinute=true;
					}
				}
            	if (isLessOneMinute) {
            		//canvas.drawArc (mArcRect, 270, -(360-cursorImageAngle) , false, mPaint);
            		mPaint.setColor(timeLeftColor);
            		canvas.drawArc(mArcRect, 270, cursorImageAngle, false, mPaint);
				}else {
					mPaint.setColor(timeLeftColor);
					canvas.drawCircle (xCenter, yCenter, radius, mPaint);
				}
                drawCursorImage(canvas, cursorImage, xCenter, yCenter);
            }
        } else {
            if (mAnimate) {
                mCurrentIntervalTime = Utils.getTimeNow() - mIntervalStartTime + mAccumulatedTime;
            }
            //draw a combination of red and white arcs to create a circle
            mArcRect.top = yCenter - radius;
            mArcRect.bottom = yCenter + radius;
            mArcRect.left =  xCenter - radius;
            mArcRect.right = xCenter + radius;
            float redPercent = (float)mCurrentIntervalTime / (float)mIntervalTime;
            // prevent timer from doing more than one full circle
            redPercent = (redPercent > 1 && mTimerMode) ? 1 : redPercent;

            float whitePercent = 1 - (redPercent > 1 ? 1 : redPercent);
            // draw red arc here
            mPaint.setColor(mAccentColor);
            if (mTimerMode){
                //canvas.drawArc (mArcRect, 270, - redPercent * 360 , false, mPaint);
            	caculateCursorAngle();
            	if (isLessOneMinute) {
            		mPaint.setColor(mWhiteColor);
                	mPaint.setAlpha(bigCircleAlpha);
            		canvas.drawArc (mArcRect, 270, -(360-cursorImageAngle) , false, mPaint);
				}else {
					mPaint.setColor(timeLeftColor);
					canvas.drawCircle (xCenter, yCenter, radius, mPaint);
				}
            } else {
                canvas.drawArc (mArcRect, 270, + redPercent * 360 , false, mPaint);
            }

            // draw white arc here
            mPaint.setStrokeWidth(mStrokeSize);
            mPaint.setColor(timeLeftColor);
            if (mTimerMode) {
                //canvas.drawArc(mArcRect, 270, + whitePercent * 360, false, mPaint);
            	if (isLessOneMinute) {
					canvas.drawArc(mArcRect, 270, cursorImageAngle, false, mPaint);
				}
            	drawCursorImage(canvas, cursorImage2, xCenter, yCenter);
            } else {
                canvas.drawArc(mArcRect, 270 + (1 - whitePercent) * 360,
                        whitePercent * 360, false, mPaint);
            }

            if (mMarkerTime != -1 && radius > 0 && mIntervalTime != 0) {
                mPaint.setStrokeWidth(mMarkerStrokeSize);
                float angle = (float)(mMarkerTime % mIntervalTime) / (float)mIntervalTime * 360;
                // draw 2dips thick marker
                // the formula to draw the marker 1 unit thick is:
                // 180 / (radius * Math.PI)
                // after that we have to scale it by the screen density
                canvas.drawArc (mArcRect, 270 + angle, mScreenDensity *
                        (float) (360 / (radius * Math.PI)) , false, mPaint);
            }
            //drawRedDot(canvas, redPercent, xCenter, yCenter, radius);
        }
        if (mAnimate) {
            invalidate();
        }
   }

    protected void drawRedDot(
            Canvas canvas, float degrees, int xCenter, int yCenter, float radius) {
        mPaint.setColor(0x27B25F);
        float dotPercent;
        if (mTimerMode) {
        	mPaint.setColor(0x27B25F);
            dotPercent = 270 - degrees * 360;
        } else {
            dotPercent = 270 + degrees * 360;
        }

        final double dotRadians = Math.toRadians(dotPercent);
        canvas.drawCircle(xCenter + (float) (radius * Math.cos(dotRadians)),
                yCenter + (float) (radius * Math.sin(dotRadians)), mDotRadius, mFill);
    }
    
    
    protected void drawCursorImage(Canvas canvas, Drawable cursorImage, int x, int y){
    	cursorWidth=cursorImage.getIntrinsicWidth();
    	cursorHeight=cursorImage.getIntrinsicHeight();
    	int cursorLeft=(int) (xCenter-cursorWidth/2);
    	int cursorTop=(int) (yCenter-radius-cursorHeight/2);
    	int cursorRight=(int) (xCenter+cursorWidth/2);
    	int cursorBottom=(int) (yCenter-radius+cursorHeight/2);
    	canvas.save();
        canvas.rotate(cursorImageAngle, x, y);
        cursorImage.setBounds(cursorLeft,cursorTop, cursorRight, cursorBottom);
        cursorImage.draw(canvas);
        canvas.restore();
    	
    }

    public static final String PREF_CTV_PAUSED  = "_ctv_paused";
    public static final String PREF_CTV_INTERVAL  = "_ctv_interval";
    public static final String PREF_CTV_INTERVAL_START = "_ctv_interval_start";
    public static final String PREF_CTV_CURRENT_INTERVAL = "_ctv_current_interval";
    public static final String PREF_CTV_ACCUM_TIME = "_ctv_accum_time";
    public static final String PREF_CTV_TIMER_MODE = "_ctv_timer_mode";
    public static final String PREF_CTV_MARKER_TIME = "_ctv_marker_time";

    // Since this view is used in multiple places, use the key to save different instances
    public void writeToSharedPref(SharedPreferences prefs, String key) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean (key + PREF_CTV_PAUSED, mPaused);
        editor.putLong (key + PREF_CTV_INTERVAL, mIntervalTime);
        editor.putLong (key + PREF_CTV_INTERVAL_START, mIntervalStartTime);
        editor.putLong (key + PREF_CTV_CURRENT_INTERVAL, mCurrentIntervalTime);
        editor.putLong (key + PREF_CTV_ACCUM_TIME, mAccumulatedTime);
        editor.putLong (key + PREF_CTV_MARKER_TIME, mMarkerTime);
        editor.putBoolean (key + PREF_CTV_TIMER_MODE, mTimerMode);
        editor.apply();
    }

    public void readFromSharedPref(SharedPreferences prefs, String key) {
        mPaused = prefs.getBoolean(key + PREF_CTV_PAUSED, false);
        mIntervalTime = prefs.getLong(key + PREF_CTV_INTERVAL, 0);
        mIntervalStartTime = prefs.getLong(key + PREF_CTV_INTERVAL_START, -1);
        mCurrentIntervalTime = prefs.getLong(key + PREF_CTV_CURRENT_INTERVAL, 0);
        mAccumulatedTime = prefs.getLong(key + PREF_CTV_ACCUM_TIME, 0);
        mMarkerTime = prefs.getLong(key + PREF_CTV_MARKER_TIME, -1);
        mTimerMode = prefs.getBoolean(key + PREF_CTV_TIMER_MODE, false);
        mAnimate = (mIntervalStartTime != -1 && !mPaused);
    }

    public void clearSharedPref(SharedPreferences prefs, String key) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove (Stopwatches.PREF_START_TIME);
        editor.remove (Stopwatches.PREF_ACCUM_TIME);
        editor.remove (Stopwatches.PREF_STATE);
        editor.remove (key + PREF_CTV_PAUSED);
        editor.remove (key + PREF_CTV_INTERVAL);
        editor.remove (key + PREF_CTV_INTERVAL_START);
        editor.remove (key + PREF_CTV_CURRENT_INTERVAL);
        editor.remove (key + PREF_CTV_ACCUM_TIME);
        editor.remove (key + PREF_CTV_MARKER_TIME);
        editor.remove (key + PREF_CTV_TIMER_MODE);
        editor.apply();
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
    
    
    private void caculateAngle(float xTouch,float yTouch){
    	//Log.d(TAG,"caculateAngle");
    	double value=0;
    	double radian=0;
    	float degrees=0;
    	if (xTouch>=xCenter&yTouch<yCenter) {
    		Log.d(TAG, "第一像限");
    		value=(yCenter-yTouch)/(radius)>1?1d:(yCenter-yTouch)/(radius);
			radian=Math.acos(value);
			degrees=(float) Math.toDegrees(radian);
			newArea=0;
		}else if (xTouch>xCenter&yTouch>=yCenter) {
			Log.d(TAG, "第二像限");
			value=(yTouch-yCenter)/(radius)>1?1d:(yTouch-yCenter)/(radius);
			radian=Math.acos(value);
			degrees=180-(float) Math.toDegrees(radian);
			newArea=1;
		}else if (xTouch<=xCenter&yTouch>yCenter) {
			Log.d(TAG, "第三像限");
			value=(yTouch-yCenter)/(radius)>1?1d:(yTouch-yCenter)/(radius);
			radian=Math.acos((yTouch-yCenter)/(radius));
			degrees=180+(float) Math.toDegrees(radian);
			newArea=2;
		}else if(xTouch<xCenter&yTouch<=yCenter){
			Log.d(TAG, "第四像限");
			value=(yCenter-yTouch)/(radius)>1?1d:(yCenter-yTouch)/(radius);
			radian=Math.acos(value);
			degrees=360-(float) Math.toDegrees(radian);
			newArea=3;
		}
    	int span=newArea-lastArea;
    	Log.d(TAG,"span="+span);
    	switch (span) {
		case 3:
			if (minutes>0) {
				minutes--;
				lastArea=3;
			}else if(minutes==0){
				cursorImageAngle=0;
				lastArea=0;
			}
			break;
		case -3:
			minutes++;
			lastArea=0;
			break;
		default:
			int abs=Math.abs(span);
			if (abs==0||abs==1) {
				cursorImageAngle=degrees;
				lastArea=newArea;
			}
			break;
		}
    	Log.d(TAG,"minutes="+minutes);
    	Log.d(TAG,"cursorImageAngle="+cursorImageAngle);
    }
    
    private void caculateSetupTimeInMillion() {
		// TODO Auto-generated method stub
    	long time=(long) ((minutes+cursorImageAngle/360)*ONE_MINUTE_IN_MILLISECOND);
    	isMovingCursor=false;
    	cursorMoveListener.onCursorMove(time);
	}

	
	@Override
	public boolean onTouchEvent(MotionEvent arg0) {
		// TODO Auto-generated method stub
		//Log.d(TAG,"xCenter="+xCenter+" yCenter="+yCenter);
		cursorLeft=(float) (xCenter+radius*Math.sin(cursorImageAngle)-cursorWidth/2);
		cursorTop=(float) (yCenter-radius*Math.cos(cursorImageAngle)-cursorHeight/2);
		cursorRight=(float) (xCenter+radius*Math.sin(cursorImageAngle)+cursorWidth/2);
		cursorBottom=(float) (yCenter+radius*Math.cos(cursorImageAngle)+cursorHeight/2);
		if (mTimerMode&&!isAnimating()) {
			switch (arg0.getAction()) {
			case MotionEvent.ACTION_DOWN:
				Log.d(TAG,"ACTION_DOWN");
				deskClock.mViewPager.requestDisallowInterceptTouchEvent(true);
				xDown=arg0.getX();
				yDown=arg0.getY();
				Log.d(TAG, "xDown="+xDown+" yDown="+yDown);
				break;
			case MotionEvent.ACTION_MOVE:
				Log.d(TAG,"ACTION_MOVE");
				float x=arg0.getX();
				float y=arg0.getY();
				Log.d(TAG,"x="+x+" y="+y);
				if (xDown!=x&yDown!=y) {
					caculateAngle(arg0.getX(), arg0.getY());
				    isMovingCursor=true;
				    caculateSetupTimeInMillion();
					//invalidate();
				}
				break;
			case MotionEvent.ACTION_UP:
				Log.d(TAG,"ACTION_UP");
				deskClock.mViewPager.requestDisallowInterceptTouchEvent(false);
				isMovingCursor=false;
				break;
			default:
				break;
			}
		}
		return true;
	}
	
	public void diliveryActivityObj(Activity activity){
		deskClock=(DeskClock) activity;
	}
	
	public void setCircleTimeViewListener(CursorMoveListener cursorMoveListener){
		this.cursorMoveListener=cursorMoveListener;
	}
	
	public interface CursorMoveListener{
		abstract public void onCursorMove(long setupTime);
	}
}
