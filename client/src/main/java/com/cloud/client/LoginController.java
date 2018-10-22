package com.cloud.client;

import com.cloud.common.AuthMessage;
import com.cloud.common.CmdMessage;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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

    public void changeStage() throws IOException {
        globParent.getScene().getWindow().hide();
        Stage primaryStage = new Stage();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main_screen.fxml"));
        Parent root = loader.load();
        MainController mainController = loader.getController();
        Network.setMainController(mainController);

        primaryStage.setTitle("Cloud Client");
        primaryStage.setScene(new Scene(root, 800, 400));
        primaryStage.show();
        StageHelper.setStage(primaryStage);
    }

    public void authorize() {
        if (loginField.getText().isEmpty() || passwordField.getText().isEmpty()) {
            StageHelper.showAlert("Login or password missing");
            return;
        }
        if (Network.getSocket() == null || Network.getSocket().isClosed()) {
            Network.connect();
        }
        String login = loginField.getText().split("\\s")[0];    //protection from entering a few words instead of one
        String password = passwordField.getText().split("\\s")[0];
        System.out.println(login);
        System.out.println(password);
        AuthMessage authMessage = new AuthMessage(login, password);
        Network.sendMessage(authMessage);
        CmdMessage msgFromServer = (CmdMessage) Network.receiveMessage();
        if (msgFromServer.getCommand() == CmdMessage.Command.AUTH_CONFIRM) {
            System.out.println("Answer from server: " + msgFromServer.getCommand());
            try {
                changeStage();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (msgFromServer.getCommand() == CmdMessage.Command.AUTH_WRONG){
            StageHelper.showAlert("Sorry, wrong login or password. Please, try again");
        } else {
            StageHelper.showAlert("Something went wrong. Please, try again.");
        }
    }



}
