/*
 * Copyright (c) 2013 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.mrc.stages;

/**
 * Concurrent changes to a files' xLocSet, will be prevented by a lock stored in the database (see also
 * {@link XLocSetCoordinator#lockXLocSet}). If the MRC failed while a xLocSet change was in progress, a crash recovery
 * routine has to be started to revalidate replicas. <br>
 * This class is a simple wrapper to hold the information if a xLocSet is locked and if the lock has been acquired by
 * the current instance or if it is a leftover from a previously crashed one.
 */
public class XLocSetLock {

    final boolean locked;
    final boolean crashed;

    public XLocSetLock(boolean locked, boolean crashed) {
        this.locked = locked;
        this.crashed = crashed;
    }

    public boolean isLocked() {
        return locked;
    }

    public boolean hasCrashed() {
        return crashed;
    }
}
