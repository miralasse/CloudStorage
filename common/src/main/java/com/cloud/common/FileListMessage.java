package com.cloud.common;

import java.util.Map;

public class FileListMessage extends Message {
    private Map<String, Long> files;

    public Map<String, Long> getFiles() {
        return files;
    }

    public void setFiles(Map<String, Long> files) {
        this.files = files;
    }
}
