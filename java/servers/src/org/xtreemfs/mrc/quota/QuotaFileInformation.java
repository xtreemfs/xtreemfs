/*
 * Copyright (c) 2015 by Robert Bärhold, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.mrc.quota;

import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.utils.MRCHelper;

/**
 * Class to keep meta information used by the quota components.
 */
public class QuotaFileInformation {

    private final String volumeId;
    private final long   fileId;
    private final String ownerId;
    private final String ownerGroupId;
    private final long   filesize;

    public QuotaFileInformation(String volumeId, FileMetadata fileMetadata) {
        this(volumeId, fileMetadata.getId(), fileMetadata.getOwnerId(), fileMetadata.getOwningGroupId(), fileMetadata
                .getSize());
    }

    public QuotaFileInformation(String volumeId, long fileId, String ownerId, String ownerGroupId, long filesize) {
        this.volumeId = volumeId;
        this.fileId = fileId;
        this.ownerId = ownerId;
        this.ownerGroupId = ownerGroupId;
        this.filesize = filesize;
    }

    /**
     * {@link MRCHelper}
     * 
     * @return
     */
    public String getGlobalFileId() {
        return volumeId + ":" + fileId;
    }

    // Getter:

    /**
     * @return the volumeId
     */
    public String getVolumeId() {
        return volumeId;
    }

    /**
     * @return the fileId
     */
    public long getFileId() {
        return fileId;
    }

    /**
     * @return the ownerId
     */
    public String getOwnerId() {
        return ownerId;
    }

    /**
     * @return the ownerGroupId
     */
    public String getOwnerGroupId() {
        return ownerGroupId;
    }

    /**
     * @return the fileSize
     */
    public long getFilesize() {
        return filesize;
    }
}
