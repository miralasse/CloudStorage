package com.cloud.client;

import com.cloud.common.CmdMessage;
import com.cloud.common.FileMessage;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;


import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MainController {

    public void testFileSend(ActionEvent actionEvent) {
        try {
            FileMessage fileMessage = new FileMessage();
            Path file = Paths.get("testFile.txt");
            fileMessage.setFileName(file.getFileName().toString());
            fileMessage.setFileSize(file.toFile().length());
            fileMessage.setContent(Files.readAllBytes(file));
            Network.sendMessage(fileMessage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void testCmd(ActionEvent actionEvent) {
        try {
            CmdMessage cmdMessage = new CmdMessage("Hello Server!!!");
            Network.sendMessage(cmdMessage);

            CmdMessage msgFromServer = (CmdMessage) Network.receiveMessage();
            System.out.println("Answer from server: " + msgFromServer.getText());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
