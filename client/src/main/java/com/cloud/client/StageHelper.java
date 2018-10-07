package com.cloud.client;

import javafx.stage.Stage;

public class StageHelper {
    private static StageHelper ourInstance;
    private Stage stage;

    public static StageHelper getInstance() {
        if (ourInstance == null) {
            ourInstance = new StageHelper();
        }
        return ourInstance;
    }

    private StageHelper() {
    }

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }
}
