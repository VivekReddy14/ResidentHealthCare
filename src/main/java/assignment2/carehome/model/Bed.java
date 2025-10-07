package assignment2.carehome.model;

public class Bed {
    private final String bedId;
    private Resident resident;
    public Bed(String bedId){ this.bedId = bedId; }
    public String getBedId(){ return bedId; }
    public Resident getResident(){ return resident; }
    public boolean isVacant(){ return resident == null; }
    public void assign(Resident r){ this.resident = r; }
    public void vacate(){ this.resident = null; }
}
