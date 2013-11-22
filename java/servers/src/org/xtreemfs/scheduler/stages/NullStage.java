/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *               
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.scheduler.stages;

/**
 * @author jensvfischer
 */
public class NullStage<Args, Callback> extends Stage<Args, Callback> {

    public NullStage(String stageName, int queueCapacity) {
        super(stageName, queueCapacity);
    }

    /**
     * Handles the actual execution of a stage request. Must be implemented by
     * all stages.
     *
     * @param stageRequest the stage method to execute
     */
    @Override
    protected void processMethod(StageRequest<Args, Callback> stageRequest) {
        System.out.println("processMethod in NullStage called");
    }
}
