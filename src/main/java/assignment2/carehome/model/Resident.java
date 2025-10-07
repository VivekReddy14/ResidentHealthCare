package assignment2.carehome.model;

import java.util.*;

public class Resident {
    private final String id;
    private final String name;
    private final Gender gender;
    private final List<Prescription> prescriptions = new ArrayList<>();
    public Resident(String id, String name, Gender gender){
        this.id=id; this.name=name; this.gender=gender;
    }
    public String getId(){ return id; }
    public String getName(){ return name; }
    public Gender getGender(){ return gender; }
    public List<Prescription> getPrescriptions(){ return prescriptions; }
}
