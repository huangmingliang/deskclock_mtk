package com.android.deskclock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.View;

/**
 * Add: Only for holster circle view
 * 
 * @author kuan.liang
 *
 */
public class HolsterCircleView extends View {

	private Paint mPaint;
	private int mColor;
	private float mCX;
	private float mCY;
	private float mR;

	public HolsterCircleView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}

	public HolsterCircleView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public HolsterCircleView(Context context) {
		super(context);
		init();
	}

	private void init() {
		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setXfermode(new PorterDuffXfermode(Mode.SRC_OVER));
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		mPaint.setColor(mColor);
		canvas.drawCircle(mCX, mCY, mR, mPaint);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		mCX = getPaddingLeft() + w / 2;
		mCY = getPaddingTop() + h / 2;
		mR = (w < h) ? w / 2 : h / 2;
	}

	public void setCircleColor(int color) {
		mColor = color;
		invalidate();
	}
}
