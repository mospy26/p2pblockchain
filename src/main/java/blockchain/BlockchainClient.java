package blockchain;


import java.util.ArrayList;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BlockchainClient {

    final private static String SUCCESS_STRING = "Succeeded\n\n";
    final private static String FAILED_STRING = "Failed\n\n";
    final private static String UNKNOWNCOMMAND_STRING = "Unknown Command\n\n";

    public static void main(String[] args) {

        if (args.length != 1) {
            return;
        }
        String configFileName = args[0];

        ServerInfoList pl = new ServerInfoList();
        pl.initialiseFromFile(configFileName);

        Scanner sc = new Scanner(System.in);
        BlockchainClient bcc = new BlockchainClient();

        while (true) {
            String message = sc.nextLine();
            String reply = "";

            if (message.matches("^ls$")) {
                reply += pl.toString() + "\n";
            }

            else if (message.matches(
                    "^ad\\|.*\\|.*$")) {
                reply += bcc.adCommandHandler(pl, message) ? SUCCESS_STRING : FAILED_STRING;
            }

            else if (message.matches("^rm\\|.*$")) {
                reply += bcc.rmCommandHandler(pl, message) ? SUCCESS_STRING : FAILED_STRING;
            }

            else if (message.matches("^cl$")) {
                if (bcc.clCommandHandler(pl)) reply += SUCCESS_STRING;
            }

            else if (message.matches("^up\\|.*\\|.*\\|.*$")) {
                reply += bcc.upCommandHandler(pl, message) ? SUCCESS_STRING : FAILED_STRING;
            }

            else if (message.matches("^pb$")) {
                bcc.pbCommandHandler(pl);
            }

            else if (message.matches("^pb\\|.*")) {
                bcc.pbCommandHandlerWithArguments(pl, message);
            }

            else if (message.matches("^tx\\|.*\\|.*")) {
                bcc.txCommandHandler(pl, message);
            }

            else if (message.matches("^sd$")) {
                sc.close();
                return;
            }

            else {
                reply = UNKNOWNCOMMAND_STRING;
            }

            System.out.print(reply);
        }
    }

    public boolean clCommandHandler(ServerInfoList pl) {
        pl.cleanNulls();
        return true;
    }

    public boolean adCommandHandler(ServerInfoList pl, String message) {
        
        String hostname;
        int port;
        
        try {
            String[] commands = message.split("\\|");
            hostname = commands[1];
            port = Integer.parseInt(commands[2]);
        } catch (ArrayIndexOutOfBoundsException ae) {
            System.err.println("Insufficient arguments provided");
            System.err.println("Usage: ad|<hostname>|<port>");
            return false;
        } catch (NumberFormatException nfe) {
            System.err.println("Port should be integers");
            System.err.println("Usage: ad|<hostname>|<port>");
            return false;
        }

        return pl.addServerInfo(new ServerInfo(hostname, port));
    }

    public boolean upCommandHandler(ServerInfoList pl, String message) {
        int index;
        int port;
        String hostname;

        try {       
            String[] commands = message.split("\\|");
            index = Integer.parseInt(commands[1]);
            hostname = commands[2];
            port = Integer.parseInt(commands[3]);
        } catch (ArrayIndexOutOfBoundsException ae) {
            System.err.println("Insufficient arguments provided");
            System.err.println("Usage: up|<index>|<hostname>|<port>");
            return false;
        } catch (NumberFormatException nfe) {
            System.err.println("Index and ports should be integers");
            System.err.println("Usage: up|<index>|<hostname>|<port>");
            return false;
        }

        return pl.updateServerInfo(index, new ServerInfo(hostname, port));
    }

    public boolean rmCommandHandler(ServerInfoList pl, String message) {
        
        int index;

        try {
            index = Integer.parseInt(message.split("\\|")[1]);
        } catch (ArrayIndexOutOfBoundsException ae) {
            System.err.println("Insufficient arguments");
            System.err.println("Usage: rm|<index>");
            return false;
        } catch (NumberFormatException nfe) {
            System.err.println("Index should be an integer");
            System.err.println("Usage: rm|<index>");
            return false;
        }

        return pl.removeServerInfo(index);
    }

    public void txCommandHandler(ServerInfoList pl, String message) {
        broadcast(pl, message);
    }

    public void pbCommandHandler(ServerInfoList pl) {
        broadcast(pl, "pb");
    }

    public void pbCommandHandlerWithArguments(ServerInfoList pl, String message) {
        String[] commands = message.split("\\|");
        ArrayList<Integer> serverIndices = new ArrayList<>();
        for (String command : commands) {
            if (command.equals("pb"))
                continue;
            try {

                int index = Integer.parseInt(command);
                if (index < 0 || index >= pl.size())
                    throw new IllegalArgumentException("Invalid arguments to the \"pb\" command");
                serverIndices.add(Integer.parseInt(command));

            } catch (IllegalArgumentException e) { // This also handles NumberFormatException
                System.err.println("Invalid arguments " + e.getMessage());
                System.err.println("Usage: pb|<index>|<index>|...");
                continue;
            }
        }
        multicast(pl, serverIndices, "pb");
    }

    public void unicast(int serverNumber, ServerInfo p, String message) {
        BlockchainClientRunnable bcr = new BlockchainClientRunnable(serverNumber, p.getHost(), p.getPort(), message);
        try {
            Thread t = new Thread(bcr);
            t.start();
            t.join();
        } catch (Exception e) {
            return;
        }
        System.out.println(bcr.getReply());
    }

    public void broadcast(ServerInfoList pl, String message) {
        ArrayList<Integer> serverIndices = IntStream.range(0, pl.size())
                                            .boxed()
                                            .collect(Collectors.toCollection(ArrayList::new));

        selectedcastHelper(pl, serverIndices, message);

    }

    public void multicast(ServerInfoList serverInfoList, ArrayList<Integer> serverIndices, String message) {
        selectedcastHelper(serverInfoList, serverIndices, message);
    }

    private void selectedcastHelper(ServerInfoList serverInfoList, ArrayList<Integer> serverIndices, String message) {
        ArrayList<BlockchainClientRunnable> bcrs = new ArrayList<>();
        ArrayList<Thread> threads = new ArrayList<>();
        int i = 0;
        String reply = "";

        for (ServerInfo s : serverInfoList.getServerInfos()) {

            if (!serverIndices.contains(i) || s == null) {
                i++;
                continue;
            }

            BlockchainClientRunnable bcr = new BlockchainClientRunnable(i++, s.getHost(), s.getPort(), message);
            bcrs.add(bcr);

            Thread t = new Thread(bcr);
            threads.add(t);
            t.start();
        }
        try {
            for (Thread t : threads) {
                t.join();
            }
            for (BlockchainClientRunnable bcr : bcrs) {
                reply += bcr.getReply();
                if (bcr.isServerUnreachable()) {
                    reply += "Server is not available\n\n";
                } else {
                    reply += "\n";
                }
            }
            System.out.print(reply);
        } catch (Exception e) {
            return;
        }
    }
}