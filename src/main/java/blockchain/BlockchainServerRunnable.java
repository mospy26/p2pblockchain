package blockchain;


import java.io.*;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

public class BlockchainServerRunnable implements Runnable {

    private Socket clientSocket;
    private Blockchain blockchain;
    private ConcurrentHashMap<ServerInfo, Date> serverStatus;
    private ObjectOutputStream objectWriter;
    private final String PORT_REGEX = "([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])";
    private final String HOST_REGEX = "(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])";

    public BlockchainServerRunnable(Socket clientSocket, Blockchain blockchain,
            ConcurrentHashMap<ServerInfo, Date> serverStatus) {
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
        PrintWriter outWriter = new PrintWriter(clientOutputStream);

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

                        String myIP = (((InetSocketAddress) clientSocket.getLocalSocketAddress()).getAddress())
                                .toString().replace("/", "");
                        String sremoteIP = tokens[2];

                        int myPort = clientSocket.getLocalPort();
                        int sremotePort = Integer.parseInt(tokens[3]);

                        String originatorIP = (((InetSocketAddress) clientSocket.getRemoteSocketAddress()).getAddress())
                                .toString().replace("/", "");
                        int originatorPort = Integer.parseInt(tokens[1]);

                        siCommandHandler(sremoteIP, myIP, originatorIP, sremotePort, myPort, originatorPort, inputLine);

                        break;

                    case "lb":
                        if (!lbCommandValid(inputLine, tokens))
                            break;

                        int senderPort = Integer.parseInt(tokens[1]);
                        int blockchainSize = Integer.parseInt(tokens[2]);
                        byte[] hash = Base64.getDecoder().decode(tokens[3]);

                        byte[] myHash = blockchain.getHead() == null ? null : blockchain.getHead().calculateHash();
                        boolean isGreaterHash = myHash == null ? true : !compareHash(myHash, hash);

                        String remote = (((InetSocketAddress) clientSocket.getRemoteSocketAddress()).getAddress())
                                .toString().replace("/", "");

                        if (blockchainSize > blockchain.getLength()) {
                            catchup(remote, senderPort);
                        }
                        else if (blockchainSize == blockchain.getLength() && !isGreaterHash) {
                            catchup(remote, senderPort);
                        }

                        break;

                    case "cu":
                        try {
                            objectWriter = new ObjectOutputStream(clientOutputStream);
                        } catch (IOException e1) {
                            return;
                        }
                        if (!cuCommandValid(inputLine))
                            break;

                        Block block;
                        if (tokens.length == 1) block = fetchBlock(null);
                        else block = fetchBlock(tokens[1]);

                        // Invalid hash or block
                        if (block == null) {
                            objectWriter.writeObject(null);
                            objectWriter.flush();
                            break;
                        }

                        sendBlock(block, objectWriter);
                        objectWriter.close();
                        break;

                    default:
                        outWriter.print("Error\n\n");
                        outWriter.flush();
                }
            }
        } catch (IOException e) {
        }
    }

    private void catchup(String remoteAddr, int remotePort) {
        // Establish new connection

        // Do the catchup

        initialCatchup(blockchain, remoteAddr, remotePort);
        return;
    }

    private void sendBlock(Block block, ObjectOutputStream sender) {
        try {
            sender.writeObject(block);
            sender.flush();
        } catch (IOException e) {
            return;
        }
    }

    private Block fetchBlock(String hash) {
        Block block = blockchain.getHead();

        while (block != null) {
            if (Base64.getEncoder().encodeToString(block.calculateHash()).equals(hash)) {
                return block;
            }
            block = block.getPreviousBlock();
        }
        return null;
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

    private void siCommandHandler(String remoteIP, String localIP, String originatorIP, int remotePort, int localPort, int originatorPort, String line) {

        ServerInfo remote = new ServerInfo(remoteIP, remotePort);
        ServerInfo local = new ServerInfo(localIP, localPort);
        ServerInfo originator = new ServerInfo(originatorIP, originatorPort);

        if (!serverStatus.keySet().contains(remote)) {
            serverStatus.put(remote, new Date());

            // relay
            ArrayList<Thread> threadArrayList = new ArrayList<>();
            for (ServerInfo s : serverStatus.keySet()) {
                if (s.equals(remote) || s.equals(local) || s.equals(originator)) {
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
                "^hb\\|" + PORT_REGEX + "\\|[0-9]+$")
                && Integer.parseInt(line.split("\\|")[1]) >= 1024;
    }

    private boolean siCommandValid(String line, String[] tokens) {
        return line.matches("^si\\|" + PORT_REGEX + "\\|" + HOST_REGEX + "\\|" + PORT_REGEX + "$")
                && Integer.parseInt(tokens[1]) > 1023 && Integer.parseInt(tokens[3]) > 1023;
    }

    private boolean lbCommandValid(String line, String[] tokens) {
        return line.matches("lb\\|" + PORT_REGEX + "\\|[0-9]+\\|.+") && Integer.parseInt(tokens[1]) > 1023;
    }

    private boolean cuCommandValid(String line) {
        return line.matches("cu") || line.matches("cu\\|.+");
    }

    // returns true if my hash is lesser than the other one
    private boolean compareHash(byte[] hash1, byte[] hash2) {
        if (hash1.length != hash2.length) return false;
        for (int i = 0; i < hash1.length; i++) {
            if (hash1[i] < hash2[i]) return true;
            else if (hash1[i] == hash2[i]) continue;
            else return false;
        }
        return false;
    }

    public void initialCatchup(Blockchain blockchain, String remoteIP, int remotePort) {

        try {
            blockchain.setHead(null);
            blockchain.setLength(0);

            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(remoteIP, remotePort), 2000);
            
            InputStream clientInputStream = socket.getInputStream();
            OutputStream clientOutputStream = socket.getOutputStream();

            PrintWriter outWriter = new PrintWriter(clientOutputStream, true);

            outWriter.println("cu");
            outWriter.flush();

            ObjectInputStream inputReader = new ObjectInputStream(clientInputStream);
            Block block = (Block) inputReader.readObject();

            if (block == null) {
                socket.close();
                return;
            }
            
            blockchain.addBlock(block);

            socket.close();

            while (!Arrays.equals(block.getPreviousHash(), new byte[32])) {
                Socket nextSocket = new Socket();
                nextSocket.connect(new InetSocketAddress(remoteIP, remotePort), 2000);

                InputStream is = nextSocket.getInputStream();
                OutputStream os = nextSocket.getOutputStream();

                PrintWriter out = new PrintWriter(os, true);
                
                out.println("cu|" + Base64.getEncoder().encodeToString(block.getPreviousHash()));
                out.flush();

                ObjectInputStream in = new ObjectInputStream(is);
                block = (Block) in.readObject();

                blockchain.addBlock(block);

                nextSocket.close();
            }
            return;

        } catch (Exception e) {
            return;
        }
    }
}