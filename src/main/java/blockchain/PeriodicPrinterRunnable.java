package blockchain;


import java.util.Date;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class PeriodicPrinterRunnable implements Runnable{

    private ConcurrentHashMap<ServerInfo, Date> serverStatus;

    public PeriodicPrinterRunnable(ConcurrentHashMap<ServerInfo, Date> serverStatus) {
        this.serverStatus = serverStatus;
    }

    @Override
    public void run() {
        while(true) {
            for (Entry<ServerInfo, Date> entry : serverStatus.entrySet()) {
                // if greater than 2T, remove
                if (new Date().getTime() - entry.getValue().getTime() > 4000) {
                    serverStatus.remove(entry.getKey());
                } else {
                    System.out.print(entry.getKey() + " - " + entry.getValue() + " ");
                }
            }
            System.out.println();

            // sleep for two seconds
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return;
            }
        }
    }
}