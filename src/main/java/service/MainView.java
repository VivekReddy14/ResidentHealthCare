package service;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import model.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class MainView {
    private final CareHomeService svc = CareHomeService.get();
    private final int[] ROOM_LAYOUT = {1, 2, 4, 4, 4, 4}; // total 19 beds per ward

    public void show(Stage stage){
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // Top bar
        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);
        Label userLbl = new Label("Logged in: " + Session.get().getCurrentUser().getUsername() +
                " ("+Session.get().getCurrentUser().getRole()+")");
        Button btnSave = new Button("Save");
        btnSave.setOnAction(e->{ try { svc.save(); UIHelpers.info("Saved","Data serialized."); } catch(Exception ex){ UIHelpers.info("Error", ex.getMessage()); } });
        Button btnCompliance = new Button("Check Compliance");
        btnCompliance.setOnAction(e->{ try { svc.checkCompliance(); UIHelpers.info("OK","All staff rosters comply."); } catch (Exception ex){ UIHelpers.info("Compliance Failed", ex.getMessage()); }});
        Button btnLogout = new Button("Logout");
        btnLogout.setOnAction(e->{ 
            try { svc.save(); } 
            catch (Exception ex) { UIHelpers.info("Error", "Failed to save: " + ex.getMessage()); }
            Session.get().setCurrentUser(null); 
            new AppFX().showLogin(stage); 
        });
        top.getChildren().addAll(userLbl, btnSave, btnCompliance, btnLogout);

        // Center wards
        HBox wardsPane = new HBox(20);
        wardsPane.setPadding(new Insets(10));
        for (Ward w: svc.getWards()){
            VBox wardBox = new VBox(8);
            wardBox.setPadding(new Insets(8));
            wardBox.setStyle("-fx-border-color: #8aa; -fx-border-radius: 4; -fx-padding: 8;");
            Label title = new Label(w.getId());
            title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);

            int bedIndex = 0;
            int roomIdx = 0;
            for (int r=0; r<3; r++){
                for (int c=0; c<2; c++){
                    if (roomIdx >= ROOM_LAYOUT.length) break;
                    int bedsInRoom = ROOM_LAYOUT[roomIdx];
                    roomIdx++;

                    VBox room = new VBox(5);
                    room.setAlignment(Pos.CENTER);
                    room.setPadding(new Insets(6));
                    room.setStyle("-fx-border-color: #ccd; -fx-background-color: #f9fbff;");
                    GridPane roomBeds = new GridPane();
                    roomBeds.setHgap(5);
                    roomBeds.setVgap(5);

                    for (int i=0; i<bedsInRoom; i++){
                        if (bedIndex >= w.getBeds().size()) break;
                        Bed bed = w.getBeds().get(bedIndex++);
                        Button bedBtn = bedButton(bed);
                        int col = (bedsInRoom == 1) ? 0 : i % 2;
                        int row = (bedsInRoom == 1) ? 0 : i / 2;
                        roomBeds.add(bedBtn, col, row);
                    }

                    room.getChildren().add(roomBeds);
                    grid.add(room, c, r);
                }
            }
            wardBox.getChildren().addAll(title, grid);
            wardsPane.getChildren().add(wardBox);
        }

        // Right panel (Actions + Legend)
        VBox rightPanel = new VBox(20);
        rightPanel.setPadding(new Insets(10));
        rightPanel.setStyle("-fx-background-color: #f3f6ff; -fx-border-color: #dde;");

        VBox actions = new VBox(10);
        actions.getChildren().add(new Label("Actions"));
        Staff u = Session.get().getCurrentUser();
        if (u.getRole()==Role.MANAGER){
            Button addResident = new Button("Add Resident & Auto-Allocate");
            addResident.setOnAction(e-> { addResidentFlow(stage); });
            Button discharge = new Button("Discharge Resident");
            discharge.setOnAction(e->{ dischargeFlow(stage); });
            Button addStaff = new Button("Add Staff");
            addStaff.setOnAction(e-> addStaffFlow());
            Button modPass = new Button("Modify Staff Password");
            modPass.setOnAction(e-> modifyStaffPasswordFlow());
            Button modShift = new Button("Modify Staff Shift");
            modShift.setOnAction(e-> modifyStaffShiftFlow());
            actions.getChildren().addAll(addResident, discharge, addStaff, modPass, modShift);
            Button viewShiftsBtn = new Button("View Shift Allotments");
            viewShiftsBtn.setOnAction(e -> showShiftAllotmentsTable());

            // Clear data button
            Button clearData = new Button("Clear All Data");
            clearData.setOnAction(e -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Clear All Data");
                confirm.setHeaderText("Are you sure?");
                confirm.setContentText("This will remove all residents and records. Default manager/doctor/nurse will remain.");
                confirm.showAndWait().ifPresent(bt -> {
                    if (bt == ButtonType.OK) {
                        svc.clearAllData(); // assumed implemented in service
                        UIHelpers.info("Cleared", "All data has been reset.");
                        refresh(stage);
                    }
                });
            });
            actions.getChildren().add(clearData);
            actions.getChildren().add(viewShiftsBtn);

        } else if (u.getRole()==Role.DOCTOR){
            Button addRx = new Button("Add Prescription");
            Button viewShiftsBtn = new Button("View Shift Allotments");
            viewShiftsBtn.setOnAction(e -> showShiftAllotmentsTable());

            addRx.setOnAction(e-> addPrescriptionFlow());
            actions.getChildren().addAll(addRx);
            actions.getChildren().add(viewShiftsBtn);
        } else if (u.getRole()==Role.NURSE){
            Button move = new Button("Move Resident");
            move.setOnAction(e->{ moveResidentFlow(stage); });
            Button admin = new Button("Administer Medication");
            admin.setOnAction(e-> administerFlow());
            Button viewShiftsBtn = new Button("View Shift Allotments");
            viewShiftsBtn.setOnAction(e -> showShiftAllotmentsTable());

            actions.getChildren().addAll(move, admin);
            actions.getChildren().add(viewShiftsBtn);
        }

        VBox legend = new VBox(5);
        legend.getChildren().add(new Label("Legend:"));
        legend.getChildren().add(legendItem("Male", Color.CORNFLOWERBLUE));
        legend.getChildren().add(legendItem("Female", Color.SALMON));
        legend.getChildren().add(legendItem("Vacant", Color.WHITE));
        legend.setPadding(new Insets(10));
        legend.setStyle("-fx-border-color: #bbb; -fx-background-color: #fff;");

        rightPanel.getChildren().addAll(actions, legend);

        root.setTop(top);
        root.setCenter(wardsPane);
        root.setRight(rightPanel);

        stage.setTitle("RMIT Care Home");
        stage.setScene(new Scene(root, 1200, 750));
        stage.show();
    }
    
    private void showShiftAllotmentsTable() {
        Stage stage = new Stage();
        stage.setTitle("Shift Allotments");

        TableView<ShiftRow> table = new TableView<>();

        TableColumn<ShiftRow, String> staffCol = new TableColumn<>("Staff");
        staffCol.setCellValueFactory(data -> data.getValue().staffNameProperty());

        TableColumn<ShiftRow, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(data -> data.getValue().roleProperty());

        TableColumn<ShiftRow, String> dayCol = new TableColumn<>("Day");
        dayCol.setCellValueFactory(data -> data.getValue().dayProperty());

        TableColumn<ShiftRow, String> startCol = new TableColumn<>("Start Time");
        startCol.setCellValueFactory(data -> data.getValue().startProperty());

        TableColumn<ShiftRow, String> endCol = new TableColumn<>("End Time");
        endCol.setCellValueFactory(data -> data.getValue().endProperty());

        table.getColumns().addAll(staffCol, roleCol, dayCol, startCol, endCol);

        // Populate rows from service
        CareHomeService svc = CareHomeService.get();
        for (Staff s : svc.getStaff()) {
            for (Shift sh : s.getShifts()) {
                table.getItems().add(new ShiftRow(s.getUsername(), s.getRole().name(),
                        sh.getDay().toString(),
                        sh.getStart().toString(),
                        sh.getEnd().toString()));
            }
        }

        VBox root = new VBox(10, table);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 600, 400);
        stage.setScene(scene);
        stage.show();
    }

    public static class ShiftRow {
        private final StringProperty staffName;
        private final StringProperty role;
        private final StringProperty day;
        private final StringProperty start;
        private final StringProperty end;

        public ShiftRow(String staffName, String role, String day, String start, String end) {
            this.staffName = new SimpleStringProperty(staffName);
            this.role = new SimpleStringProperty(role);
            this.day = new SimpleStringProperty(day);
            this.start = new SimpleStringProperty(start);
            this.end = new SimpleStringProperty(end);
        }

        public StringProperty staffNameProperty() { return staffName; }
        public StringProperty roleProperty() { return role; }
        public StringProperty dayProperty() { return day; }
        public StringProperty startProperty() { return start; }
        public StringProperty endProperty() { return end; }
    }


    private HBox legendItem(String label, Color color){
        Region box = new Region();
        box.setMinSize(20, 20);
        box.setBackground(new Background(new BackgroundFill(color, new CornerRadii(3), Insets.EMPTY)));
        Label lbl = new Label(label);
        HBox h = new HBox(5, box, lbl);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private Button bedButton(Bed bed){
        Button b = new Button();
        b.setMinSize(70, 50);
        b.setMaxSize(70, 50);
        updateBedButton(bed, b);
        b.setOnAction(e->{
            if (bed.getResident()==null){ UIHelpers.info("Bed "+bed.getId(), "Vacant"); }
            else {
                Resident r = bed.getResident();
                String details = "ID: "+r.getId()+"\nName: "+r.getName()+"\nGender: "+r.getGender()+
                        "\nPrescriptions: "+r.getPrescriptions().size()+"\nAdministrations: "+r.getAdministrations().size();
                UIHelpers.info("Bed "+bed.getId(), details);
            }
        });
        return b;
    }

    private void updateBedButton(Bed bed, Button b){
        if (bed.getResident()==null){
            b.setText(bed.getId());
            b.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(4), Insets.EMPTY)));
        } else {
            Color color = bed.getResident().getGender()==Gender.MALE ? Color.CORNFLOWERBLUE : Color.SALMON;
            b.setText(bed.getId()+"\n"+bed.getResident().getId());
            b.setBackground(new Background(new BackgroundFill(color.deriveColor(0,1,1,0.65),
                    new CornerRadii(4), Insets.EMPTY)));
        }
    }

    private void refresh(Stage stage){
        new MainView().show(stage);
    }

    // ==== Manager actions ====

    private void addResidentFlow(Stage stage){
        Dialog<Resident> d = new Dialog<>();
        d.setTitle("Add Resident");
        TextField name = new TextField();
        ChoiceBox<Gender> gender = new ChoiceBox<>();
        gender.getItems().addAll(Gender.MALE, Gender.FEMALE);
        gender.getSelectionModel().selectFirst();
        CheckBox isolation = new CheckBox("Needs Isolation");
        VBox v = new VBox(8, new Label("Name"), name, new Label("Gender"), gender, isolation);
        v.setPadding(new Insets(10));
        d.getDialogPane().setContent(v);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.setResultConverter(bt -> bt==ButtonType.OK ? new Resident("tmp","tmp", gender.getValue(), isolation.isSelected()) : null);
        Resident tmp = d.showAndWait().orElse(null);
        if (tmp==null) return;

        try {
            Resident r = svc.addResident(name.getText(), gender.getValue(), isolation.isSelected());
            // First attempt: same-gender rooms only
            try {
                svc.allocateResidentToBedWithGender(r, false);
                UIHelpers.info("Success","Resident added & allocated.");
                refresh(stage);
                return;
            } catch (Exception ex) {
                // No same-gender rooms available; ask manager to confirm fallback allocation
                String gtxt = (r.getGender()==Gender.FEMALE) ? "female" : "male";
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("No "+gtxt+"-only room available");
                confirm.setHeaderText("No "+gtxt+"-only rooms with vacancy were found.");
                confirm.setContentText("Do you want to allocate this resident to another suitable room (mixed or empty)?");
                ButtonType yes = new ButtonType("Yes", ButtonBar.ButtonData.YES);
                ButtonType no = new ButtonType("No", ButtonBar.ButtonData.NO);
                confirm.getButtonTypes().setAll(yes, no);
                ButtonType res = confirm.showAndWait().orElse(no);
                if (res == yes) {
                    svc.allocateResidentToBedWithGender(r, true);
                    UIHelpers.info("Allocated","Resident allocated to the next suitable room.");
                    refresh(stage);
                } else {
                    UIHelpers.info("Cancelled","Resident was created but not allocated to a bed.");
                }
            }
        } catch (Exception ex){ 
            UIHelpers.info("Error", ex.getMessage()); 
        }
    }

    private void dischargeFlow(Stage stage){
        TextInputDialog d = new TextInputDialog();
        d.setTitle("Discharge");
        d.setHeaderText("Enter Resident ID to discharge");
        d.setContentText("Resident ID:");
        d.showAndWait().ifPresent(id->{ 
            try { 
                svc.discharge(id); 
                UIHelpers.info("Discharged","Resident archived & removed."); 
                refresh(stage);
            } catch (Exception ex){ UIHelpers.info("Error", ex.getMessage()); } 
        });
    }

    private void addStaffFlow(){
        Dialog<String[]> d = new Dialog<>();
        d.setTitle("Add Staff");
        ChoiceBox<Role> role = new ChoiceBox<>();
        role.getItems().addAll(Role.DOCTOR, Role.NURSE, Role.MANAGER);
        role.getSelectionModel().selectFirst();
        TextField user = new TextField();
        PasswordField pass = new PasswordField();
        VBox v = new VBox(8, new Label("Role"), role, new Label("Username"), user, new Label("Password"), pass);
        v.setPadding(new Insets(10));
        d.getDialogPane().setContent(v);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.setResultConverter(bt -> bt==ButtonType.OK ? new String[]{role.getValue().name(), user.getText(), pass.getText()} : null);
        String[] res = d.showAndWait().orElse(null);
        if (res==null) return;
        try {
            switch (Role.valueOf(res[0])){
                case DOCTOR -> svc.createDoctor(res[1], res[2]);
                case NURSE -> svc.createNurse(res[1], res[2]);
                case MANAGER -> svc.createManager(res[1], res[2]);
            }
            UIHelpers.info("Success","Staff added.");
        } catch (Exception ex){ UIHelpers.info("Error", ex.getMessage()); }
    }

    private void modifyStaffPasswordFlow(){
        Dialog<String[]> d = new Dialog<>();
        d.setTitle("Modify Staff Password");

        ComboBox<Staff> staffBox = new ComboBox<>();
        staffBox.getItems().addAll(svc.getStaff());
        staffBox.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Staff s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty || s == null ? "" : s.getId()+" - "+s.getUsername()+" ("+s.getRole()+")");
            }
        });
        staffBox.setButtonCell(staffBox.getCellFactory().call(null));

        PasswordField newPass = new PasswordField();
        newPass.setPromptText("New Password");

        VBox v = new VBox(8,
                new Label("Select Staff:"), staffBox,
                new Label("New Password:"), newPass
        );
        v.setPadding(new Insets(10));
        d.getDialogPane().setContent(v);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        d.setResultConverter(bt -> {
            Staff s = staffBox.getValue();
            return (bt == ButtonType.OK && s != null)
                    ? new String[]{s.getId(), newPass.getText()}
                    : null;
        });

        String[] res = d.showAndWait().orElse(null);
        if (res == null) return;

        try {
            svc.updateStaffPassword(res[0], res[1]);
            UIHelpers.info("Updated", "Password updated successfully.");
        } catch (Exception ex) {
            UIHelpers.info("Error", ex.getMessage());
        }
    }

    private void modifyStaffShiftFlow(){
        Dialog<String[]> d = new Dialog<>();
        d.setTitle("Assign / Modify Staff Shift");

        ComboBox<Staff> staffBox = new ComboBox<>();
        staffBox.getItems().addAll(svc.getStaff());
        staffBox.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Staff s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty || s == null ? "" : s.getId()+" - "+s.getUsername()+" ("+s.getRole()+")");
            }
        });
        staffBox.setButtonCell(staffBox.getCellFactory().call(null));

        ChoiceBox<DayOfWeek> day = new ChoiceBox<>();
        day.getItems().addAll(DayOfWeek.values());
        day.getSelectionModel().selectFirst();

        TextField start = new TextField("08:00");
        TextField end = new TextField("16:00");

        VBox v = new VBox(8,
                new Label("Select Staff:"), staffBox,
                new Label("Day"), day,
                UIHelpers.spaced(new Label("Start"), start, new Label("End"), end)
        );
        v.setPadding(new Insets(10));
        d.getDialogPane().setContent(v);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        d.setResultConverter(bt -> {
            Staff s = staffBox.getValue();
            return (bt == ButtonType.OK && s != null)
                    ? new String[]{s.getId(), day.getValue().name(), start.getText(), end.getText()}
                    : null;
        });

        String[] res = d.showAndWait().orElse(null);
        if (res == null) return;

        try {
            Shift s = new Shift(DayOfWeek.valueOf(res[1]), LocalTime.parse(res[2]), LocalTime.parse(res[3]));
            svc.assignShift(res[0], s);
            UIHelpers.info("Updated", "Shift assigned/modified.");
        } catch (Exception ex) {
            UIHelpers.info("Error", ex.getMessage());
        }
    }

    // ==== Doctor & Nurse actions ====

    private void addPrescriptionFlow(){
        Dialog<Resident> d = new Dialog<>();
        d.setTitle("Add Prescription");
        ComboBox<Resident> resBox = residentCombo();
        VBox v = new VBox(8, new Label("Select Resident:"), resBox);
        v.setPadding(new Insets(10));
        d.getDialogPane().setContent(v);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.setResultConverter(bt -> bt==ButtonType.OK ? resBox.getValue() : null);
        Resident res = d.showAndWait().orElse(null);
        if (res==null) return;

        try {
            Prescription p = svc.addPrescription(res.getId());
            TextInputDialog med = new TextInputDialog("Paracetamol");
            med.setHeaderText("Medicine");
            TextInputDialog dose = new TextInputDialog("500mg");
            dose.setHeaderText("Dose");
            TextInputDialog time = new TextInputDialog("09:00");
            time.setHeaderText("Time (HH:mm)");
            if (med.showAndWait().isEmpty() || dose.showAndWait().isEmpty() || time.showAndWait().isEmpty()) return;
            svc.addMedicationOrder(res.getId(), p.getId(), med.getResult(), dose.getResult(), LocalTime.parse(time.getResult()));
            UIHelpers.info("Success","Prescription and medication order added.");
        } catch (Exception ex){ UIHelpers.info("Error", ex.getMessage()); }
    }

    private void administerFlow(){
        Dialog<String[]> d = new Dialog<>();
        d.setTitle("Administer Medication");
        ComboBox<Resident> resBox = residentCombo();
        TextField med = new TextField();
        TextField dose = new TextField();
        VBox v = new VBox(8, new Label("Select Resident"), resBox,
                new Label("Medicine"), med,
                new Label("Dose"), dose);
        v.setPadding(new Insets(10));
        d.getDialogPane().setContent(v);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.setResultConverter(bt -> {
            Resident r = resBox.getValue();
            return bt==ButtonType.OK && r!=null ? new String[]{r.getId(), med.getText(), dose.getText()} : null;
        });
        String[] res = d.showAndWait().orElse(null);
        if (res==null) return;
        try { svc.administer(res[0], res[1], res[2]); UIHelpers.info("Recorded","Administration recorded."); }
        catch (Exception ex){ UIHelpers.info("Error", ex.getMessage()); }
    }

    private ComboBox<Resident> residentCombo(){
        List<Resident> residents = new java.util.ArrayList<>(svc.getResidents());
        ComboBox<Resident> box = new ComboBox<>();
        box.getItems().addAll(residents);
        box.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Resident r, boolean empty) {
                super.updateItem(r, empty);
                setText(empty || r == null ? "" : r.getId()+" - "+r.getName());
            }
        });
        box.setButtonCell(box.getCellFactory().call(null));
        return box;
    }

    private void moveResidentFlow(Stage stage){
        Dialog<String[]> d = new Dialog<>();
        d.setTitle("Move Resident");
        TextField from = new TextField(); from.setPromptText("From Bed ID");
        TextField to = new TextField(); to.setPromptText("To Bed ID");
        VBox v = new VBox(8, new Label("From Bed ID"), from, new Label("To Bed ID"), to);
        v.setPadding(new Insets(10));
        d.getDialogPane().setContent(v);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.setResultConverter(bt -> bt==ButtonType.OK ? new String[]{from.getText(), to.getText()} : null);
        String[] res = d.showAndWait().orElse(null);
        if (res==null) return;
        try { 
            svc.moveResident(res[0], res[1]); 
            UIHelpers.info("Moved","Resident moved."); 
            refresh(stage);
        }
        catch (Exception ex){ UIHelpers.info("Error", ex.getMessage()); }
    }
}
