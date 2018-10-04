package com.cloud.client;

import com.cloud.common.CmdMessage;
import com.cloud.common.FileMessage;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;


import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MainController {

    public void uploadFile() {
        try {
            FileMessage fileMessage = new FileMessage();
            Path file = Paths.get("testFile.txt");
            fileMessage.setFileName(file.getFileName().toString());
            fileMessage.setFileSize(file.toFile().length());
            fileMessage.setContent(Files.readAllBytes(file));
            Network.sendMessage(fileMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void downloadFile() {
        CmdMessage cmdMessage = new CmdMessage(CmdMessage.Command.DOWNLOAD_FILE_FROM_SERVER);
        cmdMessage.getCommand().setFileName("newtestFile.txt");
        Network.sendMessage(cmdMessage);

//            FileMessage fileFromServer = (FileMessage) Network.receiveMessage();
//            String fileName = "fromServer" + fileFromServer.getFileName();
//            Path filePath = Paths.get(fileName);
//            System.out.println("Received file: " + filePath.toString());
//            byte[] content = fileFromServer.getContent();
//            Files.write(filePath, content);
//            System.out.println("File is ready");
    }

    public void deleteFile() {
        CmdMessage cmdMessage = new CmdMessage(CmdMessage.Command.DELETE_FILE);
        cmdMessage.getCommand().setFileName("deleteFile.txt");
        Network.sendMessage(cmdMessage);
    }

    public void renameFile() {
        CmdMessage cmdMessage = new CmdMessage(CmdMessage.Command.RENAME_FILE);
        cmdMessage.getCommand().setFileName("renameFile.txt");
        cmdMessage.getCommand().setNewFileName("newNameFile.txt");
        Network.sendMessage(cmdMessage);
    }

    public void refreshFileList() {
        CmdMessage cmdMessage = new CmdMessage(CmdMessage.Command.REFRESH_FILE_LIST);
        Network.sendMessage(cmdMessage);
    }
}
