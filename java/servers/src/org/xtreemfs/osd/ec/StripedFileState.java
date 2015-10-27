/*
 * Copyright (c) 2015 by Jan Fajerski,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.ec;

import org.xtreemfs.foundation.flease.Flease;
import org.xtreemfs.osd.FileState;
import org.xtreemfs.osd.stages.Stage;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Jan Fajerski
 *
 * Store informtion about striped files. Involved OSDs, stripe characteristics, file size, lease aso.
 */
public class StripedFileState extends FileState {

    private FileCredentials             credentials;
    private String                      fileID;
    private Flease                      lease;

    public StripedFileState(String fileID) {
        super();
        this.fileID = fileID;
        this.lease = Flease.EMPTY_LEASE;
    }

    public FileCredentials getCredentials() {
        return credentials;
    }

    public void setCredentials(FileCredentials credentials) {
        this.credentials = credentials;
    }

}
