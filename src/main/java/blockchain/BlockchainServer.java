package blockchain;


import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

public class BlockchainServer {

    public static void main(String[] args) {

        if (args.length != 3) {
            return;
        }

        int localPort = 0;
        int remotePort = 0;
        String remoteHost = null;


        try {
            localPort = Integer.parseInt(args[0]);
            remoteHost = args[1];
            remotePort = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            return;
        }

        Blockchain blockchain = initialCatchup(remoteHost, remotePort);

        ConcurrentHashMap<ServerInfo, Date> serverStatus = new ConcurrentHashMap<ServerInfo, Date>();
        serverStatus.put(new ServerInfo(remoteHost, remotePort), new Date());

        // Start committer thread
        PeriodicCommitRunnable pcr = new PeriodicCommitRunnable(blockchain);
        Thread pct = new Thread(pcr);
        pct.start();

        // Start printing/peer "removing" thread
        PeriodicPrinterRunnable ppr = new PeriodicPrinterRunnable(serverStatus);
        Thread ppt = new Thread(ppr);
        ppt.start();

        // Heart beat about self existence broadcast thread
        PeriodicHeartBeatRunnable phbr = new PeriodicHeartBeatRunnable(serverStatus, localPort);
        Thread phbt = new Thread(phbr);
        phbt.start();

        // Latest Block hash sender thread
        PeriodicLatestBlockHashSender plbhr = new PeriodicLatestBlockHashSender(serverStatus, blockchain, localPort);
        Thread plbht = new Thread(plbhr);
        plbht.start();

        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(localPort);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new BlockchainServerRunnable(clientSocket, blockchain, serverStatus)).start();
            }
        } catch (IllegalArgumentException e) {
        } catch (IOException e) {
        } finally {
            try {
                pcr.setRunning(false);
                pct.join();
                ppt.interrupt();
                phbt.interrupt();
                if (serverSocket != null)
                    serverSocket.close();
            } catch (IOException e) {
            } catch (InterruptedException e) {
            }
        }
    }

    public static Blockchain initialCatchup(String remoteIP, int remotePort) {

        Blockchain blockchain = new Blockchain();

        try {

            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(remoteIP, remotePort), 2000);
            
            InputStream clientInputStream = socket.getInputStream();
            OutputStream clientOutputStream = socket.getOutputStream();

            // ObjectInputStream inputReader = new ObjectInputStream(clientInputStream);
            PrintWriter outWriter = new PrintWriter(clientOutputStream, true);

            outWriter.println("cu");
            outWriter.flush();

            // Block block = (Block) inputReader.readObject();
            Block block = getBlock(clientInputStream);

            if (block == null) {
                blockchain.setHead(null);
                socket.close();
                return blockchain;
            }
            
            blockchain.addBlock(block);

            socket.close();

            while (!Arrays.equals(block.getPreviousHash(), new byte[32])) {
                Socket nextSocket = new Socket();
                nextSocket.connect(new InetSocketAddress(remoteIP, remotePort), 2000);

                InputStream is = nextSocket.getInputStream();
                OutputStream os = nextSocket.getOutputStream();

                // ObjectInputStream in = new ObjectInputStream(is);
                PrintWriter out = new PrintWriter(os, true);
                
                out.println("cu|" + Base64.getEncoder().encodeToString(block.getPreviousHash()));
                out.flush();

                // block = (Block) in.readObject();
                block = getBlock(is);
                blockchain.addBlock(block);

                nextSocket.close();
            }
            return blockchain;

        } catch (Exception e) {
            return new Blockchain();
        }
    }

    private static Block getBlock(InputStream clientInputStream) throws IOException, ClassNotFoundException {
        ObjectInputStream inputReader = new ObjectInputStream(clientInputStream);
        Block block = (Block) inputReader.readObject();

        inputReader.close();
        return block;
    }
}