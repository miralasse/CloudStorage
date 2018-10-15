package com.cloud.common;

import java.io.Serializable;

public class FileInfo implements Serializable {
    private String fileName;
    private long fileSize;
    private static final long serialVersionUID = 15L;


    public FileInfo(String fileName, long fileSize) {
        this.fileName = fileName;
        this.fileSize = fileSize;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
}
