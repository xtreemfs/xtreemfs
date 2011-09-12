/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.stage;

/**
 * <p>Callback for postprocessing a {@link Request}.</p>
 * 
 * @author fx.langner
 * @version 1.00, 09/03/11
 */
public interface Callback {
    
    /**
     * <p>Method that is called if execution of a request failed because of <code>error</code>.</p>
     * 
     * @param error - reason for the failure.
     */
    public void failed(Exception error);
}
