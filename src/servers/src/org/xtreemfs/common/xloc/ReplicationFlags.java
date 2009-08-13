/*  Copyright (c) 2009 Barcelona Supercomputing Center - Centro Nacional
    de Supercomputacion and Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

    This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
    Grid Operating System, see <http://www.xtreemos.eu> for more details.
    The XtreemOS project has been developed with the financial support of the
    European Commission's IST program under contract #FP6-033576.

    XtreemFS is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 2 of the License, or (at your option)
    any later version.

    XtreemFS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Christian Lorenz (ZIB)
 */
package org.xtreemfs.common.xloc;

import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.osd.replication.transferStrategies.RandomStrategy;
import org.xtreemfs.osd.replication.transferStrategies.SequentialPrefetchingStrategy;
import org.xtreemfs.osd.replication.transferStrategies.SequentialStrategy;

/**
 * 
 * <br>
 * 14.07.2009
 */
public class ReplicationFlags {
    public static int setReplicaIsComplete(int flags) {
        return flags | Constants.REPL_FLAG_IS_COMPLETE;
    }

    public static int setReplicaIsNotComplete(int flags) {
        return flags & ~Constants.REPL_FLAG_IS_COMPLETE;
    }

    public static int setPartialReplica(int flags) {
        return flags & ~Constants.REPL_FLAG_FULL_REPLICA;
    }

    public static int setFullReplica(int flags) {
        return flags | Constants.REPL_FLAG_FULL_REPLICA;
    }

    public static int setRandomStrategy(int flags) {
        return resetStrategy(flags) | RandomStrategy.REPLICATION_FLAG;
    }

    public static int setSequentialStrategy(int flags) {
        return resetStrategy(flags) | SequentialStrategy.REPLICATION_FLAG;
    }

    public static int setSequentialPrefetchingStrategy(int flags) {
        return resetStrategy(flags) | SequentialPrefetchingStrategy.REPLICATION_FLAG;
    }

    public static int setRarestFirstStrategy(int flags) {
        // TODO: when strategy is implemented: use correct flag
        return resetStrategy(flags) | RandomStrategy.REPLICATION_FLAG;
    }

    /**
     * resets the bits used for strategies to zero
     */
    private static int resetStrategy(int flags) {
        // TODO: when rarest first strategy is implemented: use correct flag
        int strategyBits = SequentialStrategy.REPLICATION_FLAG | RandomStrategy.REPLICATION_FLAG
                | SequentialPrefetchingStrategy.REPLICATION_FLAG;
        return (flags | strategyBits) ^ strategyBits;
    }

    public static boolean isReplicaComplete(int flags) {
        return (flags & Constants.REPL_FLAG_IS_COMPLETE) == Constants.REPL_FLAG_IS_COMPLETE;
    }

    public static boolean isPartialReplica(int flags) {
        return (flags & Constants.REPL_FLAG_FULL_REPLICA) == 0;
    }

    public static boolean isFullReplica(int flags) {
        return (flags & Constants.REPL_FLAG_FULL_REPLICA) == Constants.REPL_FLAG_FULL_REPLICA;
    }

    public static boolean isRandomStrategy(int flags) {
        return (flags & RandomStrategy.REPLICATION_FLAG) == RandomStrategy.REPLICATION_FLAG;
    }

    public static boolean isSequentialStrategy(int flags) {
        return (flags & SequentialStrategy.REPLICATION_FLAG) == SequentialStrategy.REPLICATION_FLAG;
    }

    public static boolean isSequentialPrefetchingStrategy(int flags) {
        return (flags & SequentialPrefetchingStrategy.REPLICATION_FLAG) == SequentialPrefetchingStrategy.REPLICATION_FLAG;
    }

    public static boolean isRarestFirstStrategy(int flags) {
        // TODO: when strategy is implemented: use correct flag
        return (flags & Constants.REPL_FLAG_STRATEGY_RANDOM) == Constants.REPL_FLAG_STRATEGY_RANDOM;
    }
}
