package assignment2.carehome.service;

import assignment2.carehome.model.*;
import assignment2.carehome.exception.*;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;

import java.util.List;

public class UIHelpers {

    public static void showAddResidentDialog(CareHomeService svc, Node owner) {
        showAddResidentDialog(svc, owner, null);
    }

    public static void showAddResidentDialog(CareHomeService svc, Node owner, String preferredBedId) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Add Resident");
        GridPane gp = new GridPane();
        gp.setHgap(8);
        gp.setVgap(8);
        gp.setPadding(new Insets(8));

        TextField name = new TextField();
        ComboBox<String> gender = new ComboBox<>();
        gender.getItems().addAll("M", "F");

        ComboBox<String> beds = new ComboBox<>();
        List<Bed> vacantBeds = svc.getVacantBeds();
        for (Bed b : vacantBeds) beds.getItems().add(b.getBedId());
        if (preferredBedId != null) beds.getSelectionModel().select(preferredBedId);

        gp.addRow(0, new Label("Name:"), name);
        gp.addRow(1, new Label("Gender:"), gender);
        gp.addRow(2, new Label("Bed ID:"), beds);

        dialog.getDialogPane().setContent(gp);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                String n = name.getText().trim();
                String g = gender.getValue();
                String bid = beds.getValue();
                if (n.isEmpty() || g == null || bid == null) {
                    alertError("Please fill all fields");
                    return null;
                }
                try {
                	Gender gen = g.equals("M") ? Gender.M : Gender.F;
                	Resident r = new Resident(IdUtil.nextId("R"), n, gen); // your Resident constructor
                    Manager manager = svc.getManager(); // <-- you need a method to get a manager
                    svc.addResidentToBed(manager, r, bid);
                    alertInfo("Resident assigned to bed " + bid);
                } catch (Exception e) {
                    alertError("Failed: " + e.getMessage());
                }
            }
            return null;
        });
        dialog.showAndWait();
    }



    public static void showAddStaffDialog(CareHomeService svc, Node owner) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Add Staff (Manager Only)");

        GridPane gp = new GridPane();
        gp.setHgap(8);
        gp.setVgap(8);
        gp.setPadding(new Insets(8));

        TextField name = new TextField();
        ComboBox<String> role = new ComboBox<>();
        role.getItems().addAll("Manager", "Doctor", "Nurse");

        gp.addRow(0, new Label("Name:"), name);
        gp.addRow(1, new Label("Role:"), role);

        dialog.getDialogPane().setContent(gp);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                String n = name.getText().trim();
                String r = role.getValue();
                if (n.isEmpty() || r == null) {
                    alertError("Please fill all fields");
                    return null;
                }
                Staff s;
                switch (r) {
                    case "Doctor":
                        s = new Doctor(IdUtil.nextId("D"), n, "password");
                        break;
                    case "Nurse":
                        s = new Nurse(IdUtil.nextId("N"), n, "password");
                        break;
                    default:
                        s = new Manager(IdUtil.nextId("M"), n, "password");
                }
                try {
                    svc.addStaff(svc.getManager(), s);
                    alertInfo("Added staff: " + s.getName());
                } catch (Exception e) {
                    alertError("Failed: " + e.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    public static void alertInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        a.showAndWait();
    }

    public static void alertError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        a.showAndWait();
    }
}
