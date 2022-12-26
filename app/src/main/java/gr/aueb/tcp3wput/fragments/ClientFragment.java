package gr.aueb.tcp3wput.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.IntStream;

import gr.aueb.tcp3wput.R;
import gr.aueb.tcp3wput.adapters.LogAdapter;

public class ClientFragment extends Fragment {

    private Button startRequestingButton;
    private EditText serverAIPEditText;
    private EditText serverBIPEditText;
    private EditText serverAPortEDitText;
    private EditText serverBPortEDitText;
    private EditText serverAAnalogyEditText;
    private EditText serverBAnalogyEditText;
    private LogAdapter logAdapter;
    private ListView clientLogListView;

    private RequestAsyncTask rat;

    private boolean clientLogUnlocked = true;

    private void unlockLog(){
        clientLogUnlocked = true;
    }

    private void lockLog(){
        clientLogUnlocked = false;
    }

    private ArrayList<String> clientLogs = new ArrayList<>();

    private synchronized String getMostRecentLog(){
        if(this.clientLogs.size() == 0)
            return null;
        return this.clientLogs.remove(0);
    }

    private synchronized void addMessageToLog(String log){
        if(clientLogUnlocked)
            this.clientLogs.add(log);
    }

    private Runnable uiRefresherRunnable = new Runnable() {
        @Override
        public void run() {
            String log = getMostRecentLog();
            if(log != null){
                logAdapter.addMessage(log);
                logAdapter.notifyDataSetChanged();
            }
            clientLogListView.postDelayed(this, 1);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_client, container, false);
        initUI(view);
        startRequestingButton.setOnClickListener(view1 -> {
            if(rat!=null && rat.getStatus() == AsyncTask.Status.RUNNING){
                logAdapter.clearLogs();
                logAdapter.notifyDataSetChanged();
                lockLog();
                synchronized (clientLogs){clientLogs.clear();}
                rat.terminate();
                rat.cancel(true);
                rat = null;
                setEditTextsEnabled(true);
                startRequestingButton.setText("START REQUESTING");

            } else {
                rat = new RequestAsyncTask();
                rat.execute();
            }
        });

        return view;
    }

    private void showResultsDialog(String results){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
                alertDialog.setTitle("Results");
                alertDialog.setMessage(results);

                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "COPY", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("tcp3wput-res", results);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(getContext(), "Results copied to clipboard!", Toast.LENGTH_SHORT).show();
                    }
                });

                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "IGNORE", (dialogInterface, i) -> {
                    alertDialog.dismiss();
                });

                alertDialog.show();
            }
        });

    }

    private void setEditTextsEnabled(boolean status) {
        serverAIPEditText.setEnabled(status);
        serverBIPEditText.setEnabled(status);
        serverAPortEDitText.setEnabled(status);
        serverBPortEDitText.setEnabled(status);
        serverAAnalogyEditText.setEnabled(status);
        serverBAnalogyEditText.setEnabled(status);
    }

    private void initUI(View view) {
        startRequestingButton = (Button) view.findViewById(R.id.startRequesting);
        serverAIPEditText = (EditText) view.findViewById(R.id.serverAIPEditText);
        serverBIPEditText = (EditText) view.findViewById(R.id.serverBIPEditText);
        serverAPortEDitText = (EditText) view.findViewById(R.id.serverAPortEDitText);
        serverBPortEDitText = (EditText) view.findViewById(R.id.serverBPortEDitText);
        serverAAnalogyEditText = (EditText) view.findViewById(R.id.serverAAnalogy);
        serverBAnalogyEditText = (EditText) view.findViewById(R.id.serverBAnalogy);

        clientLogListView = (ListView) view.findViewById(R.id.clientLogListView);
        logAdapter = new LogAdapter(getContext());
        clientLogListView.setAdapter(logAdapter);
        clientLogListView.post(uiRefresherRunnable);
    }

    private void log(String s){
        Log.d("CLIENT", ""+s);
    }


    private class RequestAsyncTask extends AsyncTask<Void, Void, ArrayList<ArrayList<Long>>> {

        private DataOutputStream dataAOutputStream = null;
        private DataInputStream dataAInputStream = null;
        private DataOutputStream dataBOutputStream = null;
        private DataInputStream dataBInputStream = null;
        private Socket clientASocket;
        private Socket clientBSocket;

        private String serverAAddress = "";
        private String serverBAddress = "";

        private int portA = 0;
        private int portB = 0;

        private ArrayList<ArrayList<Long>> metrics = new ArrayList<>();

        // files to request from A server
        private int filesANumberServer = 0;
        // files to request from B server
        private int filesBNumberServer = 0;
        // step to take when changing the file range. This is essentially what the other
        // client will ask.
        // the next file we are expecting.
        private volatile int nextAfile = 1;
        private volatile int nextBfile = 1;

        private String directory_name = "received_files";

        private ThreadPoolExecutor executor;

        private Callable<Object> task1 = new Callable<Object>() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public Object call() throws Exception {
                addMessageToLog("Starting to request from server A");
                log("Starting to request from server A");
                // breaks with an if inside
                ArrayList<Long> requestTimes = new ArrayList<>();
                while (true) {
                    long startTime = System.nanoTime(); // get the start time in nanoseconds
                    JSONObject requestforJSON = createRequest(serverAAddress, portA, nextAfile, filesANumberServer, "A");

                    dataAOutputStream.writeUTF((requestforJSON.toString()));
                    dataAOutputStream.flush();
                    receiveFile(clientASocket, dataAInputStream);
                    addMessageToLog("Exited receive File from server A");
                    log("Exited receive File from server A");
                    long endTime = System.nanoTime(); // get the end time in nanoseconds
                    long elapsedTime = endTime - startTime; // calculate the elapsed time in nanoseconds
                    addMessageToLog("It took: " + elapsedTime + " nanoseconds to complete the request.");
                    log("It took: " + elapsedTime + " nanoseconds to complete the request.");
                    requestTimes.add(elapsedTime);

                    // essentially the "Out of range number was given or there aren't any more
                    // files" acts as a done in the requests
                    // execute the code that you want to measure the time for
                    JSONArray filenames = (JSONArray) requestforJSON.get("Filenames");
                    String json_filenames_to_string = filenames.toString();
                    if (json_filenames_to_string.contains("Out of range number was given or there aren't any more files")) {
                        addMessageToLog("Breaking from for A loop");
                        clientASocket.close();
                        break;
                    }

                }
                addMessageToLog("Finished requesting from server A");
                return requestTimes;
            }
        };

        private Callable<Object> task2 = new Callable<Object>() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public Object call() throws Exception {
                addMessageToLog("Starting to request from server B");
                ArrayList<Long> requestTimes = new ArrayList<>();
                while (true) {
                    long startTime = System.nanoTime(); // get the start time in nanoseconds

                    JSONObject requestforJSON = createRequest(serverBAddress, portB, nextBfile, filesBNumberServer,
                            "B");

                    dataBOutputStream.writeUTF((requestforJSON.toString()));
                    dataBOutputStream.flush();
                    receiveFile(clientBSocket, dataBInputStream);
                    addMessageToLog("Exited receive File from server B");
                    long endTime = System.nanoTime(); // get the end time in nanoseconds
                    long elapsedTime = endTime - startTime; // calculate the elapsed time in nanoseconds
                    addMessageToLog("It took: " + elapsedTime + " nanoseconds to complete the request");
                    requestTimes.add(elapsedTime);

                    // essentially the "Out of range number was given or there aren't any more
                    // files" acts as a done in the requests
                    // execute the code that you want to measure the time for
                    JSONArray filenames = (JSONArray) requestforJSON.get("Filenames");
                    String json_filenames_to_string = filenames.toString();
                    if (json_filenames_to_string.contains("Out of range number was given or there aren't any more files")) {
                        addMessageToLog("Breaking from B for loop");
                        clientBSocket.close();
                        break;
                    }

                }
                addMessageToLog("Finished requesting from server B");
                return requestTimes;
            }
        };

        private void receiveFile(Socket socket, DataInputStream dataInputStream) {
            int counter = 0;
            int files_expected = 0;
            try {
                files_expected = dataInputStream.readInt();
            } catch (IOException e1) {
                addMessageToLog("Exception: " + e1 + "occured while receiving files expected");
                return;
            }
            addMessageToLog("Files expected in receive file: " + files_expected + " and counter is initiliazed: " + counter);
            while (!socket.isClosed()) {
                try {
                    // receive filename from connection
                    String filename = dataInputStream.readUTF();
                    addMessageToLog("Read: " + filename + " filename from server");
                    // receive file size from connection
                    long filesize = dataInputStream.readLong();
                    addMessageToLog("Read: " + filesize + " filesize from server");

                    // create the directory if it doesn't exist

                    /*File dir = new File(getContext().getFilesDir(), directory_name);
                    if(!dir.exists()){
                        dir.mkdirs();
                    }

                    // create new file if it doesn't exist
                    File destination = new File(directory_name, filename);
                    destination.createNewFile();
                    */
                    File destination = new File(getContext().getDir(directory_name, Context.MODE_PRIVATE) + filename);

                    OutputStream outputStream;
                    try {
                        outputStream = new FileOutputStream(destination, true);

                        int bytes = 0;
                        byte[] buffer = new byte[4 * 1024];
                        while (filesize > 0
                                && (bytes = dataInputStream.read(buffer, 0, (int) Math.min(buffer.length, filesize))) != -1) {
                            outputStream.write(buffer, 0, bytes);
                            filesize -= bytes; // read upto file size

                        }
                        addMessageToLog("Finished receiving file");
                        outputStream.close();

                    } catch (IOException e) {
                        addMessageToLog(e.getMessage());
                    }

                } catch (Exception e) {
                    addMessageToLog("Exception: " + e + " occured in receive file.");
                    break;
                }
                counter++;
                addMessageToLog("Counter has become: " + counter + " and files expected are: " + files_expected);
                if (counter >= files_expected) {
                    addMessageToLog("Breaking from receiving file while loop");
                    break;
                }
            }
        }

        /**
         * Call the function to create a new JSON object which is the request. It also
         * tweaks the necessary variables so the clients can ask for the next set of
         * files from the server.
         *
         * @param serverAddress     The server address we are creating the request for.
         * @param port              The port of the server.
         * @param nextfile          The next file we are awaiting.
         * @param filesNumberServer The number of files we ask in each request.
         * @param aSteporBStep      Choose what client variables we modify. These
         *                          differentiate from A and B.
         * @return returns the JSON object.
         */
        @RequiresApi(api = Build.VERSION_CODES.N)
        private JSONObject createRequest(String serverAddress, int port, int nextfile, int filesNumberServer, String aSteporBStep) {
            JSONObject requestforJSON = createRequestObject(serverAddress, port, createNumberArray(nextfile, nextfile + filesNumberServer - 1, aSteporBStep));
            addMessageToLog("Created new request for server: " + aSteporBStep);
            return requestforJSON;

        }

        /**
         * This function must be called each time after the client has received the
         * requested file/files. Creates a new JSON object which is a request object and
         * sends it to the server.
         *
         * @param serverIP    the server we will connect to
         * @param port        the port that the server listens to
         * @param filenumbers the file numbers we want to generate filenames for.
         * @return returns the JSON object created.
         */
        private JSONObject createRequestObject(String serverIP, int port, int[] filenumbers) {
            addMessageToLog("Creating new request");

            JSONObject nJsonObject = new JSONObject();
            String[] fileRequests = createFileNameFromNumberLEGACY(filenumbers);

            try {
                nJsonObject.put("ServerIP", serverIP);
                nJsonObject.put("ServerPort", port);
                JSONArray array = new JSONArray();
                for(String s: fileRequests)
                    array.put(s);
                nJsonObject.put("Filenames", array);
            } catch (JSONException e) {
                addMessageToLog(e.getMessage());
            }
            addMessageToLog("Created new JSON object: " + nJsonObject);

            return nJsonObject;
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
                    names[i] = "Out of range number was given or there aren't any more files"; // FNF: File Not Found
                } else {
                    String formatted = String.format("%03d", number[i]);
                    if (formatted == null){
                        names[i] = "Formated file number remained null after string format"; // BADF: Bad Number Formatting
                        break;
                    }  else {
                        names[i] = "s" + formatted;
                    }
                }
            }
            return names;
        }

        /**
         * Creates a new number number array inside the range.
         *
         * @param start the start of the number array.
         * @param to    the end of the number array.
         * @return Returns the number array created.
         */
        @RequiresApi(api = Build.VERSION_CODES.N)
        private synchronized int[] createNumberArray(int start, int to, String aSteporBStep) {
            int[] numberArray = IntStream.rangeClosed(start, to).toArray();
            addMessageToLog("Creating number array... File number A: " + filesANumberServer + " File Number B: " + filesBNumberServer);
            if (aSteporBStep.equals("A")) {
                nextAfile = to;
                addAStep(filesBNumberServer);
            } else {
                nextBfile = to;
                addBStep(filesANumberServer);
            }
            return numberArray;
        }

        /**
         * Adds the step into the next file to request the next file from the
         * corresponding server.
         */
        private void addAStep(int step) {
            this.nextAfile += step + 1;
            addMessageToLog("Step was: " + step + " and next file to be expected from B: " + nextAfile);
        }

        /**
         * Adds the step into the next file to request the next file from the
         * corresponding server.
         */
        private void addBStep(int step) {
            this.nextBfile += step + 1;
            addMessageToLog("Step was: " + step + " and next file to be expected from B: " + nextBfile);
        }

        private void setUpClient() {
            this.serverAAddress = serverAIPEditText.getText().toString();
            this.serverBAddress = serverBIPEditText.getText().toString();

            this.portA = Integer.parseInt(serverAPortEDitText.getText().toString());
            this.portB = Integer.parseInt(serverBPortEDitText.getText().toString());

            this.filesANumberServer = Integer.parseInt(serverAAnalogyEditText.getText().toString());
            this.filesBNumberServer = Integer.parseInt(serverBAnalogyEditText.getText().toString());

            this.nextBfile = filesANumberServer + 1;
            log(clientToString());
            addMessageToLog(clientToString());

            try {
                clientASocket = new Socket(serverAAddress, portA);
                clientBSocket = new Socket(serverBAddress, portB);
                // read and send json objects
                dataAOutputStream = new DataOutputStream(clientASocket.getOutputStream());
                dataAInputStream = new DataInputStream(clientASocket.getInputStream());
                dataBOutputStream = new DataOutputStream(clientBSocket.getOutputStream());
                dataBInputStream = new DataInputStream(clientBSocket.getInputStream());

            } catch (Exception e) {
                log(e.getMessage());
                addMessageToLog("Exception occured while trying to create client: " + e);
                return;
            }
            addMessageToLog("Succesfully connected to server");
        }

        private String clientToString() {
            return "\nClient with: \n"
                    + "server A address: " + serverAAddress + "\n"
                    + "server B address: " + serverBAddress + "\n"
                    + "request analogy A " + filesANumberServer + "\n"
                    + "request analogy B " + filesBNumberServer + "\n"
                    + "server A port: " + portA + "\n"
                    + "server B port: " + portB + "\n"
                    + "next file expected from A initialization: " + nextAfile + "\n"
                    + "next file expected from B initialization: " + nextBfile + "\n";
        }

        @Override
        protected ArrayList<ArrayList<Long>> doInBackground(Void... voids) {

            setUpClient();
            // Create a list of tasks
            executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
            List<Callable<Object>> tasks = Arrays.asList(task1, task2);
            ArrayList<ArrayList<Long>> metrics = new ArrayList<>();

            try {
                addMessageToLog("Invoking all tasks.");

                List<Future<Object>> results = executor.invokeAll(tasks);
                for (Future<Object> result : results) {
                    ArrayList<Long> value = (ArrayList<Long>) result.get(); // get the result of the task
                    metrics.add(value);
                }
                addMessageToLog("Finished requests.");
            } catch (InterruptedException e) {
                log(e.getMessage());
                addMessageToLog("Error while waiting for threads to finish and getting results: " + e);
            } catch (ExecutionException e) {
                log(e.getMessage());
                addMessageToLog("Error while waiting for threads to finish and getting results " + e);
            }
            executor.shutdown();

            return metrics;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            unlockLog();
            setEditTextsEnabled(false);
            startRequestingButton.setText("STOP");
        }

        public void terminate(){
            try {
                if(!clientASocket.isClosed())
                    clientASocket.close();

                if(!clientBSocket.isClosed())
                    clientBSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            executor.shutdown();
        }

        @SuppressLint("ResourceAsColor")
        @Override
        protected void onPostExecute(ArrayList<ArrayList<Long>> arrayLists) {
            super.onPostExecute(arrayLists);
            addMessageToLog("DONE RECEIVING.");
            setEditTextsEnabled(true);
            startRequestingButton.setText("START REQUESTING");
            String results = "";
            for (ArrayList<Long> metric : arrayLists) {
                results += metric.toString() + "\n";
            }
            showResultsDialog(results);
            lockLog();
            log("DONE!");
        }
    }
}