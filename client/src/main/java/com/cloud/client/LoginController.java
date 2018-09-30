package com.cloud.client;

import com.cloud.common.AuthMessage;
import com.cloud.common.CmdMessage;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.Socket;

public class LoginController {

    @FXML
    TextField loginField;

    @FXML
    PasswordField passwordField;

    @FXML
    VBox globParent;

    private boolean authorized;

    public void auth() throws IOException {
        globParent.getScene().getWindow().hide();
        Stage primaryStage = new Stage();
        Parent root = FXMLLoader.load(getClass().getResource("/main_screen.fxml"));
        primaryStage.setTitle("Cloud Client");
        primaryStage.setScene(new Scene(root, 600, 400));
        primaryStage.show();
    }

    public void sendAuthMsg(ActionEvent actionEvent) {
        if (loginField.getText().isEmpty() || passwordField.getText().isEmpty()) {
            showAlert("Не указан логин или пароль");
            return;
        }
        ObjectEncoderOutputStream oeos = null;
        ObjectDecoderInputStream odis = null;
        try (Socket socket = new Socket("localhost", 8189)) {
            oeos = new ObjectEncoderOutputStream(socket.getOutputStream());
            AuthMessage authMessage = new AuthMessage(loginField.getText(), passwordField.getText());
            oeos.writeObject(authMessage);
            oeos.flush();
            odis = new ObjectDecoderInputStream(socket.getInputStream());
            CmdMessage msgFromServer = (CmdMessage) odis.readObject();
            if (msgFromServer.getText().equals("AuthOk")) {
                System.out.println("Answer from server: " + msgFromServer.getText());
                auth();
            } else {
                showAlert("Неверный логин/пароль");
            }
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

    public void showAlert(String msg){
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Возникли проблемы");
                alert.setHeaderText(null);
                alert.setContentText(msg);
                alert.showAndWait();
            }
        });
    }
}
