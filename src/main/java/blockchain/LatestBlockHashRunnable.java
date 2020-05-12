package blockchain;


import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public class LatestBlockHashRunnable implements Runnable{

    private ServerInfo serverInfo;
    private String message;

    public LatestBlockHashRunnable(ServerInfo serverInfo, String message) {
        this.serverInfo = serverInfo;
        this.message = message;
    }

    @Override
    public void run() {
        try {
            // create socket with a timeout of 2 seconds
            Socket toServer = new Socket();
            toServer.connect(new InetSocketAddress(serverInfo.getHost(), serverInfo.getPort()), 2000);
            PrintWriter printWriter = new PrintWriter(toServer.getOutputStream(), true);

            // send the message forward
            printWriter.println(message);
            printWriter.flush();

            // close printWriter and socket
            printWriter.close();
            toServer.close();
        } catch (IOException e) {
        }
    }
}