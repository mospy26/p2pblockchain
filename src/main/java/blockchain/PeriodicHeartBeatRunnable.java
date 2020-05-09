package blockchain;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

public class PeriodicHeartBeatRunnable implements Runnable {

    private ConcurrentHashMap<ServerInfo, Date> serverStatus;
    private int selfPort;
    private int sequenceNumber;

    public PeriodicHeartBeatRunnable(ConcurrentHashMap<ServerInfo, Date> serverStatus, int selfPort) {
        this.serverStatus = serverStatus;
        this.sequenceNumber = 0;
        this.selfPort = selfPort;
    }

    @Override
    public void run() {
        while(true) {
            // broadcast HeartBeat message to all peers
            ArrayList<Thread> threadArrayList = new ArrayList<>();
            for (ServerInfo s : serverStatus.keySet()) {
                Thread thread = new Thread(new HeartBeatClientRunnable(s, "hb|" + selfPort + "|" + sequenceNumber));
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

            // increment the sequenceNumber
            sequenceNumber += 1;

            // sleep for two seconds
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return;
            }
        }
    }
}
