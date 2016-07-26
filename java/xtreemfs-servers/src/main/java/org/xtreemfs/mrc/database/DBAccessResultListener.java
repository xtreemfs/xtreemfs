/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.database;

public interface DBAccessResultListener<T> {
    
    public void finished(T result, Object context);

    public void failed(Throwable error, Object context);
}
