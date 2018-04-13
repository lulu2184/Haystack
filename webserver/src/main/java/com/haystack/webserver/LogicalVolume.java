package com.haystack.webserver;

import java.util.List;

public class LogicalVolume {
    public int logicalNum;
    public List<String> physicalVolumes;

    public LogicalVolume(int logicalNum, List<String> physicalVolumes) {
        this.logicalNum = logicalNum;
        this.physicalVolumes = physicalVolumes;
    }
}
