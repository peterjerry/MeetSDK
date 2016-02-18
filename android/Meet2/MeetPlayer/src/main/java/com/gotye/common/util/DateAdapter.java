package com.gotye.common.util;

import android.content.Context;
import android.support.v7.widget.AppCompatTextView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.gotye.meetplayer.R;

import java.util.List;

/**
 * Created by Michael.Ma on 2016/1/14.
 */
public class DateAdapter extends BaseAdapter {
    private List<String> mList;
    private Context mContext;
    private LayoutInflater mInflater;

    public DateAdapter(Context context, List<String> data) {
        mContext = context;
        mList = data;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        if (mList != null)
            return mList.size();

        return 0;
    }

    @Override
    public Object getItem(int position) {
        if (mList != null && position < mList.size())
            return mList.get(position);

        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = mInflater.inflate(R.layout.spinner_text, null);

        AppCompatTextView textview = (AppCompatTextView)convertView.findViewById(R.id.text1);
        textview.setText(mList.get(position));
        textview.setTextSize(18f);

        return convertView;
    }
}
