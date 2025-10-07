package assignment2.carehome.service;

import assignment2.carehome.exception.*;
import assignment2.carehome.model.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CareHomeService {

    private final Map<String, Staff> staff = new ConcurrentHashMap<>();
    private final Map<String, Bed> beds = new ConcurrentHashMap<>();
    private final Map<String, Resident> residents = new ConcurrentHashMap<>();
    private final Map<String, AdministrationRecord> administrations = new LinkedHashMap<>();
    private final AuditLog audit = new AuditLog();

    public CareHomeService(Collection<Bed> initialBeds){
        initialBeds.forEach(b -> beds.put(b.getBedId(), b));
    }

    public AuditLog getAuditLog(){ return audit; }

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

    public void setShift(Manager manager, String staffId, DayOfWeek day, Shift shift){
        ensureManager(manager);
        var s = getStaffOrThrow(staffId);
        var dayShifts = s.getRoster().get(day);
        int total = dayShifts.stream().mapToInt(Shift::hours).sum() + shift.hours();
        if (s.getRole()==Role.NURSE && total>8) throw new ShiftViolationException("Nurse over 8 hours on "+day);
        if (s.getRole()==Role.DOCTOR && total>1) throw new ShiftViolationException("Doctor over 1 hour on "+day);
        dayShifts.add(shift);
        audit.log(manager.getId(), "SET_SHIFT", "staffId="+staffId+", day="+day+", shift="+shift);
    }

    public void addResidentToVacantBed(Staff by, Resident r, String bedId){
        ensureAuthorized(by);
        if (by.getRole()!=Role.MANAGER) throw new AuthorizationException("Only manager can add resident");
        var bed = getBedOrThrow(bedId);
        if (!bed.isVacant()) throw new BedOccupiedException("Bed occupied: "+bedId);
        residents.put(r.getId(), r);
        bed.assign(r);
        audit.log(by.getId(), "ADD_RESIDENT_TO_BED", "residentId="+r.getId()+", bedId="+bedId);
    }

    public Resident checkResidentInBed(Staff by, String bedId){
        ensureAuthorized(by);
        var bed = getBedOrThrow(bedId);
        var r = bed.getResident();
        if (r==null) throw new NotFoundException("No resident in "+bedId);
        audit.log(by.getId(), "CHECK_RESIDENT", "bedId="+bedId+", residentId="+r.getId());
        return r;
    }

    public void moveResident(Staff by, String fromBedId, String toBedId){
        ensureAuthorized(by);
        if (by.getRole()!=Role.NURSE) throw new AuthorizationException("Only nurse can move residents");
        ensureRostered(by);
        var from = getBedOrThrow(fromBedId);
        var to = getBedOrThrow(toBedId);
        var r = from.getResident();
        if (r==null) throw new NotFoundException("No resident in source bed");
        if (!to.isVacant()) throw new BedOccupiedException("Destination bed occupied");
        from.vacate(); to.assign(r);
        audit.log(by.getId(), "MOVE_RESIDENT", "residentId="+r.getId()+", from="+fromBedId+", to="+toBedId);
    }

    public Prescription doctorAddPrescription(Staff by, String residentId, Prescription p){
        ensureAuthorized(by);
        if (by.getRole()!=Role.DOCTOR) throw new AuthorizationException("Only doctor can add prescriptions");
        ensureRostered(by);
        var r = getResidentOrThrow(residentId);
        r.getPrescriptions().add(p);
        audit.log(by.getId(), "ADD_PRESCRIPTION", "residentId="+residentId+", prescriptionId="+p.getId());
        return p;
    }

    public void nurseUpdatePrescriptionAdministered(Staff by, String residentId, String prescriptionId,
                                                    String medicine, double dose, String unit){
        ensureAuthorized(by);
        if (by.getRole()!=Role.NURSE) throw new AuthorizationException("Only nurse can update administration");
        ensureRostered(by);
        var r = getResidentOrThrow(residentId);
        r.getPrescriptions().stream().filter(x -> x.getId().equals(prescriptionId)).findFirst()
                .orElseThrow(() -> new NotFoundException("No prescription "+prescriptionId));
        var rec = new AdministrationRecord(residentId, prescriptionId, medicine, dose, unit, LocalDateTime.now(), by.getId());
        administrations.put(UUID.randomUUID().toString(), rec);
        audit.log(by.getId(), "UPDATE_ADMINISTRATION",
                "residentId="+residentId+", prescriptionId="+prescriptionId+", med="+medicine+", dose="+dose+unit);
    }

    public void administer(Staff by, String residentId, String prescriptionId, String medicine, double dose, String unit){
        nurseUpdatePrescriptionAdministered(by, residentId, prescriptionId, medicine, dose, unit);
    }

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
    private Resident getResidentOrThrow(String id){
        var r = residents.get(id);
        if (r==null) throw new NotFoundException("No resident "+id);
        return r;
    }

    private void ensureManager(Staff s){
        if (s==null || s.getRole()!=Role.MANAGER) throw new AuthorizationException("Manager role required");
    }
    private void ensureAuthorized(Staff s){
        if (s==null) throw new AuthorizationException("Unknown staff");
        if (!staff.containsKey(s.getId()) && s.getRole()!=Role.MANAGER)
            throw new AuthorizationException("Staff not registered: "+s.getId());
    }
    private void ensureRostered(Staff s){
        var now = LocalDateTime.now();
        var shifts = s.getRoster().get(now.getDayOfWeek());
        boolean ok = shifts.stream().anyMatch(sh -> sh.includes(now.toLocalTime()));
        if (!ok) throw new NotRosteredException("Staff not rostered now: "+s.getId());
    }
    
    
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        CareHomeService svc = new CareHomeService(List.of(
                new Bed("B1"), new Bed("B2"), new Bed("B3")
        ));

        Manager mgr = new Manager("M1", "Alice Manager", "pw");
        Doctor  doc = new Doctor("D1", "Bob Doctor", "pw");
        Nurse   nur = new Nurse("N1", "Nina Nurse", "pw");

        svc.addStaff(mgr, doc);
        svc.addStaff(mgr, nur);

        DayOfWeek today = LocalDate.now().getDayOfWeek();
        LocalTime now   = LocalTime.now().withSecond(0).withNano(0);
        svc.setShift(mgr, "N1", today, new Shift(now.minusMinutes(30), now.plusHours(1)));
        svc.setShift(mgr, "D1", today, new Shift(now, now.plusHours(1)));

        Resident r0 = new Resident("R1", "Charlie Patient", Gender.M);
        svc.addResidentToVacantBed(mgr, r0, "B1");

        while (true) {
            System.out.println("\n===== CARE HOME MENU =====");
            System.out.println("1) List beds & residents");
            System.out.println("2) List staff & shifts (today)");
            System.out.println("3) Add resident to vacant bed   [Manager]");
            System.out.println("4) Add staff (Doctor/Nurse)     [Manager]");
            System.out.println("5) Modify staff password        [Manager]");
            System.out.println("6) Set shift for staff (today)  [Manager]");
            System.out.println("7) Check resident in bed        [Any staff]");
            System.out.println("8) Doctor adds prescription     [Doctor]");
            System.out.println("9) Nurse moves resident         [Nurse]");
            System.out.println("10) Nurse administers prescription [Nurse]");
            System.out.println("11) Show audit log");
            System.out.println("0) Exit");
            System.out.print("Choose: ");
            String choice = sc.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> {
                        System.out.println("\n-- Beds --");
                        svc.beds.values().stream()
                                .sorted(Comparator.comparing(Bed::getBedId))
                                .forEach(b -> System.out.printf("%s : %s%n",
                                        b.getBedId(),
                                        b.isVacant() ? "(vacant)" :
                                                (b.getResident().getId() + " - " + b.getResident().getName() + " [" + b.getResident().getGender() + "]")));
                    }
                    case "2" -> {
                        System.out.println("\n-- Staff (today's shifts) --");
                        svc.staff.values().forEach(s -> {
                            System.out.printf("%s (%s) id=%s%n", s.getClass().getSimpleName(), s.getRole(), s.getId());
                            var shifts = s.getRoster().get(LocalDate.now().getDayOfWeek());
                            if (shifts == null || shifts.isEmpty()) {
                                System.out.println("   (no shifts today)");
                            } else {
                                for (Shift sh : shifts) {
                                    System.out.println("   " + sh.start() + " - " + sh.end());
                                }
                            }
                        });
                    }
                    case "3" -> {
                        System.out.print("Resident ID: ");
                        String rid = sc.nextLine().trim();
                        System.out.print("Resident name: ");
                        String rname = sc.nextLine().trim();
                        System.out.print("Gender (M/F): ");
                        String g = sc.nextLine().trim().toUpperCase();
                        Gender gender = "F".equals(g) ? Gender.F : Gender.M;
                        System.out.print("Vacant bedId (e.g., B2): ");
                        String bedId = sc.nextLine().trim();
                        Resident r = new Resident(rid, rname, gender);
                        svc.addResidentToVacantBed(mgr, r, bedId);
                        System.out.println("Added " + rname + " to " + bedId);
                    }
                    case "4" -> {
                        System.out.print("New staff role (D/N): ");
                        String rr = sc.nextLine().trim().toUpperCase();
                        System.out.print("Staff ID: ");
                        String sid = sc.nextLine().trim();
                        System.out.print("Name: ");
                        String sname = sc.nextLine().trim();
                        System.out.print("Password: ");
                        String spw = sc.nextLine().trim();
                        Staff ns = rr.equals("D") ? new Doctor(sid, sname, spw) : new Nurse(sid, sname, spw);
                        svc.addStaff(mgr, ns);
                        System.out.println("Added " + ns.getRole() + " id=" + sid);
                    }
                    case "5" -> {
                        System.out.print("Staff ID to modify: ");
                        String sid = sc.nextLine().trim();
                        System.out.print("New password: ");
                        String npw = sc.nextLine().trim();
                        svc.modifyStaffPassword(mgr, sid, npw);
                        System.out.println("Password updated for " + sid);
                    }
                    case "6" -> {
                        System.out.print("Staff ID: ");
                        String sid = sc.nextLine().trim();
                        System.out.print("Start hour (0-23): ");
                        int sh = Integer.parseInt(sc.nextLine().trim());
                        System.out.print("End hour (0-23): ");
                        int eh = Integer.parseInt(sc.nextLine().trim());
                        svc.setShift(mgr, sid, LocalDate.now().getDayOfWeek(), Shift.of(sh, eh));
                        System.out.println("Shift set for " + sid + " today " + sh + ":00-" + eh + ":00");
                    }
                    case "7" -> {
                        System.out.print("Bed ID: ");
                        String bedId = sc.nextLine().trim();
                        // simulate medical staff checking (nurse)
                        Resident r = svc.checkResidentInBed(nur, bedId);
                        System.out.println("Resident: " + r.getId() + " - " + r.getName() + " [" + r.getGender() + "]");
                    }
                    case "8" -> {
                        System.out.print("Resident ID: ");
                        String rid = sc.nextLine().trim();
                        System.out.print("Prescription ID: ");
                        String pid = sc.nextLine().trim();
                        Prescription p = new Prescription(pid, rid, doc.getId());

                        // allow 1..n orders
                        while (true) {
                            System.out.print("Add medication order? (y/n): ");
                            if (!sc.nextLine().trim().equalsIgnoreCase("y")) break;
                            System.out.print("Medicine name: ");
                            String med = sc.nextLine().trim();
                            System.out.print("Dose (number): ");
                            double dose = Double.parseDouble(sc.nextLine().trim());
                            System.out.print("Unit (e.g., mg/tab): ");
                            String unit = sc.nextLine().trim();
                            System.out.print("At hour (0-23): ");
                            int hh = Integer.parseInt(sc.nextLine().trim());
                            System.out.print("At minute (0-59): ");
                            int mm = Integer.parseInt(sc.nextLine().trim());
                            p.addOrder(new MedicationOrder(med, dose, unit, LocalTime.of(hh, mm)));
                        }

                        svc.doctorAddPrescription(doc, rid, p);
                        System.out.println("Prescription " + pid + " added for resident " + rid);
                    }
                    case "9" -> {
                        System.out.print("From bedId: ");
                        String from = sc.nextLine().trim();
                        System.out.print("To bedId: ");
                        String to = sc.nextLine().trim();
                        svc.moveResident(nur, from, to);
                        System.out.println("Moved from " + from + " to " + to);
                    }
                    case "10" -> {
                        System.out.print("Resident ID: ");
                        String rid = sc.nextLine().trim();
                        System.out.print("Prescription ID: ");
                        String pid = sc.nextLine().trim();
                        System.out.print("Medicine name: ");
                        String med = sc.nextLine().trim();
                        System.out.print("Dose (number): ");
                        double dval = Double.parseDouble(sc.nextLine().trim());
                        System.out.print("Unit: ");
                        String unit = sc.nextLine().trim();
                        svc.administer(nur, rid, pid, med, dval, unit);
                        System.out.println("Administered " + med + " " + dval + unit + " to resident " + rid);
                    }
                    case "11" -> {
                        System.out.println("\n=== AUDIT LOG ===");
                        svc.getAuditLog().entries().forEach(e ->
                                System.out.printf("%s | staff=%s | %s | %s%n",
                                        e.at(), e.staffId(), e.action(), e.details()));
                    }
                    case "0" -> {
                        System.out.println("Bye!");
                        return;
                    }
                    default -> System.out.println("Invalid choice.");
                }
            } catch (Exception ex) {
                System.out.println("ERROR: " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
            }
        }
    }

}
