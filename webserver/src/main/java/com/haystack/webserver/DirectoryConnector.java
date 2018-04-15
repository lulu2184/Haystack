package com.haystack.webserver;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

import java.io.*;
import java.util.*;

public class DirectoryConnector {

    private static String directoryNode = "128.2.13.137";
    private static Integer port = 9045;
//    private static List<String> cacheList = Arrays.asList("128.2.13.144:4442");
    private static Random random = new Random();
    private static String cacheListFile = "cache_list.conf";

    private static String keySpace = "haystack";
    private static int volumeOnOneStore = 10;

    private Cluster cluster;
    private Session session;
    private int maxVolumeNum;
    private List<String> cacheList;
    private Set<String> storeUrlSet;

    public DirectoryConnector() {
        connect(directoryNode, port);
    }

    public void connect(String node, Integer port) {
        Cluster.Builder builder = Cluster.builder().addContactPoint(node);
        if (port != null) {
            builder.withPort(port);
        }
        cluster = builder.build();
        session = cluster.connect();
        cacheList = getCacheListFromFile();
        System.out.println("[Initialization] cache list: " + cacheList);
        storeUrlSet = getStoreUrlSet();
        System.out.println("[Initialization] store urls: " + storeUrlSet);

        getMaxVolumeNum();
    }

    protected void finalize() {
        session.close();
        cluster.close();
    }

    private void getMaxVolumeNum() {
        String query = "SELECT MAX(lid) FROM " + keySpace + ".volumes";
        ResultSet result = session.execute(query);
        this.maxVolumeNum = result.all().get(0).getInt(0);
    }

    public List<LogicalVolume> getWritableVolume(int limit) {
        String query = "SELECT * FROM " + keySpace + ".volumes " +
                "WHERE writable = true limit " + String.valueOf(limit) + " ALLOW FILTERING";
        System.out.println(query);
        ResultSet result = session.execute(query);
        List<LogicalVolume> volumeList = new ArrayList<>();
        for (Row row : result.all()) {
            volumeList.add(new LogicalVolume(row.getInt("lid"),
                    row.getList("physical_machine", String.class)));
        }
        return volumeList;
    }

    public void storePhotoRecord(long pid, LogicalVolume logicalVolume) {
        String cacheUrl = getCacheUrl();
        if (cacheUrl == null) {
            cacheUrl = "";
            System.err.println("[Store Photo] No usable cache server.");
        }
        String query = String.format("INSERT INTO %s.photo_entries (pid, cache_url, lid, physical_machine) " +
                        "VALUES (%d, '%s', %d,%s)", keySpace, pid, cacheUrl, logicalVolume.logicalNum,
                logicalVolume.getPhysicalVolumeListString());
        System.out.println(query);
        session.execute(query);
    }

    public PhotoIndexEntry getPhotoRecord(long pid) {
        ResultSet result = session.execute(
                "SELECT * FROM haystack.photo_entries " +
                "WHERE pid = " + String.valueOf(pid) + " ALLOW FILTERING");
        Row row = result.one();
        if (row == null)
            return null;
        return new PhotoIndexEntry(row.getLong("pid"), row.getString("cache_url"),
                row.getInt("lid"), row.getList("physical_machine", String.class));
    }

    public PhotoIndexEntry deletePhotoRecord(long pid) {
        PhotoIndexEntry photoEntry = getPhotoRecord(pid);
        if (photoEntry == null)
            return null;
        session.execute("DELETE FROM haystack.photo_entries " +
                        "WHERE pid = " + String.valueOf(pid));
        return photoEntry;
    }

    public void addCacheServer(String dns) {
        boolean exist = false;
        for (String cache : cacheList) {
            if (dns.equals(cache)) {
                exist = true;
                break;
            }
        }
        if (!exist) {
            cacheList.add(dns);
            System.out.println("[Add Cache] Current cache list: " + cacheList);
            flushCacheListToFile();
        }
    }

    public boolean deleteCacheServer(String dns) {
        boolean success = cacheList.remove(dns);
        System.out.println("[Remove Cache] Current cache list: "+ cacheList);
        flushCacheListToFile();
        return success;
    }

    public void flushCacheListToFile() {
        File file = new File(cacheListFile);
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(file));
            out.write(cacheList.toString());
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Set<String> getStoreUrlSet() {
        Set<String> urlSet = new HashSet<>();

        String query = "SELECT * FROM haystack.volumes";
        ResultSet result = session.execute(query);
        Row row;
        while ((row = result.one()) != null) {
             urlSet.addAll(row.getList("physical_machine", String.class));
        }
        return urlSet;
    }

    public List<String> getCacheListFromFile() {
        File file = new File(cacheListFile);
        if (!file.exists())
            return new ArrayList<>();
        try {
            BufferedReader in = new BufferedReader(new FileReader(file));
            String line = in.readLine();
            in.close();
            return new ArrayList<>(Arrays.asList(line.substring(1, line.length() - 1)
                    .split(",")));
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public boolean addStoreServers(List<String> dnsList) {
        for (String dns : dnsList) {
            if (storeUrlSet.contains(dns)) {
                System.out.println("[Failed to Add Store] Some servers have been added.");
                return false;
            }
        }

        LogicalVolume logicalVolume;
        for (int i = 0; i < volumeOnOneStore; i++){
            logicalVolume = new LogicalVolume(++maxVolumeNum, dnsList);
            String physicalVolumes = logicalVolume.getPhysicalVolumeListString();
            String query = String.format("INSERT INTO haystack.volumes" +
                            "(lid, physical_machine, writable) VALUES (%d, %s, true)",
                    logicalVolume.logicalNum, physicalVolumes);
            System.out.println(query);
            if (i == 0)
                storeUrlSet.addAll(logicalVolume.physicalVolumes);
            session.execute(query);
        }
        return true;
    }

    public void markVolumeAsUnwritable(int volumeId) {
        String query = String.format("UPDATE haystack.volumes SET writable = false WHERE lid = %d",
                volumeId);
        session.execute(query);
    }

    private String getCacheUrl() {
        if (cacheList.size() == 0)
            return null;
        int index = random.nextInt(30) % cacheList.size();
        return cacheList.get(index);
    }
}
