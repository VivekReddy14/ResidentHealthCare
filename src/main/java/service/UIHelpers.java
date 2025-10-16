
package service;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.layout.HBox;

public class UIHelpers {
    public static void info(String title, String msg){
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
    public static HBox spaced(Node... nodes){
        HBox h = new HBox(8, nodes); h.setPadding(new Insets(8)); return h;
    }
}
