package gr.aueb.tcp3wput.server;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TCP3wputServer extends Service {

    private Context context = getApplicationContext();

    private ServerSocket serverSocket;
    private final int SERVICE_PORT = 5001; // Hardcoded, perhaps should change.
    private Thread daemonThread;

    @Override
    public void onCreate() {
        super.onCreate();
        Toast.makeText(this, "Server started!", Toast.LENGTH_LONG).show();

        Runnable daemon = () -> {
            try {
                serverSocket = new ServerSocket(SERVICE_PORT);
                while(true) {
                    Socket clientConn = serverSocket.accept();
                     ClientRequestHandler crh = new ClientRequestHandler(context, clientConn);
                     crh.start();
                }
            } catch (IOException e) {
                Log.d("BADSERV", "onCreate: "+e.getMessage());
            }
        };
        daemonThread = new Thread(daemon);
        daemonThread.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Safely close the open socket;
        try {
            if(!serverSocket.isClosed()){
                serverSocket.close();
            }
        }catch (IOException e){
            Log.e("BADCLOSESERV", "onDestroy: error while closing service socket.", e);
        }
        // TODO: find a proper way to destory the daemon thread.
    }

    // onBind method to be left intact; no overloading required.
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
