package assignment2.carehome.model;

import java.time.LocalTime;
import java.util.*;

public class Prescription {
    private final String id;
    private final String residentId;
    private final String doctorId;
    private final List<MedicationOrder> orders = new ArrayList<>();

    public Prescription(String id, String residentId, String doctorId) {
        this.id = id; this.residentId = residentId; this.doctorId = doctorId;
    }

    public String getId() { return id; }
    public String getResidentId() { return residentId; }
    public String getDoctorId() { return doctorId; }
    public List<MedicationOrder> getOrders() { return orders; }
    public void addOrder(MedicationOrder o) { orders.add(o); }
}
