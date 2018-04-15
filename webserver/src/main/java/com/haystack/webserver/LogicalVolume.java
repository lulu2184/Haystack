package com.haystack.webserver;

import java.util.ArrayList;
import java.util.List;

public class LogicalVolume {
    public int logicalNum;
    public List<String> physicalVolumes;

    public LogicalVolume(int logicalNum, List<String> physicalVolumes) {
        this.logicalNum = logicalNum;
        this.physicalVolumes = physicalVolumes;
    }

    public String getPhysicalVolumeListString() {
        List<String> volumesWithQuotes = new ArrayList<>();
        for (String url : physicalVolumes) {
            volumesWithQuotes.add("'" + url + "'");
        }
        return volumesWithQuotes.toString();
    }
}
