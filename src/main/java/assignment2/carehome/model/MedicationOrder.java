package assignment2.carehome.model;

import java.time.LocalTime;
public record MedicationOrder(String medicine, double dose, String unit, LocalTime time) {}
