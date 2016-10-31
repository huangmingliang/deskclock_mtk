package com.android.deskclock.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Checkable;
import android.widget.RelativeLayout;

public class CLinearLayout extends RelativeLayout implements Checkable{
	
	private static final int[] CHECKED_STATE_SET = { android.R.attr.state_checked };
	private boolean mChecked = false;

	public CLinearLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void setChecked(boolean checked) {
		// TODO Auto-generated method stub
		if (checked != mChecked) {
			mChecked = checked;
			refreshDrawableState();//刷新状态
		}
	}

	@Override
	public boolean isChecked() {
		// TODO Auto-generated method stub
		return mChecked;
	}

	@Override
	public void toggle() {
		// TODO Auto-generated method stub
		setChecked(!mChecked);
	}
	
	@Override
	protected int[] onCreateDrawableState(int extraSpace) {
		// TODO Auto-generated method stub
		final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
		
		if (isChecked()) {
			mergeDrawableStates(drawableState, CHECKED_STATE_SET);
		} 
		return drawableState;
	}

}
