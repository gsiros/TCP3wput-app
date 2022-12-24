package gr.aueb.tcp3wput.fragments;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;

import gr.aueb.tcp3wput.R;
import gr.aueb.tcp3wput.adapters.LogAdapter;
import gr.aueb.tcp3wput.server.TCP3wputServer;
import gr.aueb.tcp3wput.singleton.ServerBackend;

public class ServerFragment extends Fragment {

    private Button startServerBtn;
    private ListView logView;
    public LogAdapter logAdapter;

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            String log = ServerBackend.getInstance().getMostRecentLog();
            if(log != null){
                logAdapter.addMessage(log);
                logAdapter.notifyDataSetChanged();
            }
            logView.postDelayed(this, 100);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_server, container, false);
        startServerBtn = (Button) view.findViewById(R.id.startServerButton);
        startServerBtn.setOnClickListener(view1 -> {
            if (isMyServiceRunning(TCP3wputServer.class)){
                getActivity().stopService(new Intent(getActivity(), TCP3wputServer.class));
                ServerBackend.getInstance().clearLogs();
                logAdapter.clearLogs();
                logAdapter.notifyDataSetChanged();
            }
            Intent servIntent = new Intent(getActivity(), TCP3wputServer.class);
            getActivity().startService(servIntent);
        });


        logView = (ListView) view.findViewById(R.id.serverLogListView);
        logAdapter = new LogAdapter(getActivity());
        logView.setAdapter(logAdapter);
        logView.post(runnable);
        return view;
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

}