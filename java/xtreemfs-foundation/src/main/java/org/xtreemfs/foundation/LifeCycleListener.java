/*
 * Copyright (c) 2010 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation;

/**
 * Notifies a process of a life cycle event.
 *
 * @author stender
 *
 */
public interface LifeCycleListener {

    public void startupPerformed();

    public void shutdownPerformed();

    public void crashPerformed(Throwable cause);

}
