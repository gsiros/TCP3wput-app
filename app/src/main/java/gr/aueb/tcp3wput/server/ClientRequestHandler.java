package gr.aueb.tcp3wput.server;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.Socket;

public class ClientRequestHandler extends Thread {

    private Context context;
    private Socket clientSocket;
    private DataInputStream dataInputStream = null;
    private DataOutputStream dataOutputStream = null;

    private final String FILE_EXTENSION = ".m4s";

    public ClientRequestHandler(Context context, Socket clientSocket){
        this.context = context;
        this.clientSocket = clientSocket;
        try {
            this.dataOutputStream = new DataOutputStream(this.clientSocket.getOutputStream());
            this. dataInputStream = new DataInputStream(this.clientSocket.getInputStream());
        } catch (Exception e) {
            Log.e("BAD_SOCK_STREAM", "ClientRequestHandler: error in getting input/output streams from connection.", e);
        }
    }

    /**
     * Author; F. Bistas
     *
     * Filenames are received in the form: String(["s001.ms", "s002.ms"]).
     * Removes the commas the brackets and the quotes from the string.
     *
     * @param filenames the filenames json array turned toString()
     * @return all the filenames to read
     */
    private String[] parseToStringArray(String filenames) {
        filenames = filenames.replace("[", "");
        filenames = filenames.replace("]", "");
        filenames = filenames.replace("\"", "");
        filenames = filenames.replace("Out of range number was given or there aren't any more files", "");
        filenames = filenames.replace("Formated file number remained null after string format", "");
        Log.d("PROG", "parseToStringArray: "+"Turned received json array to: '" + filenames+"'");
        if(filenames.equals("")){
            return new String[0];
        }
        return filenames.split(",");
    }

    @Override
    public void run() {
        Log.d("PROG", "run: "+"Handling connection from: " + this.clientSocket.getInetAddress());
        while(!this.clientSocket.isClosed()){ // WARNING: WHAT HAPPENS IF CONNECTION IS CLOSED IS UNDEFINED.
            try {
                String string_json = dataInputStream.readUTF();
                JSONObject json = new JSONObject(string_json);
                JSONArray filenames = ((JSONArray) json.get("Filenames"));
                String[] workable = parseToStringArray(filenames.toString());
                Log.d("PROG", "run: "+"Length of workable is: " + workable.length);
                // means its finished sending ALL the files
                if (workable.length == 0) {
                    this.clientSocket.close();
                    break;
                }
                this.dataOutputStream.writeInt(workable.length);
                this.dataOutputStream.flush();

                // Modified code to fit older SDKs:
                for(String filename : workable)
                    sendFile(filename);

                Log.d("PROG", "run: "+"Finished sending files to the client.");
            } catch (Exception e) {
                Log.e("BAD_SEND_FILE", "run: "+"Exception occurred while receiving requests and sending files.", e);

                try {
                    this.clientSocket.close();
                } catch (IOException e1) {
                    Log.e("BAD_CONN_CLOSE", "run: Couldn't close the connection.", e1);
                }
                break;
            }
        }
    }

    private void sendFile(String filename) {
        try {
            InputStream fileInputStream = getSingleFragment(filename);
            // send the file name
            String fullname = filename+this.FILE_EXTENSION;
            Log.d("PROG", "sendFile: "+"Sending new file to client: " + fullname);
            this.dataOutputStream.writeUTF(filename+this.FILE_EXTENSION);
            this.dataOutputStream.flush();

            // send the file size
            int fsize = fileInputStream.available();
            Log.d("PROG", "sendFile: "+"Sending file length: " + fsize + " to client.");
            this.dataOutputStream.writeLong(fsize);
            this.dataOutputStream.flush();
            // chunking the file
            int bytes;
            byte[] buffer = new byte[4 * 1024];
            while ((bytes = fileInputStream.read(buffer)) != -1) {
                this.dataOutputStream.write(buffer, 0, bytes);
                this.dataOutputStream.flush();
            }
            fileInputStream.close();
        } catch (Exception e) {
            Log.e("BAD_SEND_FILE", "sendFile: "+"Exception occured while reading or the sending the file: " + filename, e);
        }
    }

    /**
     * This method fetches a single fragment from the Android assets folder and returns
     * an InputStream linked to it.
     *
     * @param fragname String of fragment file name
     * @return returns an InputStream linked to the fragment from the assets folder
     * @throws IOException when asset is missing/nonexistent
     */
    private InputStream getSingleFragment(String fragname) throws IOException {
        InputStream fragment;
        AssetManager am = context.getAssets();
        InputStream inputStream = am.open(fragname+this.FILE_EXTENSION);
        if (inputStream != null){
            fragment = inputStream;
        } else {
            throw new FileNotFoundException("Requested fragment was not found.");
        }
        return fragment;
    }

}
