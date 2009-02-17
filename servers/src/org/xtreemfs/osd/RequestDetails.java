/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

 This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
 Grid Operating System, see <http://www.xtreemos.eu> for more details.
 The XtreemOS project has been developed with the financial support of the
 European Commission's IST program under contract #FP6-033576.

 XtreemFS is free software: you can redistribute it and/or modify it under
 the terms of the GNU General Public License as published by the Free
 Software Foundation, either version 2 of the License, or (at your option)
 any later version.

 XtreemFS is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Björn Kolbeck (ZIB), Jan Stender (ZIB)
 */

package org.xtreemfs.osd;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.ClientLease;
import org.xtreemfs.common.striping.Location;
import org.xtreemfs.common.striping.Locations;
import org.xtreemfs.osd.replication.TransferStrategy;
import org.xtreemfs.osd.replication.TransferStrategy.NextRequest;
import org.xtreemfs.osd.storage.CowPolicy;

public final class RequestDetails {

    protected String fileId;

    protected long objectNumber;

    protected boolean rangeRequested;

    protected long byteRangeStart;

    protected long byteRangeEnd;

    protected Capability capability;

    protected Locations locationList;

    protected Location currentReplica;

    protected boolean objectVersionNumberRequested;

    protected int objectVersionNumber;

    private long truncateFileSize;

    private String newFSandEpoch;

    private boolean checkOnly;

    private boolean invalidChecksum;

    /**
     * true, if the object doesn't exist on disk because read follows POSIX, you
     * won't notice that otherwise
     */
    private boolean objectNotExistsOnDisk = false;

    private ClientLease lease;

    private TransferStrategy replicationTransferStrategy;
    
    // FIXME
    public NextRequest nextReplicationStep;
    
    private String requestId;

    private CowPolicy cowPolicy;

    public String getFileId() {
	return fileId;
    }

    public void setFileId(String fileId) {
	this.fileId = fileId;
    }

    public long getObjectNumber() {
	return objectNumber;
    }

    public void setObjectNumber(long objectNumber) {
	this.objectNumber = objectNumber;
    }

    public boolean isRangeRequested() {
	return rangeRequested;
    }

    public void setRangeRequested(boolean rangeRequested) {
	this.rangeRequested = rangeRequested;
    }

    public long getByteRangeStart() {
	return byteRangeStart;
    }

    public void setByteRangeStart(long byteRangeStart) {
	this.byteRangeStart = byteRangeStart;
    }

    public long getByteRangeEnd() {
	return byteRangeEnd;
    }

    public void setByteRangeEnd(long byteRangeEnd) {
	this.byteRangeEnd = byteRangeEnd;
    }

    public Capability getCapability() {
	return capability;
    }

    public void setCapability(Capability capability) {
	this.capability = capability;
    }

    public Locations getLocationList() {
	return locationList;
    }

    public void setLocationList(Locations locationList) {
	this.locationList = locationList;
    }

    public Location getCurrentReplica() {
	return currentReplica;
    }

    public void setCurrentReplica(Location currentReplica) {
	this.currentReplica = currentReplica;
    }

    public long getTruncateFileSize() {
	return truncateFileSize;
    }

    public void setTruncateFileSize(long fileSize) {
	this.truncateFileSize = fileSize;
    }

    public boolean isObjectVersionNumberRequested() {
	return objectVersionNumberRequested;
    }

    public void setObjectVersionNumberRequested(
	    boolean objectVersionNumberRequested) {
	this.objectVersionNumberRequested = objectVersionNumberRequested;
    }

    public int getObjectVersionNumber() {
	return objectVersionNumber;
    }

    public void setObjectVersionNumber(int objectVersionNumber) {
	this.objectVersionNumber = objectVersionNumber;
    }

    public String getNewFSandEpoch() {
	return newFSandEpoch;
    }

    public void setNewFSandEpoch(String newFSandEpoch) {
	this.newFSandEpoch = newFSandEpoch;
    }

    public boolean isCheckOnly() {
	return checkOnly;
    }

    public void setCheckOnly(boolean checkOnly) {
	this.checkOnly = checkOnly;
    }

    public boolean isInvalidChecksum() {
	return invalidChecksum;
    }

    public void setInvalidChecksum(boolean invalidChecksum) {
	this.invalidChecksum = invalidChecksum;
    }

    public ClientLease getLease() {
	return lease;
    }

    public void setLease(ClientLease lease) {
	this.lease = lease;
    }

    public TransferStrategy getReplicationTransferStrategy() {
	return this.replicationTransferStrategy;
    }

    public void setReplicationTransferStrategy(TransferStrategy strategy) {
	if (this != null)
	    this.replicationTransferStrategy = strategy;
	// TODO: throw exception for additional set
    }

    public String getRequestId() {
	return requestId;
    }

    public void setRequestId(String requestId) {
	this.requestId = requestId;
    }

    public CowPolicy getCowPolicy() {
	return cowPolicy;
    }

    public void setCowPolicy(CowPolicy cowPolicy) {
	this.cowPolicy = cowPolicy;
    }

    /**
     * @param objectExistsOnDisk
     *            the objectExistsOnDisk to set
     */
    public void setObjectNotExistsOnDisk(boolean notExisting) {
	this.objectNotExistsOnDisk = notExisting;
    }

    /**
     * @return the objectExistsOnDisk
     */
    public boolean isObjectNotExistingOnDisk() {
	return objectNotExistsOnDisk;
    }
}
