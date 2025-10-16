
package service;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

public class AppFX extends Application {
    private final CareHomeService svc = CareHomeService.get();

    @Override
    public void start(Stage stage){
        showLogin(stage);
    }

    public void showLogin(Stage stage){
        BorderPane root = new BorderPane();
        VBox box = new VBox(10); box.setPadding(new Insets(20)); box.setAlignment(Pos.CENTER);
        TextField user = new TextField(); user.setPromptText("Username");
        PasswordField pass = new PasswordField(); pass.setPromptText("Password");
        Button login = new Button("Login"); Label msg = new Label();
        box.getChildren().addAll(new Label("RMIT Care Home Login"), user, pass, login, msg);
        root.setCenter(box);

        login.setOnAction(e -> {
            try {
                svc.login(user.getText(), pass.getText());
                // ⬇️ Load persisted data here before showing MainView
                try { svc.load(); } catch (Exception loadEx) { System.out.println("No previous data found or failed to load: " + loadEx.getMessage()); }
                
                new MainView().show(stage);
            } catch (Exception ex) {
                msg.setText("Login failed: " + ex.getMessage());
            }
        });

        // seed a nurse and doctor on first run with shifts (and a copy of manager if needed)
        if (svc.getStaff().size() <= 1){
            try {
                // temporary elevate to allow seeding
                Session.get().setCurrentUser(new Manager("seed","seed","seed"));
                Nurse n = svc.createNurse("nurse", "password");
                Doctor d = svc.createDoctor("doctor", "password");
                Manager m = svc.createManager("manager2","password");
                // assign shifts: nurses 08-16 and 14-22; doctors 1h per day
                for (DayOfWeek dw: DayOfWeek.values()){
                    svc.assignShift(n.getId(), new Shift(dw, LocalTime.of(8,0), LocalTime.of(16,0)));
                    svc.assignShift(n.getId(), new Shift(dw, LocalTime.of(14,0), LocalTime.of(22,0)));
                    svc.assignShift(d.getId(), new Shift(dw, LocalTime.of(9,0), LocalTime.of(10,0)));
                }
                Session.get().setCurrentUser(null);
            } catch (Exception ignored){}
        }

        stage.setTitle("Login - RMIT Care Home");
        stage.setScene(new Scene(root, 420, 300));
        stage.show();
    }

    public static void main(String[] args){ launch(args); }
}
