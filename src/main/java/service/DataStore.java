
package service;

import model.*;
import java.io.*;
import java.util.*;

public class DataStore implements Serializable {
    public Map<String, Staff> staff = new HashMap<>();
    public Map<String, Resident> residents = new HashMap<>();
    public List<Ward> wards = new ArrayList<>();

    public static void save(DataStore ds, String file) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(ds);
        }
    }
    public static DataStore load(String file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (DataStore) ois.readObject();
        }
    }
}
