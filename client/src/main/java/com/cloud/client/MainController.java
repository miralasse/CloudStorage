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
            ObjectEncoderOutputStream oeos = null;
            try (Socket socket = new Socket("localhost", 8189)) {
                oeos = new ObjectEncoderOutputStream(socket.getOutputStream());
                FileMessage fileMessage = new FileMessage();
                Path file = Paths.get("testFile.txt");
                fileMessage.setFileName(file.getFileName().toString());
                fileMessage.setFileSize(file.toFile().length());
                fileMessage.setContent(Files.readAllBytes(file));
                oeos.writeObject(fileMessage);
                oeos.flush();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    oeos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
    }

    public void testCmd(ActionEvent actionEvent) {
        ObjectEncoderOutputStream oeos = null;
        ObjectDecoderInputStream odis = null;
        try (Socket socket = new Socket("localhost", 8189)) {
            oeos = new ObjectEncoderOutputStream(socket.getOutputStream());
            CmdMessage cmdMessage = new CmdMessage("Hello Server!!!");
            oeos.writeObject(cmdMessage);
            oeos.flush();
            odis = new ObjectDecoderInputStream(socket.getInputStream());
            CmdMessage msgFromServer = (CmdMessage) odis.readObject();
            System.out.println("Answer from server: " + msgFromServer.getText());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                oeos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                odis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
