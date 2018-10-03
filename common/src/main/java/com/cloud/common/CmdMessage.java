package com.cloud.common;

public class CmdMessage extends Message {

    public enum Command {
        AUTH_CONFIRM,
        AUTH_WRONG,
        DOWNLOAD_FILE,
        RENAME_FILE,
        RENAME_FILE_CONFIRM,
        DELETE_FILE,
        DELETE_FILE_CONFIRM,
        FILE_LIST,
        FAILED
    }

    private Command command;
    private String fileName;

    public CmdMessage(Command command) {
        this.command = command;
    }

    public CmdMessage(Command command, String fileName) {
        this.command = command;
        this.fileName = fileName;
    }

    public Command getCommand() {
        return command;
    }

    public void setCommand(Command command) {
        this.command = command;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
