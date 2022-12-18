package gr.aueb.tcp3wput.server;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TCP3wputServer extends Service {

    private ServerSocket serverSocket;
    private Thread daemonThread;

    @Override
    public void onCreate() {
        super.onCreate();
        Toast.makeText(this, "Server started!", Toast.LENGTH_LONG).show();

        Runnable daemon = new Runnable() {
            @Override
            public void run() {
                try {
                    serverSocket = new ServerSocket(9999);
                    while(true){
                        try {
                            Socket clientConn = serverSocket.accept();

                            /* TODO: serve the incoming client connection;
                             ClientRequestHandler crh = new ClientRequestHandler(clientConn);
                            */

                        } catch (IOException e) {
                            Log.d("BADCONN", "onCreate: "+e.getMessage());
                        }
                    }
                } catch (IOException e) {
                    Log.d("BADSERV", "onCreate: "+e.getMessage());
                }
            }
        };
        daemonThread = new Thread(daemon);
        daemonThread.start();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // TODO: find a proper way to destory the daemon thread.
    }

    // onBind method to be left intact; no overloading required.
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
