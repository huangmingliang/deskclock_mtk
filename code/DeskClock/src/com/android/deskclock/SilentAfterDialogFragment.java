package com.android.deskclock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.R.bool;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class SilentAfterDialogFragment extends DialogFragment implements OnClickListener{
	private String TAG=getClass().getSimpleName();
	private Context mContext;
	private ListView mSilentList;
	private TextView btn_cancel,btn_ok;
	private  SilentAdapter adapter=new SilentAdapter();
	private List<HashMap<String, Object>> maps=new ArrayList<HashMap<String,Object>>();
	private String entry;
	private String value;
	private String[]entries,values;
	private SilentDialogListener listener;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		mContext=getContext();
		initData();
		
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
		View view=inflater.inflate(R.layout.silent_after,null);
		mSilentList=(ListView)view.findViewById(R.id.silent_after_list);
		btn_cancel=(TextView)view.findViewById(R.id.btn_cancel);
		btn_ok=(TextView)view.findViewById(R.id.btn_ok);
		btn_cancel.setOnClickListener(this);
		btn_ok.setOnClickListener(this);
		mSilentList.setAdapter(adapter);
		return view;
	}
	
	private void initData(){
		entries=mContext.getResources().getStringArray(R.array.auto_silence_entries);
		values=mContext.getResources().getStringArray(R.array.auto_silence_values);
		if (entries!=null&&entries.length>0) {
			for (int i = 0; i < entries.length; i++) {
				HashMap<String, Object> map=new HashMap<String, Object>();
				map.put("time", entries[i]);
				if (i==0) {
					map.put("selected", true);
				}else {
					map.put("selected", false);
				}
				maps.add(map);
			}
		}
		
	}
	
	class SilentAdapter extends BaseAdapter{

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return maps.size();
		}

		@Override
		public HashMap<String, Object> getItem(int position) {
			// TODO Auto-generated method stub
			return maps.get(position);
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			HolderView holderView;
			LayoutInflater inflater=LayoutInflater.from(mContext);
			if (convertView==null) {
				holderView=new HolderView();
				convertView=inflater.inflate(R.layout.silent_after_item, null);
				holderView.rl=(RelativeLayout)convertView.findViewById(R.id.silent);
				holderView.mSilentTime=(TextView)convertView.findViewById(R.id.silent_time);
				holderView.img=(ImageView)convertView.findViewById(R.id.silent_img);
				holderView.rl.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						Log.d(TAG,"onClick position="+position);
						List<HashMap<String, Object>> newMaps=new ArrayList<HashMap<String,Object>>();
						for (int i = 0; i < entries.length; i++) {
							HashMap<String, Object> map=new HashMap<String, Object>();
							map.put("time", entries[i]);
							if (i==position) {
								map.put("selected", true);
								entry=entries[position];
								value=values[position];
							}else {
								map.put("selected", false);
							}
							newMaps.add(map);
						}
						maps.clear();
						maps.addAll(newMaps);
						notifyDataSetChanged();
					}
				});
				convertView.setTag(holderView);
			}else {
				holderView=(HolderView) convertView.getTag();
			}
			holderView.mSilentTime.setText(maps.get(position).get("time")+"");
			boolean isVisible=(boolean) maps.get(position).get("selected");
			holderView.img.setVisibility(isVisible?View.VISIBLE:View.INVISIBLE);
			return convertView;
		}
		
	}
	
	class HolderView{
		private RelativeLayout rl;
		private TextView mSilentTime;
		private ImageView img;
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.btn_cancel:
			Log.d(TAG, "entry="+entry+" value="+value);
			dismiss();
			break;
	    case R.id.btn_ok:
	    	Log.d(TAG, "entry="+entry+" value="+value);
	    	listener.onSilentAferChange(entry,value);
	    	dismiss();
	    	break;

		default:
			break;
		}
	}
	
	public void setSilentDialogListener(SilentDialogListener listener){
		this.listener=listener;
	}
	
	interface SilentDialogListener{
		public void onSilentAferChange(String entry,String value);
	}

}
