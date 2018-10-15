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
        primaryStage.setScene(new Scene(root, 800, 400));
        primaryStage.show();
        StageHelper.setStage(primaryStage);
    }

    public void sendAuthMsg() {
        if (loginField.getText().isEmpty() || passwordField.getText().isEmpty()) {
            StageHelper.showAlert("Не указан логин или пароль");
            return;
        }
        if (Network.getSocket() == null || Network.getSocket().isClosed()) {
            Network.connect();
        }
        String login = loginField.getText().split("\\s")[0];    //защита от ввода нескольких слов
        String password = passwordField.getText().split("\\s")[0];
        System.out.println(login);
        System.out.println(password);
        AuthMessage authMessage = new AuthMessage(login, password);
        Network.sendMessage(authMessage);
        CmdMessage msgFromServer = (CmdMessage) Network.receiveMessage();
        if (msgFromServer.getCommand() == CmdMessage.Command.AUTH_CONFIRM) {
            System.out.println("Answer from server: " + msgFromServer.getCommand());
            try {
                auth();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (msgFromServer.getCommand() == CmdMessage.Command.AUTH_WRONG){
            StageHelper.showAlert("Неверный логин/пароль");
        } else {
            StageHelper.showAlert("Произошла ошибка при авторизации");
        }
    }


}
