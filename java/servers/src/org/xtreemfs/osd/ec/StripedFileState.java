/*
 * Copyright (c) 2015 by Jan Fajerski,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.ec;

import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.flease.Flease;
import org.xtreemfs.osd.RedundantFileState;
import org.xtreemfs.osd.rwre.ReplicaUpdatePolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;

/**
 * @author Jan Fajerski
 *
 * Store informtion about striped files. Involved OSDs, stripe characteristics, file size, lease aso.
 */
public class StripedFileState extends RedundantFileState {


    public StripedFileState(String fileID) {
        super(fileID);
    }

    @Override
    public XLocations getLocations() {
        return null;
    }

    @Override
    public ReplicaUpdatePolicy getPolicy() {
        return null;
    }

    @Override
    public StripingPolicyImpl getStripingPolicy() {
        return null;
    }
}
