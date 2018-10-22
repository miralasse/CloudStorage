package com.cloud.client;

import com.cloud.common.CmdMessage;
import com.cloud.common.Message;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import java.io.IOException;
import java.net.Socket;

public class Network {
    private static Socket socket;
    private static ObjectEncoderOutputStream outputStream;
    private static ObjectDecoderInputStream inputStream;
    private static final String SERVER_IP = "localhost";
    private static final int PORT = 8189;

    private static MainController mainController;

    public static Socket getSocket() {
        return socket;
    }

    public static void setMainController(MainController mainController) {
        Network.mainController = mainController;
    }

    public static void connect() {
        try {
            socket = new Socket(SERVER_IP, PORT);
            inputStream = new ObjectDecoderInputStream(socket.getInputStream());
            outputStream = new ObjectEncoderOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendMessage(Message msg) {
        try {
            outputStream.writeObject(msg);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Message receiveMessage() {
        Message msgFromServer = null;
        try {
            msgFromServer = (Message) inputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return msgFromServer;
    }

    public static void disconnect() {
        mainController.setClosedByClient(true);
        if (socket != null && !socket.isClosed()) {
            sendMessage(new CmdMessage(CmdMessage.Command.CLIENT_EXIT));
        }
        try {
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
