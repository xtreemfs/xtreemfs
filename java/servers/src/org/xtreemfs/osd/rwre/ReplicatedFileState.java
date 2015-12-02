/*
 * Copyright (c) 2010-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.rwre;

import java.io.IOException;
import java.util.List;

import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.osd.RedundantFileState;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectVersionMapping;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;

/**
 *
 * @author bjko
 */
public class ReplicatedFileState extends RedundantFileState {

    private List<ObjectVersionMapping> objectsToFetch;

    public ReplicatedFileState(String fileId, XLocations locations, ServiceUUID localUUID,
                               OSDServiceClient client) throws UnknownUUIDException, IOException {
        super(fileId, locations, localUUID, client);
    }

    /**
     * @return the objectsToFetch
     */
    public List<ObjectVersionMapping> getObjectsToFetch() {
        return objectsToFetch;
    }

    /**
     * @param objectsToFetch the objectsToFetch to set
     */
    public void setObjectsToFetch(List<ObjectVersionMapping> objectsToFetch) {
        this.objectsToFetch = objectsToFetch;
    }
}
