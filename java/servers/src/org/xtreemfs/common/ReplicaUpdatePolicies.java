/*
 * Copyright (c) 2010 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common;

/**
 *
 * @author bjko
 */
public class ReplicaUpdatePolicies {

    public static final String REPL_UPDATE_PC_NONE = "";
    public static final String REPL_UPDATE_PC_RONLY = "ronly";
    public static final String REPL_UPDATE_PC_WARONE = "WaR1";
    public static final String REPL_UPDATE_PC_WARA = "WaRa";  // @deprecated as of XtreemFS 1.3.1 and no longer allowed to set. Use WaR1 instead.
    public static final String REPL_UPDATE_PC_WQRQ = "WqRq";
    
}
