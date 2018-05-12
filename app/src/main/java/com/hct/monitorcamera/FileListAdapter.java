package com.hct.monitorcamera;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class FileListAdapter extends BaseAdapter {

    private ArrayList<String> mList;
    private Context mContext;
    private LayoutInflater mInflater;

    public FileListAdapter(Context context) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mList = new ArrayList<String>();
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = mInflater.inflate(R.layout.fileitem, null);
        final TextView textView = (TextView) convertView.findViewById(R.id.list_item);
        if (mList.size() > 0)
            textView.setText(mList.get(position));
        return convertView;
    }

    public void updateAdapter(ArrayList<String> list) {
        if (list != null && list.size() > 0) {
            mList = list;
            notifyDataSetChanged();
        }
    }
}
