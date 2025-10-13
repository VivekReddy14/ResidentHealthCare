package assignment2.carehome.model;

import java.time.DayOfWeek;
import java.util.*;

public abstract class Staff {
    private final String id;
    private String name;
    private String password;
    private final Role role;
    private final Map<DayOfWeek, List<Shift>> roster = new EnumMap<>(DayOfWeek.class);

    public Staff(String id, String name, String password, Role role) {
        this.id = id;
        this.name = name;
        this.password = password;
        this.role = role;
        for (DayOfWeek d : DayOfWeek.values()) {
            roster.put(d, new ArrayList<>());
        }
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Role getRole() { return role; }
    public Map<DayOfWeek, List<Shift>> getRoster() { return roster; }

//    Clears all shifts from this staff's roster. 
     
    public void clearRoster() {
        for (List<Shift> shifts : roster.values()) {
            shifts.clear();
        }
    }
}
