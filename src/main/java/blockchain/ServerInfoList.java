package blockchain;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class ServerInfoList {

    ArrayList<ServerInfo> serverInfos;

    public ServerInfoList() {
        serverInfos = new ArrayList<>();
    }

    public void initialiseFromFile(String filename) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(filename)));
            List<String> lines = Arrays.asList(content.trim().split("\n"));
            lines = lines.stream().filter(line -> !line.equals("")).map(line -> line.trim())
                    .collect(Collectors.toList());
            serverInfos = parseServerInfos(lines);
        } catch (IOException fne) {
            System.out.println("There was some error opening/searching the file. Check if the file exists or whether the file as correct permissions.");
            return;
        }
    }

    private ArrayList<ServerInfo> parseServerInfos(List<String> lines) {
        Map<Integer, ServerInfo> serverInfosMap = new HashMap<>();
        int limit = -1;

        for (String line : lines) {
            if (!validateEntry(line)) {
                continue;
            } else if (validateNumEntry(line)) {
                limit = parseNumEntry(line);
            }
        }

        // no servers.num line - will add nothing to the serverinfolist i.e. list will be of size 0
        if (limit == -1) {
            return new ArrayList<>();
        }

        for (String line : lines) {
            if (validateHostnameEntry(line)) {
                int index = parseIndexEntry(line);

                if (index >= limit)
                    continue;

                if (serverInfosMap.containsKey(index))
                    serverInfosMap.get(index).setHost(parseHostnameEntry(line));
                else
                    serverInfosMap.put(index, new ServerInfo(parseHostnameEntry(line), -1));
            }

            else if (validatePortEntry(line)) {
                int index = parseIndexEntry(line);

                if (index >= limit)
                    continue;

                if (serverInfosMap.containsKey(index))
                    serverInfosMap.get(index).setPort(parsePortEntry(line));
                else
                    serverInfosMap.put(index, new ServerInfo("", parsePortEntry(line)));
            }
        }

        ArrayList<ServerInfo> serverInfos = new ArrayList<>(Collections.nCopies(limit, null));

        for (Entry<Integer, ServerInfo> entry : serverInfosMap.entrySet()) {
            serverInfos.set(entry.getKey(), entry.getValue());
        }

        serverInfos = serverInfos.stream().map(entry -> entry != null ? (entry.isValid() ? entry : null) : entry)
                .collect(Collectors.toCollection(ArrayList::new));

        return serverInfos;
    }

    private boolean validateEntry(String entry) {
        return validateNumEntry(entry) || validateHostnameEntry(entry) || validatePortEntry(entry);
    }

    private boolean validateNumEntry(String entry) {
        return entry.matches("^servers\\.num=[0-9]+");
    }

    private boolean validateHostnameEntry(String entry) {
        return entry.matches(
                "^server[0-9]+\\.host=(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$");
    }

    private boolean validatePortEntry(String entry) {
        return entry.matches(
                "^server[0-9]+\\.port=()([1-9]|[1-5]?[0-9]{2,4}|6[1-4][0-9]{3}|65[1-4][0-9]{2}|655[1-2][0-9]|6553[1-5])$");
    }

    private int parseNumEntry(String entry) {
        return Integer.parseInt(entry.split("=")[1]);
    }

    private int parseIndexEntry(String entry) {
        return Integer.parseInt(entry.split("=")[0].split("\\.")[0].split("server")[1]);
    }

    private String parseHostnameEntry(String entry) {
        return entry.split("=")[1];
    }

    private int parsePortEntry(String entry) {
        return Integer.parseInt(entry.split("=")[1]);
    }

    public ArrayList<ServerInfo> getServerInfos() {
        return serverInfos;
    }

    public void setServerInfos(ArrayList<ServerInfo> serverInfos) {
        this.serverInfos = serverInfos;
    }

    public boolean addServerInfo(ServerInfo newServerInfo) {
        if (!newServerInfo.isValid())
            return false;
        serverInfos.add(newServerInfo);
        return true;
    }

    public boolean updateServerInfo(int index, ServerInfo newServerInfo) {
        if (index >= serverInfos.size() || index < 0)
            return false;
        if (!newServerInfo.isValid())
            return false;
        serverInfos.set(index, newServerInfo);
        return true;
    }

    public boolean removeServerInfo(int index) {
        if (index >= serverInfos.size() || index < 0 || serverInfos.size() == 0)
            return false;
        serverInfos.set(index, null);
        return true;
    }

    public boolean clearServerInfo() {
        serverInfos.clear();
        return true;
    }

    public void cleanNulls() {
        serverInfos = serverInfos.stream()
                .filter(entry -> entry != null)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public int size() {
        return serverInfos.size();
    }

    public String toString() {
        String s = "";
        for (int i = 0; i < serverInfos.size(); i++) {
            if (serverInfos.get(i) != null) {
                s += "Server" + i + ": " + serverInfos.get(i).getHost() + " " + serverInfos.get(i).getPort() + "\n";
            }
        }
        return s;
    }
}