package gr.aueb.tcp3wput.server;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.Socket;

public class ClientRequestHandler extends Thread {

    private Context context;
    private Socket clientSocket;
    private ObjectInputStream objectInputStream = null;

    public ClientRequestHandler(Context context, Socket clientSocket){
        this.context = context;
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        super.run();
        try {
            // Set up the object input stream that will receive the JSON-formatted requests.
            this.objectInputStream = new ObjectInputStream(this.clientSocket.getInputStream());
            JSONObject jsonRequest = (JSONObject) this.objectInputStream.readObject();
            // Process the JSON request.
            String[] fragnames = processRequest(jsonRequest);
            InputStream[] frags = getFragments(fragnames);
            // TODO: decide on how the files are going to be sent.

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method processes the JSON-formatted client request and
     * returns the filenames of the requested fragments as Strings.
     *
     * @param jsonRequest the client request in JSON format
     * @return returns array of names of the requested fragments
     */
    private String[] processRequest(JSONObject jsonRequest) {
        // TODO: Check if the request is directed to the correct server.
        String[] fragnames;
        try {
            int[] filenums = (int[]) jsonRequest.get("Filenames");
            fragnames = createFileNameFromNumberLEGACY(filenums);
            return fragnames;
        } catch (JSONException e) {
            Log.e("BADJSONREQ", "processRequest: bad JSON format.", e);
            return null;
        }
    }

    /**
     * *Modified* version of Bistas' & Toumazatos' 'createFileNameFromNumber'
     * method to support lower Android SDKs.
     *
     * Creates a new file name of the following format:
     * sXXX where XXX starts from 001 and goes up to 160.
     *
     * @param number the number of the file we want to request next
     * @return The formated file names or an error if an error occured.
     */
    private String[] createFileNameFromNumberLEGACY(int[] number) {

        String[] names = new String[number.length];

        for(int i=0; i<number.length; i++){
            if (number[i] < 1 || number[i] > 160) {
                names[i] = "FNF"; // FNF: File Not Found
            } else {
                String formatted = String.format("%03d", number[i]);
                if (formatted == null){
                    names[i] = "BNF"; // BADF: Bad Number Formatting
                    break;
                }  else {
                    names[i] = "s" + formatted;
                }
            }
        }
        return names;
    }

    /**
     * This method fetches the fragments from the Android assets folder and returns
     * an array of InputStreams.
     *
     * @param fragnames String array of the fragment file names
     * @return returns an InputStream array linked to each fragment from the assets folder
     */
    private InputStream[] getFragments(String[] fragnames){
        InputStream[] fragments = new InputStream[fragnames.length];
        AssetManager am = context.getAssets();

        for(int i=0; i<fragnames.length; i++){
            try {
                InputStream inputStream = am.open(fragnames[i]);
                if (inputStream != null){
                    fragments[i] = inputStream;
                } else {
                    throw new FileNotFoundException("Requested fragment" + i + " named '" + fragments[i] + "' was not found.");
                }
            } catch (IOException e) {
                Log.e("BAD_FRAG_READ", "getFragments: error when opening fragment.", e);
            }
        }
        return fragments;
    }

}
