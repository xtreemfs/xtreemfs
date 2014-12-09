/*
 * Copyright (c) 2008-2014 by Christoph Kleineweber,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.stages;

import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;

/**
 * @author Christoph Kleineweber <kleineweber@zib.de>
 */
public class TracingStage extends Stage {
    private OSDRequestDispatcher master;

    public TracingStage(OSDRequestDispatcher master, int queueCapacity) {
        super("OSD Tracing Stage", queueCapacity);
        this.master = master;
    }

    @Override
    protected void processMethod(StageRequest method) {
        switch(method.getStageMethod()) {
        }
    }

    public void traceRequest(OSDRequest req) {
        this.enqueueOperation(req.getOperation().getProcedureId(), new Object[]{}, req, null);
    }
}
