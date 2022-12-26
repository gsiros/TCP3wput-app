package gr.aueb.tcp3wput.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import gr.aueb.tcp3wput.R;

public class LogAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<String> msgs;
    private LayoutInflater inflater;

    public LogAdapter(Context context){
        this.context = context;
        this.msgs = new ArrayList<String>();
        inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return this.msgs.size();
    }

    @Override
    public Object getItem(int i) {
        return this.msgs.get(i);
    }

    @Override
    public long getItemId(int i) {
        return this.msgs.indexOf(getItem(i));
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        view = inflater.inflate(R.layout.server_log_item, null);
        TextView logTV = (TextView) view.findViewById(R.id.logTextView);
        logTV.setText(this.msgs.get(i));
        return view;
    }

    public void addMessage(String msg){
        this.msgs.add(msg);
    }

    public void clearLogs() {
        this.msgs.clear();
    }
}
