package gr.aueb.tcp3wput.server;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import gr.aueb.tcp3wput.MainActivity;
import gr.aueb.tcp3wput.fragments.ServerFragment;
import gr.aueb.tcp3wput.singleton.ServerBackend;

public class TCP3wputServer extends Service {

    private Thread daemonThread;

    @Override
    public void onCreate() {
        Toast.makeText(this, "Server started!", Toast.LENGTH_SHORT).show();

        Runnable daemon = () -> {
            ServerBackend.getInstance().startServer();
        };
        daemonThread = new Thread(daemon);
        daemonThread.start();
    }

    @Override
    public void onDestroy() {
        ServerBackend.getInstance().onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
