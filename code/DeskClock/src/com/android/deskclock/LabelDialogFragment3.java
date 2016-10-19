package com.android.deskclock;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;

public class LabelDialogFragment3 extends DialogFragment implements View.OnClickListener{
	private EditText mLabelInput;
	private TextView ok;
	private TextView cancel;
	private LabelDialogListener listener;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
		View view=inflater.inflate(R.layout.alarm_label_dialog, null);
		mLabelInput=(EditText)view.findViewById(R.id.label_input);
		ok=(TextView)view.findViewById(R.id.btn_ok);
		cancel=(TextView)view.findViewById(R.id.btn_cancel);
		ok.setOnClickListener(this);
		cancel.setOnClickListener(this);
		return view;
	}
	
	public void setLabelDialogListener(LabelDialogListener listener){
		this.listener=listener;
	}
	
	interface LabelDialogListener{
		abstract void onLabelChange(String label);
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.btn_ok:
			String label=mLabelInput.getText().toString();
			listener.onLabelChange(label);
			dismiss();
			break;
		case R.id.btn_cancel:
			dismiss();
            break;
		default:
			break;
		}
	}
	
}
