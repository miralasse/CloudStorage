package com.cloud.client;

import com.cloud.common.AuthMessage;
import com.cloud.common.CmdMessage;
import javafx.application.Platform;
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

public class LoginController {

    @FXML
    TextField loginField;

    @FXML
    PasswordField passwordField;

    @FXML
    VBox globParent;

    public void auth() throws IOException {
        globParent.getScene().getWindow().hide();
        Stage primaryStage = new Stage();
        Parent root = FXMLLoader.load(getClass().getResource("/main_screen.fxml"));
        primaryStage.setTitle("Cloud Client");
        primaryStage.setScene(new Scene(root, 600, 400));
        primaryStage.show();
    }

    public void sendAuthMsg() {
        if (loginField.getText().isEmpty() || passwordField.getText().isEmpty()) {
            showAlert("Не указан логин или пароль");
            return;
        }
        Network.connect();
        AuthMessage authMessage = new AuthMessage(loginField.getText(), passwordField.getText());
        Network.sendMessage(authMessage);
        CmdMessage msgFromServer = (CmdMessage) Network.receiveMessage();
        if (msgFromServer.getText().equals("AuthOk")) {
            System.out.println("Answer from server: " + msgFromServer.getText());
            try {
                auth();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            showAlert("Неверный логин/пароль");
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
