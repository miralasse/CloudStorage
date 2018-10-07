package com.cloud.common;

import java.util.ArrayList;

public class FileListMessage extends Message {
    private ArrayList<FileInfo> fileList;

    public ArrayList<FileInfo> getFileList() {
        return fileList;
    }

    public void setFileList(ArrayList<FileInfo> fileList) {
        this.fileList = fileList;
    }
}
