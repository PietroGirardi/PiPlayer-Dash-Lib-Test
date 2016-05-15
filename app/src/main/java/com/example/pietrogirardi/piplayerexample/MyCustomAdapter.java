package com.example.pietrogirardi.piplayerexample;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by pietrogirardi on 14/05/16.
 */
public class MyCustomAdapter extends ArrayAdapter<Sample> {

    private final List<Sample> list;
    private final Activity context;

    public MyCustomAdapter(Activity context, List<Sample> list) {
        super(context, R.layout.rowlist, list);
        this.context = context;
        this.list = list;
    }

    static class ViewHolder {
        protected TextView text;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = null;
        if (convertView == null) {
            LayoutInflater inflator = context.getLayoutInflater();
            view = inflator.inflate(R.layout.rowlist, null);
            final ViewHolder viewHolder = new ViewHolder();
            viewHolder.text = (TextView) view.findViewById(R.id.label);

            view.setTag(viewHolder);
        } else {
            view = convertView;
        }
        ViewHolder holder = (ViewHolder) view.getTag();
        holder.text.setText(list.get(position).getName());
        return view;
    }
}