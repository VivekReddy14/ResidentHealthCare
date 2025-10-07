package assignment2.carehome.service;

import java.time.LocalDateTime;
import java.util.*;

public class AuditLog {
    public static record Entry(LocalDateTime at, String staffId, String action, String details) {}
    private final List<Entry> entries = new ArrayList<>();
    public void log(String staffId, String action, String details){
        entries.add(new Entry(LocalDateTime.now(), staffId, action, details));
    }
    public List<Entry> entries(){ return Collections.unmodifiableList(entries); }
}
