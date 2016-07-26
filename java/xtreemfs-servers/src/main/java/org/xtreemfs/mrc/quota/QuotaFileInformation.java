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
    private final long   filesize;

    private String       ownerId;
    private String       ownerGroupId;
    private int          replicaCount;

    /**
     * Copy constructor
     * 
     * @param quotaFileInformation
     */
    public QuotaFileInformation(QuotaFileInformation quotaFileInformation) {
        this(quotaFileInformation.getVolumeId(), quotaFileInformation.getFileId(), quotaFileInformation.getOwnerId(),
                quotaFileInformation.getOwnerGroupId(), quotaFileInformation.getFilesize(), quotaFileInformation
                        .getReplicaCount());
    }

    public QuotaFileInformation(String volumeId, FileMetadata fileMetadata) {
        this(volumeId, fileMetadata.getId(), fileMetadata.getOwnerId(), fileMetadata.getOwningGroupId(), fileMetadata
                .getSize(), fileMetadata.getXLocList().getReplicaCount());
    }

    public QuotaFileInformation(String volumeId, long fileId, String ownerId, String ownerGroupId, long filesize,
            int replicaCount) {
        this.volumeId = volumeId;
        this.fileId = fileId;
        this.ownerId = ownerId;
        this.ownerGroupId = ownerGroupId;
        this.filesize = filesize;
        this.replicaCount = replicaCount;
    }

    /**
     * {@link MRCHelper}
     * 
     * @return
     */
    public String getGlobalFileId() {
        return volumeId + ":" + fileId;
    }

    // Setter:

    /**
     * @param replicaCount
     *            the replicaCount to set
     */
    public void setReplicaCount(int replicaCount) {
        this.replicaCount = replicaCount;
    }

    /**
     * @param ownerId
     *            the ownerId to set
     */
    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    /**
     * @param ownerGroupId
     *            the ownerGroupId to set
     */
    public void setOwnerGroupId(String ownerGroupId) {
        this.ownerGroupId = ownerGroupId;
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

    /**
     * @return the replicaCount
     */
    public int getReplicaCount() {
        return replicaCount;
    }
}
