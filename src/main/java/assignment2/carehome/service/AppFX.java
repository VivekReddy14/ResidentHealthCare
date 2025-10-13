package assignment2.carehome.service;

import javafx.application.Application;
import javafx.stage.Stage;

public class AppFX extends Application {

    @Override
    public void start(Stage primaryStage) {
        MainView mainView = new MainView(primaryStage);
        mainView.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
