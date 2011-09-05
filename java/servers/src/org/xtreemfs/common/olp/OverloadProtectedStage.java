/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.olp;

import org.xtreemfs.common.stage.Stage;

/**
 * <p>Stage that is protected by overload control.</p>
 * 
 * @author flangner
 * @version 1.00, 08/31/11
 * @see OverloadProtection
 */
public abstract class OverloadProtectedStage<R extends IRequest> extends Stage<R> {

    /**
     * <p>Reference to the overload-protection algorithm core.</p>
     */
    private final OverloadProtection olp;
    
    public OverloadProtectedStage(String name, int identifier, int numTypes) {
        this(name, new OverloadProtection(identifier, numTypes));
    }
    
    /**
     * <p>Private constructor initializing a stage with the given name and the already initialized Overload-protection 
     * algorithm.</p>
     * 
     * @param name - of the stage.
     * @param olp - the initialized algorithm.
     */
    private OverloadProtectedStage(String name, OverloadProtection olp) {
        super(name, new SimpleProtectedQueue<R>(olp));
        this.olp = olp;
    }
}