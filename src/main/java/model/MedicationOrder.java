
package model;

import java.io.Serializable;
import java.time.LocalTime;

public class MedicationOrder implements Serializable {
    private final String medicine; private final String dose; private final LocalTime time;
    public MedicationOrder(String medicine, String dose, LocalTime time){
        this.medicine=medicine; this.dose=dose; this.time=time;
    }
    public String getMedicine(){ return medicine; }
    public String getDose(){ return dose; }
    public LocalTime getTime(){ return time; }
}
