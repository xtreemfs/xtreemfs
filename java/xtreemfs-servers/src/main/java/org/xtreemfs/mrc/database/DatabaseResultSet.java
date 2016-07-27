/*
 * Copyright (c) 2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.database;

import java.util.Iterator;

/**
 * A result set for MRC database queries.
 * 
 * @author stender
 *
 * @param <T>
 */
public interface DatabaseResultSet<T> extends Iterator<T> {
    
    /**
     * Frees all resources attached to the result set.
     */
    public void destroy();
    
}
