/*
 * Copyright (c) 2013 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.mrc.stages;

import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.mrc.operations.AddReplicaOperation;

/**
 * This callback will be executed, when the {@link XLocSetCoordinator} ensured the data is updated on a sufficient
 * number of replicas and the new xLocList can be installed without inconsistencies.<br>
 * The callback is intended to be implemented by an operation such as {@link AddReplicaOperation}.
 */
public interface XLocSetCoordinatorCallback {
    public void installXLocSet(String fileId, XLocList newXLocList, XLocList prevXLocList) throws Throwable;

    public void handleInstallXLocSetError(Throwable error, String fileId, XLocList newXLocList, XLocList prevXLocList)
            throws Throwable;
}
