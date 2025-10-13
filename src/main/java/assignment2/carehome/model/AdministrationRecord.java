package assignment2.carehome.model;

import java.time.LocalDateTime;

public record AdministrationRecord(String residentId, String prescriptionId, String medicine,
                                   double dose, String unit, LocalDateTime time, String staffId) {}
