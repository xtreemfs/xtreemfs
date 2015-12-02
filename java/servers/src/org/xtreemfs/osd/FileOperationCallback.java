/*
 * Copyright (c) ${year} by Jan Fajerski,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd;

public interface FileOperationCallback extends FailableFileOperationCallback{
    void success(long newObjectVersion);
    void redirect(String redirectTo);
}
