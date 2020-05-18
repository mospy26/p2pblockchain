package blockchain;


import java.io.*;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Stack;
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

                        int myPort = clientSocket.getLocalPort();
                        int sremotePort = Integer.parseInt(tokens[3]);
                        String sremoteIP = new InetSocketAddress(tokens[2], sremotePort).getAddress().toString().replace("/", "");

                        String originatorIP = (((InetSocketAddress) clientSocket.getRemoteSocketAddress()).getAddress())
                                .toString().replace("/", "");
                        int originatorPort = Integer.parseInt(tokens[1]);

                        siCommandHandler(sremoteIP, myIP, originatorIP, sremotePort, myPort, originatorPort, inputLine);

                        break;

                    case "lb":
                    System.out.println(inputLine);
                        if (!lbCommandValid(inputLine, tokens))
                            break;

                        int senderPort = Integer.parseInt(tokens[1]);
                        int blockchainSize = Integer.parseInt(tokens[2]);
                        byte[] hash = Base64.getDecoder().decode(tokens[3]);

                        byte[] myHash = blockchain.getHead() == null ? null : blockchain.getHead().calculateHash();
                        boolean isSmallerHash = myHash == null ? false : compareHash(myHash, hash);

                        String remote = (((InetSocketAddress) clientSocket.getRemoteSocketAddress()).getAddress())
                                .toString().replace("/", "");

                        System.out.println(blockchain.getLength());
                        System.out.println(blockchainSize);
                        System.out.println(!isSmallerHash);
                        if (blockchainSize > blockchain.getLength()) {
                            synchronized(blockchain) {
                                catchup(remote, senderPort, tokens[3], false);
                            }
                        } else if (blockchainSize == blockchain.getLength() && !isSmallerHash) {
                            synchronized(blockchain) {
                                catchup(remote, senderPort, tokens[3], true);
                            }
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
                        if (tokens.length == 1)
                            block = blockchain.fetchBlock(null);
                        else
                            block = blockchain.fetchBlock(tokens[1]);

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

    /**
     * 
     * Catcup algorithm
     *  1. Get all blocks from the peer that I dont have
     *  2. prune my blockchain to remove those unnecessary blocks and replace them with their blocks
     * 
     * @param remoteAddr
     * @param remotePort
     * @param hash
     * @param prune
     */
    private synchronized void catchup(String remoteAddr, int remotePort, String hash, boolean prune) {
        try {
            Block block;
            Stack<Block> blocks = new Stack<>();

            // case where my blockchain is empty
            if (blockchain.getHead() == null) {
                while (!Arrays.equals(Base64.getDecoder().decode(hash), new byte[32])) {
                    block = getBlockFromCatchup(remoteAddr, remotePort, hash);
                    blockchain.addBlock(block);
                    hash = Base64.getEncoder().encodeToString(block.getPreviousHash());
                }
                return;
            }

            // get all blocks that are unknown to blockchain
            while (blockchain.fetchBlock(hash) == null) {
                block = getBlockFromCatchup(remoteAddr, remotePort, hash);
                if (block == null) break;
                blocks.push(block);
                hash = Base64.getEncoder().encodeToString(block.getPreviousHash());
            }

            // This must be the intersection as the above while loop broke
            // If block was null i.e. all blocks in remote blockchain are different, append them all to my blockchain
            block = blockchain.fetchBlock(hash);

            // remove all blocks until intersection and move all transactions into the pool
            ArrayList<Transaction> transactions = new ArrayList<>(blockchain.getPool());
            if (block != null) shiftBlockTransactionsToPool(transactions, block);
            else blockchain.clear();

            // add blocks from peer from the forked block onwards
            addBlocksFromPeer(blocks, transactions);

            // blockchain.setPool(transactions);
        } catch (Exception e) {
            return;
        }
    }

    private void shiftBlockTransactionsToPool(ArrayList<Transaction> transactions, Block intersectionBlock) {
        Block removedBlock = blockchain.getHead();
        byte[] intersectionHash = intersectionBlock.calculateHash();

        while (!Arrays.equals(removedBlock.calculateHash(), intersectionHash)) {
            removedBlock = blockchain.removeHead();
            transactions.addAll(removedBlock.getTransactions());
        }
    }

    private void addBlocksFromPeer(Stack<Block> blocks, ArrayList<Transaction> transactions) {
        while (!blocks.empty()) {
            Block block = blocks.pop();
            blockchain.addBlockToHead(block);
            transactions.removeAll(block.getTransactions());
        }
    }

    private void sendBlock(Block block, ObjectOutputStream sender) {
        try {
            sender.writeObject(block);
            sender.flush();
        } catch (IOException e) {
            return;
        }
    }

    private void heartBeatReceivedHandler(String remoteIP, String localIP, int remotePort, int localPort, String seq) {

        // convert remote and local config to serverinfo
        ServerInfo local = new ServerInfo(localIP, localPort);
        ServerInfo remote = new ServerInfo(remoteIP, remotePort);

        // don't add yourself
        if (!local.equals(remote)) serverStatus.put(new ServerInfo(remoteIP, remotePort), new Date());

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
                    thread.join();
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }

    private void siCommandHandler(String remoteIP, String localIP, String originatorIP, int remotePort, int localPort,
            int originatorPort, String line) {

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
                Thread thread = new Thread(
                        new HeartBeatClientRunnable(s, "si|" + localPort + "|" + remoteIP + "|" + remotePort));
                threadArrayList.add(thread);
                thread.start();
            }
            for (Thread thread : threadArrayList) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }

    private boolean hbCommandValid(String line) {
        return line.matches("^hb\\|" + PORT_REGEX + "\\|[0-9]+$") && Integer.parseInt(line.split("\\|")[1]) >= 1024;
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

    // returns true if my hash is smaller than the other one
    private boolean compareHash(byte[] hash1, byte[] hash2) {
        // if (hash1.length != hash2.length) {
        //     return true;
        // }
        // if (Arrays.equals(hash1, hash2)) {
        //     return false;
        // }

        // for (int i = 0; i < hash1.length; i++) {
        //     if (Byte.compare(hash1[i], hash2[i]) > 0) return true;
        //     else return false;
        // }

        // return false;
        if (hash1.length != hash2.length) return false;
        for (int i = 0; i < hash1.length; i++) {
            if (hash1[i] < hash2[i]) return true;
            else if (hash1[i] == hash2[i]) continue;
            else return false;
        }
        return false;
    }

    private Block getBlockFromCatchup(String remoteAddr, int remotePort, String hash) {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(remoteAddr, remotePort), 3000);

            PrintWriter printWriter = new PrintWriter(socket.getOutputStream());
            printWriter.println("cu|" + hash);
            printWriter.flush();

            ObjectInputStream input = new ObjectInputStream(socket.getInputStream());

            Block block = (Block) input.readObject();

            socket.close();
            return block;            
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }


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