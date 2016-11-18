package com.android.deskclock.timer;

import com.android.deskclock.R;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.NumberPicker.Formatter;
import android.widget.Toast;

public class NewTimePickerFG extends DialogFragment implements Formatter {
	
	private OnTimeSaveListener listener;
	public NewTimePickerFG(OnTimeSaveListener listener) {
		this.listener = listener;
	}

	public interface OnTimeSaveListener {
		void onDateSet(long finalValue);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.timepicker_layout, null);
		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		final NumberPicker hourPicker = (NumberPicker) view
				.findViewById(R.id.listView_hour);
		final NumberPicker minutePicker = (NumberPicker) view
				.findViewById(R.id.listView_minute);
		final NumberPicker secondPicker = (NumberPicker) view
				.findViewById(R.id.listView_second);

		hourPicker.setMaxValue(23);
		hourPicker.setMinValue(0);
		hourPicker.setFormatter(this);

		minutePicker.setMaxValue(59);
		minutePicker.setMinValue(0);
		minutePicker.setFormatter(this);

		secondPicker.setMaxValue(59);
		secondPicker.setMinValue(0);
		secondPicker.setFormatter(this);

		Button btn_set = (Button) view.findViewById(R.id.time_picker_set);
		btn_set.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				int hourValue = hourPicker.getValue();
				int minuteValue = minutePicker.getValue();
				int secondValue = secondPicker.getValue();
				long finalValue = secondValue * 1000 + minuteValue * 60 * 1000
						+ hourValue * 60 * 1000 * 60;
				
				listener.onDateSet(finalValue);
				dismiss();
			}
		});

		Button btn_cancle = (Button) view.findViewById(R.id.time_picker_cancel);
		btn_cancle.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				
				dismiss();
			}
		});
	}

	@Override
	public String format(int arg0) {
		String str;
		if (arg0 < 10) {
			str = "0" + arg0;
		} else {

			str = "" + arg0;
		}
		return str;
	}

}
