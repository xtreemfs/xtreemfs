/*
 * Copyright (c) ${year} by Jan Fajerski,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd;

import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC;

/**
 * @author Jan Fajerski
 */

public interface FailableFileOperationCallback {
    void failed(RPC.RPCHeader.ErrorResponse ex);
}
