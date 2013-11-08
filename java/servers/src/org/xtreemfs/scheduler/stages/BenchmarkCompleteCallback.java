/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.scheduler.stages;

import org.xtreemfs.scheduler.data.OSDPerformanceDescription;

/**
 * The interface for the benchmark callback class for {@link Stage.StageRequest}.
 *
 * @author jensvfischer
 */
public interface BenchmarkCompleteCallback {

    /**
     * Callback for when the measurement of the OSD was successfully completed.
     *
     * @param perfDescription the performance description of the OSD
     */
    public void benchmarkComplete(OSDPerformanceDescription perfDescription);

    /**
     * Callback for when the measurement finally (i.e. more often the the maximum number of retries) failed.
     *
     * @param error the error with which the last benchmark failed.
     */
    public void benchmarkFailed(Throwable error);
}
