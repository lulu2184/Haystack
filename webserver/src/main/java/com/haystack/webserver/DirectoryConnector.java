package com.haystack.webserver;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

import java.util.ArrayList;
import java.util.List;

public class DirectoryConnector {

    private static String node = "128.2.13.144";
    private static Integer port = 9042;

    private static String keySpace = "haystack";

    private Cluster cluster;
    private Session session;

    public DirectoryConnector() {
        connect(node, port);
    }

    public void connect(String node, Integer port) {
        Cluster.Builder builder = Cluster.builder().addContactPoint(node);
        if (port != null) {
            builder.withPort(port);
        }
        cluster = builder.build();
        session = cluster.connect();
    }

    public void close() {
        session.close();
        cluster.close();
    }

    protected void finalize() {
        close();
    }

    public List<LogicalVolume> getWritableVolume(int limit) {
        ResultSet result = session.execute("SELECT * FROM " + keySpace + ".volumes " +
                "WHERE writable = true limit " + String.valueOf(limit));
        List<LogicalVolume> volumeList = new ArrayList<>();
        for (Row row : result.all()) {
            volumeList.add(new LogicalVolume(row.getInt("lid"),
                    row.getList("physical_machine", String.class)));
        }
        return volumeList;
    }

    public void storePhotoRecord(long pid, LogicalVolume logicalVolume) {
        session.execute(String.format("INSERT INTO %s.photo_entries (pid, cache_url, lid, physical_machine) " +
                "VALUES (%d, '', %d,%s)", keySpace, pid, logicalVolume.logicalNum),
                logicalVolume.physicalVolumes.toString());
    }

    public PhotoIndexEntry getPhotoRecord(long pid) {
        ResultSet result = session.execute(
                "SELECT * FROM %s.photo_entries, %s.volumes " +
                "WHERE pid = %d" + String.valueOf(pid));
        Row row = result.all().get(0);
        return new PhotoIndexEntry(row.getInt("pid"), row.getString("cache_url"),
                row.getInt("lid"), row.getList("physical_machine", String.class));
    }
}
