package assignment2.carehome.model;

import java.time.*;

public record Shift(LocalTime start, LocalTime end) {
    public int hours() { return (int) Duration.between(start, end).toHours(); }
    public boolean includes(LocalTime t){ return !t.isBefore(start) && t.isBefore(end); }
    public static Shift of(int startHour, int endHour){
        return new Shift(LocalTime.of(startHour,0), LocalTime.of(endHour,0));
    }
}
