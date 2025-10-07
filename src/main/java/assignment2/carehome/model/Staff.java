package assignment2.carehome.model;

import java.time.DayOfWeek;
import java.util.*;

public abstract class Staff {
    protected final String id;
    protected String name;
    protected Role role;
    protected String password;
    protected final Map<DayOfWeek, List<Shift>> roster = new EnumMap<>(DayOfWeek.class);

    protected Staff(String id, String name, Role role, String password){
        this.id=id; this.name=name; this.role=role; this.password=password;
        for (var d : DayOfWeek.values()) roster.put(d, new ArrayList<>());
    }
    public String getId(){ return id; }
    public Role getRole(){ return role; }
    public void setPassword(String pw){ this.password = pw; }
    public Map<DayOfWeek, List<Shift>> getRoster(){ return roster; }
    public void clearRoster(){ roster.values().forEach(List::clear); }
}
