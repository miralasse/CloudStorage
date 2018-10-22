package com.cloud.client;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class StageHelper {
    private static Stage stage;

    public static Stage getStage() {
        return stage;
    }

    public static void setStage(Stage stage) {
        StageHelper.stage = stage;
    }

    public static void showAlert(String msg) {
        final Runnable showAlertRunnable = () -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Warning");
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        };
        if (Platform.isFxApplicationThread()) {
            showAlertRunnable.run();
        } else {
            Platform.runLater(showAlertRunnable);
        }
    }
}
