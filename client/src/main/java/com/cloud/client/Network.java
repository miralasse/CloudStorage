package com.cloud.client;

import com.cloud.common.CmdMessage;
import com.cloud.common.Message;
import com.cloud.common.Settings;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;

import java.io.IOException;
import java.net.Socket;

public class Network {
    private static Socket socket;
    private static ObjectEncoderOutputStream outputStream;
    private static ObjectDecoderInputStream inputStream;
    private static final String SERVER_IP = "localhost";
    private static final int PORT = 8189;

    public static Socket getSocket() {
        return socket;
    }

    public static void connect() {
        try {
            socket = new Socket(SERVER_IP, PORT);
            inputStream = new ObjectDecoderInputStream(socket.getInputStream(), Settings.MAX_OBJ_SIZE);
            outputStream = new ObjectEncoderOutputStream(socket.getOutputStream(), Settings.MAX_OBJ_SIZE);
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

    public static Message receiveMessage() throws IOException, ClassNotFoundException {
        return (Message) inputStream.readObject();    //блокирующая операция - тред висит и ждет объект
    }

    public static void disconnect() {
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
        Platform.exit();
    }
}
