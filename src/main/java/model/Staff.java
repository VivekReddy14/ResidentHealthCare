
package model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

public abstract class Staff implements Serializable {
    protected final String id;
    protected String username;
    protected String password;
    protected final Role role;
    protected final List<Shift> shifts = new ArrayList<>();

    public Staff(String id, String username, String password, Role role){
        this.id=id; this.username=username; this.password=password; this.role=role;
    }
    public String getId(){ return id; }
    public String getUsername(){ return username; }
    public Role getRole(){ return role; }
    public boolean credentialsMatch(String u, String p){ return Objects.equals(username,u) && Objects.equals(password,p); }
    public void setPassword(String p){ this.password=p; }
    public void assignShift(Shift s){ shifts.add(s); }
    public List<Shift> getShifts(){ return Collections.unmodifiableList(shifts); }
    public boolean isRostered(LocalDateTime dt){
        return shifts.stream().anyMatch(s -> s.getDay()==dt.getDayOfWeek() && s.contains(dt.toLocalTime()));
    }
}
