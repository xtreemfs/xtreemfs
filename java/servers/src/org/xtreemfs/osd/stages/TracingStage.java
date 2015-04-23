/*
 * Copyright (c) 2008-2015 by Christoph Kleineweber,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.stages;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.tracing.TracingPolicy;

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
        try {
            short policyId = getPolicyId(method);
            TracingPolicy policy = getPolicy(policyId);
            policy.traceRequest(method.getRequest());
        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        }
    }

    public void traceRequest(OSDRequest req) {
        this.enqueueOperation(req.getOperation().getProcedureId(), new Object[]{}, req, null);
    }

    private TracingPolicy getPolicy(short id) throws Exception {
        return master.getPolicyContainer().getTracingPolicy(id);
    }

    private short getPolicyId(StageRequest method) {
        short result = 0;
        if(method.getRequest().getCapability().getTraceConfig().hasTracingPolicy()) {
            result = Short.valueOf(method.getRequest().getCapability().getTraceConfig().getTracingPolicy());
        }
        return result;
    }
}
