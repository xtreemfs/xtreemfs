/*
 * Copyright (c) 2008-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.stages;

import org.xtreemfs.mrc.MRCRequest;


public interface MRCStageCallbackInterface {

    public void methodExecutionCompleted(MRCRequest request, MRCStage.StageResponseCode result);

}
