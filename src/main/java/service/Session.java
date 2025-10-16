
package service;

import model.Staff;

public class Session {
    private static final Session INSTANCE = new Session();
    private Staff currentUser;
    private Session(){}
    public static Session get(){ return INSTANCE; }
    public void setCurrentUser(Staff s){ currentUser = s; }
    public Staff getCurrentUser(){ return currentUser; }
}
