/*
 * Copyright (c) 2008-2011 by Christian Lorenz,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.xloc;

import org.xtreemfs.osd.replication.transferStrategies.RandomStrategy;
import org.xtreemfs.osd.replication.transferStrategies.RarestFirstStrategy;
import org.xtreemfs.osd.replication.transferStrategies.SequentialPrefetchingStrategy;
import org.xtreemfs.osd.replication.transferStrategies.SequentialStrategy;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.REPL_FLAG;

/**
 * 
 * <br>
 * 14.07.2009
 */
public class ReplicationFlags {
    private static final int STRATEGY_BITS = SequentialStrategy.REPLICATION_FLAG.getNumber()
                                                   | RandomStrategy.REPLICATION_FLAG.getNumber()
                                                   | SequentialPrefetchingStrategy.REPLICATION_FLAG.getNumber()
                                                   | RarestFirstStrategy.REPLICATION_FLAG.getNumber();

    private static final int OTHER_BITS    = REPL_FLAG.REPL_FLAG_IS_COMPLETE.getNumber()
                                                   | REPL_FLAG.REPL_FLAG_FULL_REPLICA.getNumber();

    public static int setReplicaIsComplete(int flags) {
        return flags | REPL_FLAG.REPL_FLAG_IS_COMPLETE.getNumber();
    }

    public static int setReplicaIsNotComplete(int flags) {
        return flags & ~REPL_FLAG.REPL_FLAG_IS_COMPLETE.getNumber();
    }

    public static int setPartialReplica(int flags) {
        return flags & ~REPL_FLAG.REPL_FLAG_FULL_REPLICA.getNumber();
    }

    public static int setFullReplica(int flags) {
        return flags | REPL_FLAG.REPL_FLAG_FULL_REPLICA.getNumber();
    }

    public static int setRandomStrategy(int flags) {
        return resetStrategy(flags) | RandomStrategy.REPLICATION_FLAG.getNumber();
    }

    public static int setSequentialStrategy(int flags) {
        return resetStrategy(flags) | SequentialStrategy.REPLICATION_FLAG.getNumber();
    }

    public static int setSequentialPrefetchingStrategy(int flags) {
        return resetStrategy(flags) | SequentialPrefetchingStrategy.REPLICATION_FLAG.getNumber();
    }

    public static int setRarestFirstStrategy(int flags) {
        return resetStrategy(flags) | RarestFirstStrategy.REPLICATION_FLAG.getNumber();
    }

    public static boolean isReplicaComplete(int flags) {
        return (flags & REPL_FLAG.REPL_FLAG_IS_COMPLETE.getNumber()) == REPL_FLAG.REPL_FLAG_IS_COMPLETE.getNumber();
    }

    public static boolean isPartialReplica(int flags) {
        return (flags & REPL_FLAG.REPL_FLAG_FULL_REPLICA.getNumber()) == 0;
    }

    public static boolean isFullReplica(int flags) {
        return (flags & REPL_FLAG.REPL_FLAG_FULL_REPLICA.getNumber()) == REPL_FLAG.REPL_FLAG_FULL_REPLICA.getNumber();
    }

    public static boolean isRandomStrategy(int flags) {
        return resetOther(flags) == RandomStrategy.REPLICATION_FLAG.getNumber();
    }

    public static boolean isSequentialStrategy(int flags) {
        return resetOther(flags) == SequentialStrategy.REPLICATION_FLAG.getNumber();
    }

    public static boolean isSequentialPrefetchingStrategy(int flags) {
        return resetOther(flags) == SequentialPrefetchingStrategy.REPLICATION_FLAG.getNumber();
    }

    public static boolean isRarestFirstStrategy(int flags) {
        return resetOther(flags) == RarestFirstStrategy.REPLICATION_FLAG.getNumber();
    }

    public static boolean containsStrategy(int flags) {
        return (resetOther(flags) > 0);
    }

    public static int getStrategy(int flags) {
        return resetOther(flags);
    }

    /**
     * resets the bits used for strategies to zero
     */
    private static int resetStrategy(int flags) {
        return (flags | STRATEGY_BITS) ^ STRATEGY_BITS;
    }

    /**
     * resets the bits NOT used for strategies to zero
     */
    private static int resetOther(int flags) {
        return (flags | OTHER_BITS) ^ OTHER_BITS;
    }
}
