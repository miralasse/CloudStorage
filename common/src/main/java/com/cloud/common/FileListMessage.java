package com.cloud.common;

import java.util.List;

public class FileListMessage extends Message {
    private List<FileInfo> fileList;
    private static final long serialVersionUID = 16L;

    public FileListMessage(List<FileInfo> fileList) {
        this.fileList = fileList;
    }

    public List<FileInfo> getFileList() {
        return fileList;
    }

    public void setFileList(List<FileInfo> fileList) {
        this.fileList = fileList;
    }
}
