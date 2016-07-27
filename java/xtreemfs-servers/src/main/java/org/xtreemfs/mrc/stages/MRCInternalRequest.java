/*
 * Copyright (c) 2015 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.mrc.stages;

public class MRCInternalRequest {

    private final Object[] args;

    public MRCInternalRequest(Object[] args) {
        this.args = args;
    }

    public Object[] getArgs() {
        return args;
    }

}
