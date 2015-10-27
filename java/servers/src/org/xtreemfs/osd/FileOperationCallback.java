/*
 * Copyright (c) ${year} by Jan Fajerski,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd;

import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;

/**
 * @author Jan Fajerski
 */
public interface FileOperationCallback {
    void success(long newObjectVersion);
    void redirect(String redirectTo);
    void failed(ErrorResponse ex);
}
