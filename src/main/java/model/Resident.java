
package model;

import java.io.Serializable;
import java.util.*;

public class Resident implements Serializable {
    private final String id; private final String name; private final Gender gender; private final boolean isolation;
    private final List<Prescription> prescriptions = new ArrayList<>();
    private final List<AdministrationRecord> administrations = new ArrayList<>();

    public Resident(String id, String name, Gender gender, boolean isolation){
        this.id=id; this.name=name; this.gender=gender; this.isolation=isolation;
    }
    public String getId(){ return id; }
    public String getName(){ return name; }
    public Gender getGender(){ return gender; }
    public boolean isIsolation(){ return isolation; }
    public List<Prescription> getPrescriptions(){ return prescriptions; }
    public List<AdministrationRecord> getAdministrations(){ return administrations; }
    public void addPrescription(Prescription p){ prescriptions.add(p); }
    public void addAdministration(AdministrationRecord r){ administrations.add(r); }
}
