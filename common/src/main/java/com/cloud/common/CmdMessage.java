package com.cloud.common;

public class CmdMessage extends Message {

    public enum Command {
        AUTH_CONFIRM,
        AUTH_WRONG,
        DOWNLOAD_FILE_FROM_SERVER,
        RENAME_FILE,
        DELETE_FILE,
        REFRESH_FILE_LIST;

        Command() {}

        Command(String fileName) {
            this.fileName = fileName;
        }

        Command(String fileName, String newFileName) {
            this.fileName = fileName;
            this.newFileName = newFileName;
        }

        private String fileName;
        private String newFileName;

        public String getFileName() {
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

    private Command command;

    public CmdMessage(Command command) {
        this.command = command;
    }

    public Command getCommand() {
        return command;
    }

    public void setCommand(Command command) {
        this.command = command;
    }
}
