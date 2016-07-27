/*
 * Copyright (c) 2008-2011 by Christian Lorenz, Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc;

import java.util.List;
import java.util.Map;

import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;

/**
 * 
 * 29.09.2008
 * 
 * @author clorenz
 */
public final class RequestDetails {
    
    public String              userId;
    
    public boolean             superUser;
    
    public List<String>        groupIds;
    
    public Auth                auth;
    
    public String              password;
    
    public Map<String, Object> context;
    
    /**
	 *
	 */
    public RequestDetails() {
        userId = null;
    }
}
