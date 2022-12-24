package gr.aueb.tcp3wput.singleton;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;


public class ServerBackend {

    private static ServerBackend instance;

    private Context context;
    private ServerSocket serverSocket;
    private final int SERVICE_PORT = 5096; // Hardcoded, perhaps should change.
    private ArrayList<String> logs;


    private ServerBackend(Context c) {
        this.context = c;
        this.logs = new ArrayList<>();
    }

    public synchronized static void initialize(Context applicationContext) {
        if (applicationContext == null)
            throw new NullPointerException("Provided application context is null");
        else if (instance == null) {
            instance = new ServerBackend(applicationContext);
        }
    }

    public static ServerBackend getInstance(){
        return instance;
    }

    public void startServer(){
        try {
            serverSocket = new ServerSocket(SERVICE_PORT);
            while(true) {
                Log.d("SERVINIT", "waiting for connection on port 5096.");
                addLog("Waiting for connection on port 5096.");
                Socket clientConn = serverSocket.accept();
                ClientRequestHandler crh = new ClientRequestHandler(context, clientConn);
                crh.start();
            }
        } catch (IOException e) {
            Log.d("BADSERV", "onCreate: "+e.getMessage());
        }
    }

    public void onDestroy(){
        // Safely close the open socket;
        try {
            if(!serverSocket.isClosed()){
                serverSocket.close();
            }
        }catch (IOException e){
            Log.e("BADCLOSESERV", "onDestroy: error while closing service socket.", e);
        }
        Toast.makeText(context, "Server stopped!", Toast.LENGTH_SHORT).show();
    }

    public synchronized String getMostRecentLog(){
        if(this.logs.size()==0)
            return null;
        return this.logs.remove(0);
    }

    public synchronized void addLog(String logMsg){
        DateTimeFormatter dtf = null;
        LocalDateTime now = null;
        String date = "";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            now = LocalDateTime.now();
            date = dtf.format(now) + " ";
        }
        this.logs.add(date+logMsg);
    }

    public synchronized void clearLogs() {
        this.logs.clear();
    }
}
