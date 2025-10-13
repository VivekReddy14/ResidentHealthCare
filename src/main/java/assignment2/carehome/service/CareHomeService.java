package assignment2.carehome.service;

import assignment2.carehome.exception.*;
import assignment2.carehome.model.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class CareHomeService {

    private final Map<String, Staff> staff = new ConcurrentHashMap<>();
    private final Map<String, Bed> beds = new ConcurrentHashMap<>();
    private final Map<String, Resident> residents = new ConcurrentHashMap<>();
    private final AuditLog audit = new AuditLog();

    public CareHomeService(Collection<Bed> initialBeds){
        initialBeds.forEach(b -> beds.put(b.getBedId(), b));
    }

    // --- Accessors for UI ---
    public Collection<Bed> getBeds() {
        return beds.values();
    }

    public Collection<Staff> getStaff() {
        return staff.values();
    }

    public Collection<Resident> getResidents() {
        return residents.values();
    }

    public AuditLog getAuditLog() {
        return audit;
    }

    // --- Staff Management ---
    public void addStaff(Manager manager, Staff newStaff){
        ensureManager(manager);
        staff.put(newStaff.getId(), newStaff);
        audit.log(manager.getId(), "ADD_STAFF", "staffId="+newStaff.getId()+", role="+newStaff.getRole());
    }

    public void modifyStaffPassword(Manager manager, String staffId, String newPw){
        ensureManager(manager);
        var s = getStaffOrThrow(staffId);
        s.setPassword(newPw);
        audit.log(manager.getId(), "MODIFY_STAFF_PASSWORD", "staffId="+staffId);
    }

    // --- Resident Management ---
    public void addResidentToBed(Manager manager, Resident r, String bedId){
        ensureManager(manager);
        var bed = getBedOrThrow(bedId);
        if (!bed.isVacant()) throw new BedOccupiedException("Bed already occupied: "+bedId);
        residents.put(r.getId(), r);
        bed.assign(r);
        audit.log(manager.getId(), "ADD_RESIDENT", "residentId="+r.getId()+", bedId="+bedId);
    }

    public void removeResidentFromBed(Manager manager, String bedId){
        ensureManager(manager);
        var bed = getBedOrThrow(bedId);
        if (bed.isVacant()) throw new NotFoundException("No resident to remove in "+bedId);
        Resident r = bed.getResident();
        bed.vacate();
        residents.remove(r.getId());
        audit.log(manager.getId(), "REMOVE_RESIDENT", "residentId="+r.getId()+", bedId="+bedId);
    }

    // --- Bed Access ---
    public Bed getBed(String bedId){
        return getBedOrThrow(bedId);
    }

    // --- Helpers ---
    private Staff getStaffOrThrow(String id){
        var s = staff.get(id);
        if (s==null) throw new NotFoundException("No staff "+id);
        return s;
    }

    private Bed getBedOrThrow(String id){
        var b = beds.get(id);
        if (b==null) throw new NotFoundException("No bed "+id);
        return b;
    }

    private void ensureManager(Staff s){
        if (s==null || s.getRole()!=Role.MANAGER) throw new AuthorizationException("Manager role required");
    }
    
 // Add this method
    public List<Bed> getVacantBeds() {
        return beds.values().stream()
                .filter(Bed::isVacant)
                .collect(Collectors.toList());
    }

    // Add helper method to get Manager (for UI/Staff adding)
    public Manager getManager() {
        return staff.values().stream()
                .filter(s -> s.getRole() == Role.MANAGER)
                .map(s -> (Manager) s)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No manager found"));
    }

}
