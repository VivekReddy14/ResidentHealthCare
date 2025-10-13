package assignment2.carehome;

import assignment2.carehome.exception.*;
import assignment2.carehome.model.*;
import assignment2.carehome.service.CareHomeService;
import org.junit.jupiter.api.*;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CareHomeServiceTest {

    CareHomeService svc;
    Manager manager;
    Doctor doctor;
    Nurse nurse;

    @BeforeEach
    void setup(){
        svc = new CareHomeService(List.of(new Bed("B1"), new Bed("B2"), new Bed("B3")));
        manager = new Manager("M1","Mgr","pw");
        doctor = new Doctor("D1","Doc","pw");
        nurse = new Nurse("N1","Nurse","pw");

        svc.addStaff(manager, doctor);
        svc.addStaff(manager, nurse);

        DayOfWeek today = LocalDate.now().getDayOfWeek();
        LocalTime now = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);

        // Nurse shift: 1 hour before to 1 hour after now
        svc.setShift(manager, "N1", today, new Shift(now.minusHours(1), now.plusHours(1)));

        // Doctor shift: now to 1 hour later
        svc.setShift(manager, "D1", today, new Shift(now, now.plusHours(1)));
    }

    @Test
    void addResidentToVacantBed_ok(){
        var r = new Resident("R1","Pat", Gender.F);
        svc.addResidentToVacantBed(manager, r, "B1");
        assertEquals("R1", svc.checkResidentInBed(nurse, "B1").getId());
    }

    @Test
    void addResidentToOccupiedBed_throws(){
        var r1 = new Resident("R1","A", Gender.M);
        var r2 = new Resident("R2","B", Gender.F);
        svc.addResidentToVacantBed(manager, r1, "B1");
        assertThrows(BedOccupiedException.class, () -> svc.addResidentToVacantBed(manager, r2, "B1"));
    }

    @Test
    void modifyStaffPassword_ok(){
        assertDoesNotThrow(() -> svc.modifyStaffPassword(manager, "N1", "newpw"));
    }

//    This testcase gives error while compiling as modifyStaffPassord only take manager, so commenting it 
//    @Test
//    void modifyStaffPassword_unauthorized_throws(){
//        assertThrows(AuthorizationException.class, () -> svc.modifyStaffPassword(doctor, "N1", "pw"));
//    }

    @Test
    void doctorAddsPrescription_onlyDoctorAndRostered(){
        var r = new Resident("R2","Casey", Gender.M);
        svc.addResidentToVacantBed(manager, r, "B2");
        var p = new Prescription("P1", "R2", "D1");

        // doctor can add prescription
        assertDoesNotThrow(() -> svc.doctorAddPrescription(doctor, "R2", p));

        // nurse cannot add prescription
        assertThrows(AuthorizationException.class, () -> svc.doctorAddPrescription(nurse, "R2", p));

        // doctor not rostered
        doctor.clearRoster();
        assertThrows(NotRosteredException.class, () -> svc.doctorAddPrescription(doctor, "R2", p));
    }

    @Test
    void nurseMovesResident_onlyNurseAndRostered(){
        var r = new Resident("R3","Lee", Gender.F);
        svc.addResidentToVacantBed(manager, r, "B1");

        assertDoesNotThrow(() -> svc.moveResident(nurse, "B1", "B2"));
        assertThrows(AuthorizationException.class, () -> svc.moveResident(doctor, "B2", "B1"));

        // Not rostered nurse
        nurse.clearRoster();
        assertThrows(NotRosteredException.class, () -> svc.moveResident(nurse, "B2", "B1"));
    }

    @Test
    void administer_logsAndRequiresRoster(){
        var r = new Resident("R4","Jo", Gender.F);
        svc.addResidentToVacantBed(manager, r, "B1");
        var p = new Prescription("P2", "R4", "D1");
        svc.doctorAddPrescription(doctor, "R4", p);

        // Rostered nurse can administer
        assertDoesNotThrow(() -> svc.administer(nurse, "R4", "P2", "Amox", 500, "mg"));

        // Not rostered nurse throws
        nurse.clearRoster();
        assertThrows(NotRosteredException.class, () -> svc.administer(nurse, "R4", "P2", "Amox", 500, "mg"));
    }

    @Test
    void auditLogRecordsActions(){
        var r = new Resident("R5","Alex", Gender.M);
        svc.addResidentToVacantBed(manager, r, "B3");

        var p = new Prescription("P3","R5","D1");
        svc.doctorAddPrescription(doctor,"R5",p);

        svc.administer(nurse,"R5","P3","Paracetamol",500,"mg");

        var entries = svc.getAuditLog().entries();
        assertTrue(entries.stream().anyMatch(e -> e.action().equals("ADD_RESIDENT_TO_BED")));
        assertTrue(entries.stream().anyMatch(e -> e.action().equals("ADD_PRESCRIPTION")));
        assertTrue(entries.stream().anyMatch(e -> e.action().equals("UPDATE_ADMINISTRATION")));
    }

    @Test
    void setShift_overLimit_throws(){
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        // nurse > 8 hours
        assertThrows(ShiftViolationException.class,
                () -> svc.setShift(manager, "N1", today, new Shift(LocalTime.of(0,0), LocalTime.of(12,0))));
        // doctor > 1 hour
        assertThrows(ShiftViolationException.class,
                () -> svc.setShift(manager, "D1", today, new Shift(LocalTime.of(0,0), LocalTime.of(3,0))));
    }

    @Test
    void checkResidentInEmptyBed_throws(){
        assertThrows(NotFoundException.class, () -> svc.checkResidentInBed(nurse, "B1"));
    }

    @Test
    void unauthorizedActions_throwAuthorization(){
        var r = new Resident("R6","Sam",Gender.F);
        svc.addResidentToVacantBed(manager,r,"B3");

        Staff fake = new Nurse("X1","Fake","pw");
        assertThrows(AuthorizationException.class, () -> svc.checkResidentInBed(fake,"B3"));
    }
}
