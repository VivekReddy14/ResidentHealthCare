package assignment2.carehome;

import exception.*;
import model.*;
import service.CareHomeService;

import org.junit.jupiter.api.*;

import java.time.*;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

public class CareHomeServiceTest {

    private CareHomeService svc;

    @BeforeEach
    void setup() {
        svc = CareHomeService.get();
        svc.clearAllData();  // ensures fresh state for each test
    }

    @Test
    void testLoginValidCredentials() throws AuthorizationException {
        Staff s = svc.login("manager", "password");
        assertNotNull(s);
        assertEquals(Role.MANAGER, s.getRole());
    }

    @Test
    void testLoginInvalidCredentialsThrows() {
        assertThrows(AuthorizationException.class, () -> svc.login("wrong", "pass"));
    }

    @Test
    void testAddResidentAndAllocateBed() throws Exception {
        // Login as manager to perform admin actions
        svc.login("manager", "password");

        Resident r = svc.addResident("Alice", Gender.FEMALE, false);
        svc.allocateResidentToBed(r.getId());

        boolean found = svc.getWards().stream()
                .flatMap(w -> w.getBeds().stream())
                .anyMatch(b -> b.getResident() != null && b.getResident().getId().equals(r.getId()));

        assertTrue(found, "Resident should be allocated to a bed");
    }

    @Test
    void testAddResidentIsolationAllocation() throws Exception {
        svc.login("manager", "password");

        Resident r = svc.addResident("Bob", Gender.MALE, true);
        svc.allocateResidentToBed(r.getId());

        Bed allocated = svc.getWards().stream()
                .flatMap(w -> w.getBeds().stream())
                .filter(b -> b.getResident() != null && b.getResident().getId().equals(r.getId()))
                .findFirst().orElseThrow();

        assertTrue(allocated.getId().contains("B1") || allocated.getId().contains("B2"),
                "Isolation patient should be allocated to a single or 2-bed room");
    }

    @Test
    void testClearAllDataResetsResidentsAndKeepsDefaultLogins() throws Exception {
        svc.login("manager", "password");

        svc.addResident("Chris", Gender.MALE, false);
        svc.clearAllData();

        Collection<Resident> residents = svc.getResidents();
        assertEquals(0, residents.size(), "Residents should be cleared");

        Staff m = svc.login("manager", "password");
        assertNotNull(m);
        assertEquals(Role.MANAGER, m.getRole());
    }

    @Test
    void testShiftComplianceForNurses() throws Exception {
        svc.login("manager", "password");

        // Create a nurse and assign over 8 hours in one day
        Nurse n = svc.createNurse("testnurse", "pw");
        Shift longShift = new Shift(DayOfWeek.MONDAY, LocalTime.of(0, 0), LocalTime.of(10, 0));
        svc.assignShift(n.getId(), longShift);

        assertThrows(ComplianceException.class, svc::checkCompliance);
    }


    @Test
    void testUpdateStaffPassword() throws Exception {
        svc.login("manager", "password");

        Staff nurse = svc.createNurse("newnurse", "1234");
        svc.updateStaffPassword(nurse.getId(), "abcd");

        Staff logged = svc.login("newnurse", "abcd");
        assertNotNull(logged);
        assertEquals(nurse.getId(), logged.getId());
    }

    @Test
    void testDischargeRemovesResident() throws Exception {
        svc.login("manager", "password");

        Resident r = svc.addResident("Eve", Gender.FEMALE, false);
        svc.allocateResidentToBed(r.getId());

        svc.discharge(r.getId());
        assertFalse(svc.getResidents().contains(r));
    }
}
