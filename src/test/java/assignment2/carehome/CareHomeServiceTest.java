package assignment2.carehome;

import assignment2.carehome.exception.*;
import assignment2.carehome.model.*;
import assignment2.carehome.service.CareHomeService;
import org.junit.jupiter.api.*;

import java.time.DayOfWeek;
import java.time.*;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

public class CareHomeServiceTest {

    CareHomeService svc;
    Manager manager;
    Doctor doctor;
    Nurse nurse;

    @BeforeEach
    void setup(){
        svc = new CareHomeService(java.util.List.of(new Bed("B1"), new Bed("B2")));
        manager = new Manager("M1","Mgr","pw");
        doctor = new Doctor("D1","Doc","pw");
        nurse = new Nurse("N1","Nurse","pw");

        svc.addStaff(manager, doctor);
        svc.addStaff(manager, nurse);

        DayOfWeek today = LocalDate.now().getDayOfWeek();

        LocalTime now = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);

        LocalTime nurseStart = now.minusHours(1);
        LocalTime nurseEnd   = now.plusHours(1);
        svc.setShift(manager, "N1", today, new Shift(nurseStart, nurseEnd));

        LocalTime docStart = now;
        LocalTime docEnd   = now.plusHours(1);
        svc.setShift(manager, "D1", today, new Shift(docStart, docEnd));
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

    @Test
    void doctorAddsPrescription_onlyDoctorAndRostered(){
        var r = new Resident("R2","Casey", Gender.M);
        svc.addResidentToVacantBed(manager, r, "B2");
        var p = new Prescription("P1", "R2", "D1");
        assertDoesNotThrow(() -> svc.doctorAddPrescription(doctor, "R2", p));
        assertThrows(AuthorizationException.class, () -> svc.doctorAddPrescription(nurse, "R2", p));
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
}
