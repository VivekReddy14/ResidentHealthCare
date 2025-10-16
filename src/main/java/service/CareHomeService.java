package service;

import exception.*;
import model.*;

import java.io.IOException;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

public class CareHomeService {
    private static final String DATA_FILE = "carehome.ser";
    private static CareHomeService INSTANCE;

    public static CareHomeService get() {
        if (INSTANCE == null) INSTANCE = new CareHomeService();
        return INSTANCE;
    }

    private DataStore store = new DataStore();
    private static final int[] ROOM_LAYOUT = new int[]{1, 2, 4, 4, 4, 4};

    private CareHomeService() {
        // Build two wards with the exact layout
        Ward w1 = new Ward("Ward 1", true);
        Ward w2 = new Ward("Ward 2", false);
        createBedsForWard(w1);
        createBedsForWard(w2);
        store.wards.add(w1);
        store.wards.add(w2);

        // Create default manager, doctor, nurse if they don't exist
        Manager mgr = getOrCreateDefaultManager();
        Doctor doc = getOrCreateDefaultDoctor();
        Nurse nur = getOrCreateDefaultNurse();

        // ✅ Auto-assign a shift that covers current time so they're always rostered
        autoAssignCurrentShift(mgr);
        autoAssignCurrentShift(doc);
        autoAssignCurrentShift(nur);
    }

    private void createBedsForWard(Ward w) {
        int bedCount = Arrays.stream(ROOM_LAYOUT).sum(); // 19
        for (int i = 1; i <= bedCount; i++) {
            w.addBed(new Bed((w.getId().equals("Ward 1") ? "W1" : "W2") + "-B" + i));
        }
    }

    private Manager getOrCreateDefaultManager() {
        return (Manager) store.staff.values().stream()
                .filter(s -> s instanceof Manager && s.getUsername().equals("manager"))
                .findFirst()
                .orElseGet(() -> {
                    Manager m = new Manager(IdUtil.nextId("STF"), "manager", "password");
                    store.staff.put(m.getId(), m);
                    return m;
                });
    }

    private Doctor getOrCreateDefaultDoctor() {
        return (Doctor) store.staff.values().stream()
                .filter(s -> s instanceof Doctor && s.getUsername().equals("doctor"))
                .findFirst()
                .orElseGet(() -> {
                    Doctor d = new Doctor(IdUtil.nextId("STF"), "doctor", "password");
                    store.staff.put(d.getId(), d);
                    return d;
                });
    }

    private Nurse getOrCreateDefaultNurse() {
        return (Nurse) store.staff.values().stream()
                .filter(s -> s instanceof Nurse && s.getUsername().equals("nurse"))
                .findFirst()
                .orElseGet(() -> {
                    Nurse n = new Nurse(IdUtil.nextId("STF"), "nurse", "password");
                    store.staff.put(n.getId(), n);
                    return n;
                });
    }

    private void autoAssignCurrentShift(Staff staff) {
        LocalDateTime now = LocalDateTime.now();
        DayOfWeek day = now.getDayOfWeek();
        LocalTime start = now.toLocalTime().minusHours(1);
        LocalTime end = now.toLocalTime().plusHours(1);
        staff.assignShift(new Shift(day, start, end));
    }

    public void save() throws IOException {
        DataStore.save(store, DATA_FILE);
    }

    public void load() throws Exception {
        store = DataStore.load(DATA_FILE);
    }

    public Staff login(String username, String password) throws AuthorizationException {
        Optional<Staff> match = store.staff.values().stream()
                .filter(s -> s.credentialsMatch(username, password))
                .findFirst();
        if (match.isEmpty()) throw new AuthorizationException("Invalid credentials");
        Session.get().setCurrentUser(match.get());
        AuditLog.log(match.get().getId(), "login");
        return match.get();
    }

    private void ensureRole(Role role) throws AuthorizationException {
        Staff u = Session.get().getCurrentUser();
        if (u == null || u.getRole() != role)
            throw new AuthorizationException("Only " + role + " may perform this action");
    }

    private void ensureRostered() throws NotRosteredException {
        Staff u = Session.get().getCurrentUser();
        if (u == null || !u.isRostered(LocalDateTime.now()))
            throw new NotRosteredException("You are not rostered right now");
    }

    public Manager createManager(String u, String p) throws AuthorizationException {
        ensureRole(Role.MANAGER);
        Manager m = new Manager(IdUtil.nextId("STF"), u, p);
        store.staff.put(m.getId(), m);
        AuditLog.log(Session.get().getCurrentUser().getId(), "create manager " + u);
        return m;
    }

    public Doctor createDoctor(String u, String p) throws AuthorizationException {
        ensureRole(Role.MANAGER);
        Doctor d = new Doctor(IdUtil.nextId("STF"), u, p);
        store.staff.put(d.getId(), d);
        AuditLog.log(Session.get().getCurrentUser().getId(), "create doctor " + u);
        return d;
    }

    public Nurse createNurse(String u, String p) throws AuthorizationException {
        ensureRole(Role.MANAGER);
        Nurse n = new Nurse(IdUtil.nextId("STF"), u, p);
        store.staff.put(n.getId(), n);
        AuditLog.log(Session.get().getCurrentUser().getId(), "create nurse " + u);
        return n;
    }

    public void updateStaffPassword(String id, String pass) throws Exception {
        ensureRole(Role.MANAGER);
        Staff s = store.staff.get(id);
        if (s == null) throw new NotFoundException("No staff " + id);
        s.setPassword(pass);
        AuditLog.log(Session.get().getCurrentUser().getId(), "update password " + id);
    }

    public void assignShift(String id, Shift shift) throws AuthorizationException {
        ensureRole(Role.MANAGER);
        store.staff.get(id).assignShift(shift);
        AuditLog.log(Session.get().getCurrentUser().getId(), "assign shift " + id + " " + shift);
    }

    public Resident addResident(String name, Gender gender, boolean iso) throws Exception {
        ensureRole(Role.MANAGER);
        Resident r = new Resident(IdUtil.nextId("RES"), name, gender, iso);
        store.residents.put(r.getId(), r);
        AuditLog.log(Session.get().getCurrentUser().getId(), "add resident " + name);
        return r;
    }

    // Helper methods for rooms
    private List<List<Bed>> roomsFor(Ward w) {
        List<List<Bed>> rooms = new ArrayList<>();
        int idx = 0;
        for (int size : ROOM_LAYOUT) {
            List<Bed> room = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                if (idx < w.getBeds().size()) room.add(w.getBeds().get(idx++));
            }
            rooms.add(room);
        }
        return rooms;
    }

    private boolean roomHasVacancy(List<Bed> room) {
        return room.stream().anyMatch(Bed::isEmpty);
    }

    private boolean roomEmpty(List<Bed> room) {
        return room.stream().allMatch(Bed::isEmpty);
    }

    public boolean roomAllGender(List<Bed> room, Gender gender) {
        return room.stream().filter(b -> !b.isEmpty())
                .allMatch(b -> b.getResident().getGender() == gender);
    }

    public List<List<Bed>> findRoomsForGender(Gender gender) {
        List<List<Bed>> result = new ArrayList<>();
        for (Ward w : store.wards) {
            for (List<Bed> room : roomsFor(w)) {
                if (roomHasVacancy(room) && roomAllGender(room, gender)) {
                    result.add(room);
                }
            }
        }
        return result;
    }

    public Bed allocateToRoom(List<Bed> room, Resident r) {
        for (Bed b : room) {
            if (b.isEmpty()) {
                b.assign(r);
                AuditLog.log(Session.get().getCurrentUser().getId(), "allocate " + r.getName() + " to " + b.getId());
                return b;
            }
        }
        return null;
    }

    // ---------- NEW: precise finders to enforce 4 -> 2 -> 1 priority ----------
    private Bed findBedSameGenderOrEmpty(Resident r, int... sizes) {
        for (int size : sizes) {
            for (Ward w : store.wards) {
                for (List<Bed> room : roomsFor(w)) {
                    if (room.size() != size || !roomHasVacancy(room)) continue;
                    if (roomEmpty(room) || roomAllGender(room, r.getGender())) {
                        for (Bed b : room) if (b.isEmpty()) return b;
                    }
                }
            }
        }
        return null;
    }

    private Bed findBedIgnoreGender(int... sizes) {
        for (int size : sizes) {
            for (Ward w : store.wards) {
                for (List<Bed> room : roomsFor(w)) {
                    if (room.size() != size || !roomHasVacancy(room)) continue;
                    for (Bed b : room) if (b.isEmpty()) return b;
                }
            }
        }
        return null;
    }
    // -------------------------------------------------------------------------

    /**
     * Manager flow with gender preference:
     * - Try same-gender or empty rooms with 4 -> 2 -> 1 priority.
     * - If none and confirmed==false, throw so UI can ask user whether mixed is ok.
     * - If confirmed==true, allocate ignoring gender with 4 -> 2 -> 1 priority.
     * Isolation residents are handled by allocateResidentToBed (1 empty -> 2 empty).
     */
    public void allocateResidentToBedWithGender(Resident r, boolean confirmed) throws Exception {
        ensureRole(Role.MANAGER);

        if (r.isIsolation()) {
            // Defer to isolation path in the generic allocator to avoid duplication
            allocateResidentToBed(r.getId());
            return;
        }

        Bed target = findBedSameGenderOrEmpty(r, 4, 2, 1);
        if (target != null) {
            target.assign(r);
            AuditLog.log(Session.get().getCurrentUser().getId(), "allocate " + r.getName() + " to " + target.getId());
            return;
        }

        if (!confirmed) {
            throw new BedOccupiedException("No same-gender (or empty) room available.");
        }

        target = findBedIgnoreGender(4, 2, 1);
        if (target == null) throw new BedOccupiedException("No vacant bed available.");
        target.assign(r);
        AuditLog.log(Session.get().getCurrentUser().getId(), "allocate (mixed) " + r.getName() + " to " + target.getId());
    }

    public void allocateResidentToBed(String residentId) throws Exception {
        ensureRole(Role.MANAGER);
        Resident r = store.residents.get(residentId);
        if (r == null) throw new NotFoundException("Resident not found");

        java.util.function.Function<Integer, Bed> findByRoomSize = (Integer roomSize) -> {
            for (Ward w : store.wards) {
                for (List<Bed> room : roomsFor(w)) {
                    if (room.size() == roomSize && roomHasVacancy(room)) {
                        for (Bed b : room) if (b.isEmpty()) return b;
                    }
                }
            }
            return null;
        };

        if (!r.isIsolation()) {
            Bed target = findByRoomSize.apply(4);
            if (target == null) target = findByRoomSize.apply(2);
            if (target == null) target = findByRoomSize.apply(1);
            if (target == null) throw new BedOccupiedException("No vacant bed available.");
            target.assign(r);
            AuditLog.log(Session.get().getCurrentUser().getId(), "allocate " + r.getName() + " to " + target.getId());
            return;
        }

        for (Ward w : store.wards) {
            for (List<Bed> room : roomsFor(w)) {
                if (room.size() == 1 && roomEmpty(room)) {
                    Bed b = room.get(0);
                    b.assign(r);
                    AuditLog.log(Session.get().getCurrentUser().getId(), "allocate (isolation) " + r.getName() + " to " + b.getId());
                    return;
                }
            }
        }
        for (Ward w : store.wards) {
            for (List<Bed> room : roomsFor(w)) {
                if (room.size() == 2 && roomEmpty(room)) {
                    Bed b = room.get(0);
                    b.assign(r);
                    AuditLog.log(Session.get().getCurrentUser().getId(), "allocate (isolation fallback) " + r.getName() + " to " + b.getId());
                    return;
                }
            }
        }
        throw new BedOccupiedException("No suitable isolation bed available. You may need to move other residents.");
    }

    public void moveResident(String fromId, String toId) throws Exception {
        ensureRole(Role.NURSE);
        ensureRostered();
        Bed from = null, to = null;
        for (Ward w : store.wards)
            for (Bed b : w.getBeds()) {
                if (b.getId().equals(fromId)) from = b;
                if (b.getId().equals(toId)) to = b;
            }
        if (from == null || to == null) throw new NotFoundException("Bed id invalid");
        if (to.getResident() != null) throw new BedOccupiedException("Destination occupied");
        Resident r = from.getResident();
        if (r == null) throw new NotFoundException("No resident in source");
        to.assign(r);
        from.vacate();
        AuditLog.log(Session.get().getCurrentUser().getId(), "move " + r.getName() + " " + fromId + "->" + toId);
    }

    public Prescription addPrescription(String residentId) throws Exception {
        ensureRole(Role.DOCTOR);
        ensureRostered();
        Resident r = store.residents.get(residentId);
        if (r == null) throw new NotFoundException("Resident not found");
        Prescription p = new Prescription(IdUtil.nextId("RX"), Session.get().getCurrentUser().getId(), LocalDateTime.now());
        r.addPrescription(p);
        AuditLog.log(Session.get().getCurrentUser().getId(), "add prescription for " + r.getName());
        return p;
    }

    public void addMedicationOrder(String residentId, String prescId, String med, String dose, LocalTime time) throws Exception {
        ensureRole(Role.DOCTOR);
        Resident r = store.residents.get(residentId);
        Prescription p = r.getPrescriptions().stream()
                .filter(x -> x.getId().equals(prescId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Prescription not found"));
        p.addOrder(new MedicationOrder(med, dose, time));
        AuditLog.log(Session.get().getCurrentUser().getId(), "add medication " + med);
    }

    public void administer(String residentId, String med, String dose) throws Exception {
        ensureRole(Role.NURSE);
        ensureRostered();
        Resident r = store.residents.get(residentId);
        if (r == null) throw new NotFoundException("Resident not found");
        r.addAdministration(new AdministrationRecord(med, dose, LocalDateTime.now(), Session.get().getCurrentUser().getId()));
        AuditLog.log(Session.get().getCurrentUser().getId(), "administer " + med + " to " + r.getName());
    }

    public void discharge(String residentId) throws Exception {
        ensureRole(Role.MANAGER);
        Resident r = store.residents.remove(residentId);
        if (r == null) throw new NotFoundException("Resident not found");
        for (Ward w : store.wards)
            for (Bed b : w.getBeds())
                if (b.getResident() != null && b.getResident().getId().equals(residentId)) b.vacate();
        ArchiveUtil.archiveResident(r);
        AuditLog.log(Session.get().getCurrentUser().getId(), "discharge " + r.getName());
    }

    public void checkCompliance() throws ComplianceException {
        Map<String, Map<DayOfWeek, Integer>> nurseHours = new HashMap<>();
        for (var s : store.staff.values()) {
            if (s.getRole() == Role.NURSE) {
                Map<DayOfWeek, Integer> dayHours = nurseHours.computeIfAbsent(s.getId(), k -> new EnumMap<>(DayOfWeek.class));
                for (Shift sh : s.getShifts()) {
                    dayHours.merge(sh.getDay(), sh.hours(), Integer::sum);
                }
                for (DayOfWeek d : DayOfWeek.values()) {
                    int h = dayHours.getOrDefault(d, 0);
                    if (h > 8) throw new ComplianceException("Nurse " + s.getUsername() + " exceeds 8 hours on " + d);
                }
            }
        }
        boolean doctorOk = store.staff.values().stream().filter(st -> st.getRole() == Role.DOCTOR).anyMatch(st -> {
            for (DayOfWeek d : DayOfWeek.values()) {
                int hours = st.getShifts().stream().filter(sh -> sh.getDay() == d).mapToInt(Shift::hours).sum();
                if (hours < 1) return false;
            }
            return true;
        });
        if (!doctorOk) throw new ComplianceException("No doctor assigned for 1 hour every day");
    }

    public List<Ward> getWards() {
        return store.wards;
    }

    public Collection<Staff> getStaff() {
        return store.staff.values();
    }

    public Collection<Resident> getResidents() {
        return store.residents.values();
    }
    
    // Wipe everything except default logins, rebuild wards, roster defaults now, and save.
    public void clearAllData() {
        // Optional: if you want IDs to restart from 1 after clearing, uncomment:
        // IdUtil.reset();

        // Fresh datastore
        DataStore newStore = new DataStore();

        // Rebuild two wards with the fixed 19-bed layout
        Ward w1 = new Ward("Ward 1", true);
        Ward w2 = new Ward("Ward 2", false);
        createBedsForWard(w1);
        createBedsForWard(w2);
        newStore.wards.add(w1);
        newStore.wards.add(w2);

        // Recreate default accounts
        Manager mgr = new Manager(IdUtil.nextId("STF"), "manager", "password");
        Doctor  doc = new Doctor (IdUtil.nextId("STF"), "doctor",  "password");
        Nurse   nur = new Nurse  (IdUtil.nextId("STF"), "nurse",   "password");

        newStore.staff.put(mgr.getId(), mgr);
        newStore.staff.put(doc.getId(), doc);
        newStore.staff.put(nur.getId(), nur);

        // Swap in the fresh store
        this.store = newStore;

        // Make sure defaults are rostered right now
        autoAssignCurrentShift(mgr);
        autoAssignCurrentShift(doc);
        autoAssignCurrentShift(nur);

        // Persist cleared state
        try {
            save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
