package com.cloud.client;

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

    public static void connect() {
        try {
            socket = new Socket(SERVER_IP, PORT);
            inputStream = new ObjectDecoderInputStream(socket.getInputStream());
            outputStream = new ObjectEncoderOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static boolean sendMessage(Message msg) {
        try {
            outputStream.writeObject(msg);
            outputStream.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
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
            if (!(socket == null || socket.isClosed())) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
