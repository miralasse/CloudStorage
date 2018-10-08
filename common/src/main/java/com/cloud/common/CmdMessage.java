package com.cloud.common;

import java.io.Serializable;

public class CmdMessage extends Message {

    public enum Command {
        AUTH_CONFIRM,
        AUTH_WRONG,
        DOWNLOAD_FILE_FROM_SERVER,
        RENAME_FILE,
        DELETE_FILE,
        REFRESH_FILE_LIST;
    }

    private Command command;
    private String fileName;
    private String newFileName;

    public CmdMessage(Command command) {
        this.command = command;
    }

    public Command getCommand() {
        return command;
    }

    public void setCommand(Command command) {
        this.command = command;
    }

    public String getFileName() {
        System.out.println("Вывод при вызове метода getFileName" + fileName);
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getNewFileName() {
        return newFileName;
    }

    public void setNewFileName(String newFileName) {
        this.newFileName = newFileName;
    }
}
