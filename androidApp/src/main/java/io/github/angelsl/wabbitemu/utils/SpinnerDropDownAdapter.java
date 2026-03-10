package io.github.angelsl.wabbitemu.utils;

import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

public class SpinnerDropDownAdapter extends BaseAdapter implements SpinnerAdapter {

	private final List<String> mItems;
	private final Context mContext;

	public SpinnerDropDownAdapter(final Context context, final List<String> items) {
		mContext = context;
		mItems = items;
	}

	@Override
	public int getCount() {
		return mItems.size();
	}

	@Override
	public Object getItem(final int position) {
		return mItems.get(position);
	}

	@Override
	public long getItemId(final int position) {
		return mItems.get(position).hashCode();
	}

	@Override
	public View getView(final int position, final View convertView, final ViewGroup parent) {
		final TextView text = new TextView(mContext);
		text.setTextColor(Color.WHITE);
		text.setText(mItems.get(position));
		text.setPadding(20, 20, 20, 20);
		return text;
	}

}
