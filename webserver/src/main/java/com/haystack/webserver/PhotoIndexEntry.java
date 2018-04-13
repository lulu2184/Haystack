package com.haystack.webserver;

import java.util.List;

public class PhotoIndexEntry {
    public long pid;
    public String cacheUrl;
    public int logicalId;
    public List<String> physicalMachines;

    public PhotoIndexEntry(long pid, String cacheUrl, int logicalId,
                           List<String> physicalMachines) {
        this.pid = pid;
        this.cacheUrl = cacheUrl;
        this.logicalId = logicalId;
        this.physicalMachines = physicalMachines;
    }
}
