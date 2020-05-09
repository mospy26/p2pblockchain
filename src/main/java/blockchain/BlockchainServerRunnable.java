package blockchain;

import java.io.*;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;

public class BlockchainServerRunnable implements Runnable {

    private Socket clientSocket;
    private Blockchain blockchain;
    private HashMap<ServerInfo, Date> serverStatus;

    public BlockchainServerRunnable(Socket clientSocket, Blockchain blockchain,
            HashMap<ServerInfo, Date> serverStatus) {
        this.clientSocket = clientSocket;
        this.blockchain = blockchain;
        this.serverStatus = serverStatus;
    }

    public void run() {
        try {
            serverHandler(clientSocket.getInputStream(), clientSocket.getOutputStream());
            clientSocket.close();
        } catch (IOException e) {
        }
    }

    public void serverHandler(InputStream clientInputStream, OutputStream clientOutputStream) {

        BufferedReader inputReader = new BufferedReader(new InputStreamReader(clientInputStream));
        PrintWriter outWriter = new PrintWriter(clientOutputStream, true);

        try {
            while (true) {
                String inputLine = inputReader.readLine();
                if (inputLine == null) {
                    break;
                }

                String[] tokens = inputLine.split("\\|");
                switch (tokens[0]) {
                    case "tx":
                        if (blockchain.addTransaction(inputLine))
                            outWriter.print("Accepted\n\n");
                        else
                            outWriter.print("Rejected\n\n");
                        outWriter.flush();
                        break;
                    case "pb":
                        outWriter.print(blockchain.toString() + "\n");
                        outWriter.flush();
                        break;
                    case "cc":
                        return;
                    case "hb":
                        if (!hbCommandValid(inputLine))
                            break;

                        String localIP = (((InetSocketAddress) clientSocket.getLocalSocketAddress()).getAddress())
                                .toString().replace("/", "");
                        String remoteIP = (((InetSocketAddress) clientSocket.getRemoteSocketAddress()).getAddress())
                                .toString().replace("/", "");

                        int localPort = clientSocket.getLocalPort();
                        int remotePort = Integer.parseInt(tokens[1]);

                        heartBeatReceivedHandler(remoteIP, localIP, remotePort, localPort, tokens[2]);

                        break;

                    case "si":
                        if (!siCommandValid(inputLine, tokens))
                            break;

                        String slocalIP = (((InetSocketAddress) clientSocket.getLocalSocketAddress()).getAddress())
                                .toString().replace("/", "");
                        String sremoteIP = (((InetSocketAddress) clientSocket.getRemoteSocketAddress()).getAddress())
                                .toString().replace("/", "");

                        int slocalPort = clientSocket.getLocalPort();
                        int sremotePort = Integer.parseInt(tokens[1]);

                        siCommandHandler(sremoteIP, slocalIP, sremotePort, slocalPort, inputLine);

                        break;

                    default:
                        outWriter.print("Error\n\n");
                        outWriter.flush();
                }
            }
        } catch (IOException e) {
        }
    }

    private void heartBeatReceivedHandler(String remoteIP, String localIP, int remotePort, int localPort, String seq) {
        serverStatus.put(new ServerInfo(remoteIP, remotePort), new Date());

        // convert remote and local config to serverinfo
        ServerInfo local = new ServerInfo(localIP, localPort);
        ServerInfo remote = new ServerInfo(remoteIP, remotePort);

        // first token
        if (seq.equals("0")) {
            ArrayList<Thread> threadArrayList = new ArrayList<>();
            for (ServerInfo s : serverStatus.keySet()) {
                if (s.equals(remote) || s.equals(local)) {
                    continue;
                }
                Thread thread = new Thread(
                        new HeartBeatClientRunnable(s, "si|" + localPort + "|" + remoteIP + "|" + remotePort));
                threadArrayList.add(thread);
                thread.start();
            }
            for (Thread thread : threadArrayList) {
                try {
                    // TODO do we need 2000 ms here cause HeartBeatClient already handles it
                    thread.join();
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }

    private void siCommandHandler(String remoteIP, String localIP, int remotePort, int localPort, String line) {

        ServerInfo remote = new ServerInfo(remoteIP, remotePort);
        ServerInfo local = new ServerInfo(localIP, localPort);

        if (!serverStatus.keySet().contains(remote)) {
            serverStatus.put(remote, new Date());

            // relay
            ArrayList<Thread> threadArrayList = new ArrayList<>();
            for (ServerInfo s : serverStatus.keySet()) {
                if (s.equals(remote) || s.equals(local)) {
                    continue;
                }
                Thread thread = new Thread(new HeartBeatClientRunnable(s, "si|" + localPort + "|" + remoteIP + "|" + remotePort));
                threadArrayList.add(thread);
                thread.start();
            }
            for (Thread thread : threadArrayList) {
                try {
                    // TODO do we need 2000 ms here cause HeartBeatClient already handles it
                    thread.join();
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }

    private boolean hbCommandValid(String line) {
        return line.matches(
                "^hb\\|([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])\\|[0-9]+$")
                && Integer.parseInt(line.split("\\|")[1]) >= 1024;
    }

    private boolean siCommandValid(String line, String[] tokens) {
        String portRegex = "([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])";
        String hostRegex = "(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])";
        return line.matches("^si\\|" + portRegex + "\\|" + hostRegex + "\\|" + portRegex + "$")
                && Integer.parseInt(tokens[1]) > 1023 && Integer.parseInt(tokens[3]) > 1023;
    }
}
