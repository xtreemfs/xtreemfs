/*
 * Copyright (c) 2008-2015 by Christoph Kleineweber,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.tracing;

import org.xtreemfs.osd.OSDRequest;

/**
 * @author Christoph Kleineweber <kleineweber@zib.de>
 */
public interface TracingPolicy {
    public int getId();
    public TraceInfo extractTraceInfo(OSDRequest req);
    public String logTraceInfo(TraceInfo info);
    public String getTargetPath(TraceInfo info);
}
