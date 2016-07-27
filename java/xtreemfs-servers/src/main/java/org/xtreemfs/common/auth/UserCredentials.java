/*
 * Copyright (c) 2008 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.auth;

import java.util.List;

/**
 * User credentials.
 * @author bjko
 */
public class UserCredentials {
    protected String userID;
    protected List<String> groupIDs;
    protected boolean superUser;

    public UserCredentials(String userID,List<String> groupIDs, boolean superUser) {
        this.userID = userID;
        this.groupIDs = groupIDs;
        this.superUser = superUser;
    }
    
    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public List<String> getGroupIDs() {
        return groupIDs;
    }

    public void setGroupIDs(List<String> groupIDs) {
        this.groupIDs = groupIDs;
    }

    public boolean isSuperUser() {
        return superUser;
    }

    public void setSuperUser(boolean superUser) {
        this.superUser = superUser;
    }
            
            
}
