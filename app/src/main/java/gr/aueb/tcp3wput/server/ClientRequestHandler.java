package gr.aueb.tcp3wput.server;

import java.net.Socket;

public class ClientRequestHandler extends Thread {

    private Socket clientSocket;

    public ClientRequestHandler(Socket clientSocket){
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        super.run();
        /* TODO: handle the socket programming;
            Must agree on the file sending method.
         */
    }
}
