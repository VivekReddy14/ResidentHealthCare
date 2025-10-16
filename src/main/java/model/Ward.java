
package model;

import java.io.Serializable;
import java.util.*;

public class Ward implements Serializable {
    private final String id; private final boolean malePreferred;
    private final List<Bed> beds = new ArrayList<>();
    public Ward(String id, boolean malePreferred){ this.id=id; this.malePreferred=malePreferred; }
    public String getId(){ return id; }
    public boolean isMalePreferred(){ return malePreferred; }
    public List<Bed> getBeds(){ return beds; }
    public void addBed(Bed b){ beds.add(b); }
    public Optional<Bed> findEmpty(){ return beds.stream().filter(Bed::isEmpty).findFirst(); }
}
