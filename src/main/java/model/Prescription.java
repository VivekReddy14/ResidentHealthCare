
package model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

public class Prescription implements Serializable {
    private final String id; private final String doctorId; private final LocalDateTime createdAt;
    private final List<MedicationOrder> orders = new ArrayList<>();
    public Prescription(String id, String doctorId, LocalDateTime createdAt){
        this.id=id; this.doctorId=doctorId; this.createdAt=createdAt;
    }
    public String getId(){ return id; }
    public String getDoctorId(){ return doctorId; }
    public LocalDateTime getCreatedAt(){ return createdAt; }
    public List<MedicationOrder> getOrders(){ return orders; }
    public void addOrder(MedicationOrder o){ orders.add(o); }
}
