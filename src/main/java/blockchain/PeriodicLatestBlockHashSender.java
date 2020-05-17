package blockchain;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class PeriodicLatestBlockHashSender implements Runnable {

    private ConcurrentHashMap<ServerInfo, Date> serverStatus;
    private Blockchain blockchain;
    private int selfPort;

    public PeriodicLatestBlockHashSender(ConcurrentHashMap<ServerInfo, Date> serverStatus, Blockchain blockchain,
            int port) {
        this.serverStatus = serverStatus;
        this.blockchain = blockchain;
        this.selfPort = port;
    }

    @Override
    public void run() {
        while (true) {
            HashSet<ServerInfo> servers = selectRandomServerInfos();

            ArrayList<Thread> threadArrayList = new ArrayList<>();
            if (blockchain.getLength() == 0) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                }
                continue;
            }

            for (ServerInfo s : servers) {
                String blockchainHash = Base64.getEncoder().encodeToString(blockchain.getHead().calculateHash());
                LatestBlockHashRunnable lbr = new LatestBlockHashRunnable(s, "lb|" + selfPort + "|" + blockchain.getLength() + "|" + blockchainHash);
                Thread thread = new Thread(lbr);
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

            // sleep for two seconds
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private HashSet<ServerInfo> selectRandomServerInfos() {
        Random rand = new Random();
        HashSet<ServerInfo> servers = new HashSet<>();
        Object[] allServers = serverStatus.keySet().toArray();

        if (serverStatus.size() < 5) return new HashSet<ServerInfo>(serverStatus.keySet());

        while(servers.size() < Math.min(serverStatus.size(), 5)) {
            servers.add((ServerInfo) allServers[rand.nextInt(allServers.length)]);
        }

        return servers;
    }
}