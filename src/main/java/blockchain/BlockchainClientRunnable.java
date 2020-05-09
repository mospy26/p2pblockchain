package blockchain;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class BlockchainClientRunnable implements Runnable {

    private String reply;
    private String serverName;
    private int serverPort;
    private String message;
    private boolean serverNotReachable;


    public BlockchainClientRunnable(int serverNumber, String serverName, int portNumber, String message) {
        this.reply = "Server" + serverNumber + ": " + serverName + " " + portNumber + "\n"; // header string
        this.serverName = serverName;
        this.serverPort = portNumber;
        this.message = message;
    }

    public void run() {
        // implement your code here
        try (Socket server = new Socket(serverName, serverPort)) {
            server.setSoTimeout(2000);
            clientHandler(server.getInputStream(), server.getOutputStream(), message);
            server.close();
        } catch (Exception e) {
            serverNotReachable = true;
            return;
        }
    }

    public String getReply() {
        return reply;
    }

    public void clientHandler(InputStream serverInputStream, OutputStream serverOutputStream, String message) {
        BufferedReader inputReader = new BufferedReader(new InputStreamReader(serverInputStream));
        PrintWriter outWriter = new PrintWriter(serverOutputStream, true);
        
        Scanner sc = new Scanner(message);
        try {
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                outWriter.write(line + "\n");
                outWriter.flush();

                if (line.equals("cc")) return;
                
                String output = null;
                while ((output = inputReader.readLine()) != null && output.length() != 0) {
                    reply += output + "\n";
                }
            }
        } catch (Exception e) {
            return;
        } finally {
			outWriter.flush();
            outWriter.close();
            sc.close();
		}
    }

    public boolean isServerUnreachable() {
        return serverNotReachable;
    }
}