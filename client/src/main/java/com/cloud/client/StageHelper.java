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

    Stage getStage() {
        return stage;
    }

    void setStage(Stage stage) {
        this.stage = stage;
    }
}
