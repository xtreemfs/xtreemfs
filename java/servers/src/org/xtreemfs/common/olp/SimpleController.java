/*
 * Copyright (c) 2012 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.olp;

/**
 * <p>Methods of this class are not thread safe, because processing of a request is assumed to 
 * be single threaded. For internal nodes of the processing tree.</p>
 * 
 * @author fx.langner
 * @version 1.01, 08/25/11
 */
class SimpleController extends Controller {

    /**
     * @param numTypes
     * @param numInternalTypes
     * @param numSubsequentStages
     */
    SimpleController(int numTypes, int numInternalTypes, int numSubsequentStages) {
        super(numTypes, numInternalTypes, numSubsequentStages);
    }
}
