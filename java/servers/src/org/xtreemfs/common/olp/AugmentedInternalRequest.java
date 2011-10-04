/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.olp;


/**
 * <p>Stage-internal custom requests, that will not fall under overload-protection restrictions, but be considered for
 * performance measurements.</p>
 * 
 * @author fx.langner
 * @version 1.00, 09/27/11
 */
public final class AugmentedInternalRequest extends AugmentedRequest {

    /**
     * @param type
     * @param size
     */
    public AugmentedInternalRequest(int type, long size) {
        
        super(type);
        updateSize(size);
    }
    
    /**
     * @param type
     */
    public AugmentedInternalRequest(int type) {
        
        super(type);
    }
}