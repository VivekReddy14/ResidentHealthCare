
package model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class AdministrationRecord implements Serializable {
    private final String medicine; private final String dose; private final LocalDateTime at; private final String nurseId;
    public AdministrationRecord(String medicine, String dose, LocalDateTime at, String nurseId){
        this.medicine=medicine; this.dose=dose; this.at=at; this.nurseId=nurseId;
    }
    public String getMedicine(){ return medicine; }
    public String getDose(){ return dose; }
    public LocalDateTime getAt(){ return at; }
    public String getNurseId(){ return nurseId; }
}
