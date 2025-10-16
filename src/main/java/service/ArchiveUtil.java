
package service;

import model.*;

import java.io.FileWriter;
import java.io.IOException;

public class ArchiveUtil {
    public static void archiveResident(Resident r) throws IOException {
        try (FileWriter fw = new FileWriter("archive_"+r.getId()+".csv")) {
            fw.write("Resident,"+r.getId()+","+r.getName()+","+r.getGender()+",isolation="+r.isIsolation()+"\n");
            for (Prescription p: r.getPrescriptions()){
                fw.write("Prescription,"+p.getId()+","+p.getDoctorId()+","+p.getCreatedAt()+"\n");
                for (MedicationOrder o: p.getOrders()){
                    fw.write("MedicationOrder,"+o.getMedicine()+","+o.getDose()+","+o.getTime()+"\n");
                }
            }
            for (AdministrationRecord a: r.getAdministrations()){
                fw.write("Administration,"+a.getMedicine()+","+a.getDose()+","+a.getAt()+","+a.getNurseId()+"\n");
            }
        }
    }
}
