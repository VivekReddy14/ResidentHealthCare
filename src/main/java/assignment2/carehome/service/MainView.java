package assignment2.carehome.service;

import assignment2.carehome.model.*;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.List;

public class MainView {
    private final CareHomeService service;
    private final Stage stage;
    private BorderPane root;
    private Manager manager;

    public MainView(Stage stage) {
        this.stage = stage;

        // Initialize service with some beds
        this.service = new CareHomeService(List.of(
                new Bed("B1"), new Bed("B2"), new Bed("B3")
        ));

        // Create a default manager for demo
        this.manager = new Manager("M1", "Default Manager", "pw");

        // Add demo staff
        Doctor doc = new Doctor("D1", "Doctor", "pw");
        Nurse nurse = new Nurse("N1", "Nurse", "pw");
        service.addStaff(manager, doc);
        service.addStaff(manager, nurse);

        root = new BorderPane();
        root.setPadding(new Insets(10));
        createUI();
    }

    public void show() {
        stage.setTitle("RMIT Care Home");
        stage.setScene(new Scene(root, 900, 600));
        stage.show();
    }

    private void createUI() {
        // --- Top: Title ---
        Label title = new Label("RMIT CareHome Management");
        title.setFont(new Font(24));
        HBox top = new HBox(title);
        top.setPadding(new Insets(10));
        root.setTop(top);

        // --- Left: Bed Overview ---
        VBox left = new VBox(10);
        left.setPadding(new Insets(10));
        Label leftTitle = new Label("Beds Overview");
        leftTitle.setFont(new Font(16));
        left.getChildren().add(leftTitle);

        for (Bed bed : service.getBeds()) {
            String residentName = bed.getResident() == null ? "vacant" : bed.getResident().getName();
            Button bedBtn = new Button(bed.getBedId() + " - " + residentName);
            bedBtn.setMaxWidth(Double.MAX_VALUE);
            bedBtn.setOnAction(e -> showBedDialog(bed));
            left.getChildren().add(bedBtn);
        }
        root.setLeft(left);

        // --- Right: Manager Controls ---
        VBox right = new VBox(12);
        right.setPadding(new Insets(10));
        Label ctrlTitle = new Label("Manager Controls");
        ctrlTitle.setFont(new Font(16));

        Button addResidentBtn = new Button("Add Resident");
        addResidentBtn.setOnAction(e -> UIHelpers.showAddResidentDialog(service, root));

        Button addStaffBtn = new Button("Add Staff");
        addStaffBtn.setOnAction(e -> UIHelpers.showAddStaffDialog(service, root));

        right.getChildren().addAll(ctrlTitle, addResidentBtn, addStaffBtn);
        root.setRight(right);
    }

    private void showBedDialog(Bed bed) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Bed Details - " + bed.getBedId());
        VBox vb = new VBox(8);
        vb.setPadding(new Insets(10));

        vb.getChildren().add(new Label("Bed ID: " + bed.getBedId()));
        vb.getChildren().add(new Label("Resident: " + (bed.getResident() == null ? "None" : bed.getResident().getName())));

        if (bed.getResident() != null) {
            Button discharge = new Button("Discharge Resident");
            discharge.setOnAction(e -> {
                try {
                    bed.vacate();
                    UIHelpers.alertInfo("Resident discharged from " + bed.getBedId());
                    dialog.close();
                } catch (Exception ex) {
                    UIHelpers.alertError("Error discharging: " + ex.getMessage());
                }
            });
            vb.getChildren().add(discharge);
        } else {
            Button assign = new Button("Assign New Resident");
            assign.setOnAction(e -> {
                UIHelpers.showAddResidentDialog(service, root, bed.getBedId());
                dialog.close();
            });
            vb.getChildren().add(assign);
        }

        dialog.getDialogPane().setContent(vb);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }
}
