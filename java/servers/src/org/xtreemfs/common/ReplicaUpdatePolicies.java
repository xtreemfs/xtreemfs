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

    public static final String REPL_UPDATE_PC_NONE   = "";
    public static final String REPL_UPDATE_PC_RONLY  = "ronly";
    public static final String REPL_UPDATE_PC_WARONE = "WaR1";
    public static final String REPL_UPDATE_PC_WQRQ   = "WqRq";
    public static final String REPL_UPDATE_PC_EC     = "ec";

    @Deprecated // as of XtreemFS 1.3.1 and no longer allowed to set. Use WaR1 instead.
    public static final String REPL_UPDATE_PC_WARA   = "WaRa";

    /**
     * Return true if the read-only replication policy is set.
     */
    public static boolean isNONE(String replicaUpdatePolicy) {
        return (replicaUpdatePolicy.equals(REPL_UPDATE_PC_NONE));
    }

    /**
     * Return true if the read-only replication policy is set.
     */
    public static boolean isRO(String replicaUpdatePolicy) {
        return (replicaUpdatePolicy.equals(REPL_UPDATE_PC_RONLY));
    }

    /**
     * Returns true if a read-write replication policy is set.
     */
    public static boolean isRW(String replicaUpdatePolicy) {
        return (replicaUpdatePolicy.equals(REPL_UPDATE_PC_WARA) || replicaUpdatePolicy.equals(REPL_UPDATE_PC_WARONE)
                || replicaUpdatePolicy.equals(REPL_UPDATE_PC_WQRQ));
    }

    /**
     * Returns true if the erasure coding policy is set.
     */
    public static boolean isEC(String replicaUpdatePolicy) {
        return replicaUpdatePolicy.equals(REPL_UPDATE_PC_EC);
    }

}
