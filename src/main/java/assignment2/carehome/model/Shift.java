package assignment2.carehome.model;

import java.time.LocalTime;

public record Shift(LocalTime start, LocalTime end) {
    public int hours() {
        return end.getHour() - start.getHour();
    }

    public boolean includes(LocalTime t) {
        return !t.isBefore(start) && !t.isAfter(end);
    }

    public static Shift of(int startHour, int endHour) {
        return new Shift(LocalTime.of(startHour,0), LocalTime.of(endHour,0));
    }
}
