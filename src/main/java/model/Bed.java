
package model;

import java.io.Serializable;

public class Bed implements Serializable {
    private final String id; private Resident resident;
    public Bed(String id){ this.id=id; }
    public String getId(){ return id; }
    public Resident getResident(){ return resident; }
    public boolean isEmpty(){ return resident==null; }
    public void assign(Resident r){ this.resident=r; }
    public void vacate(){ this.resident=null; }
}
