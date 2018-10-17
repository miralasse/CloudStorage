package com.cloud.common;

public class FilePartMessage extends Message {
    private String fileName;
    private long partSize;
    private byte[] content;
    private int partNumber;
    private int numOfParts;
    private static final long serialVersionUID = 18L;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getPartSize() {
        return partSize;
    }

    public void setPartSize(long partSize) {
        this.partSize = partSize;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public int getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(int partNumber) {
        this.partNumber = partNumber;
    }

    public int getNumOfParts() {
        return numOfParts;
    }

    public void setNumOfParts(int numOfParts) {
        this.numOfParts = numOfParts;
    }
}
