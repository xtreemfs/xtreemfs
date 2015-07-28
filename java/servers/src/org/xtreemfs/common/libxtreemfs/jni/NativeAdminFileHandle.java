/*
 * Copyright (c) 2015 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs.jni;

import java.io.IOException;
import java.util.List;

import org.xtreemfs.common.clients.InvalidChecksumException;
import org.xtreemfs.common.libxtreemfs.AdminFileHandle;
import org.xtreemfs.common.libxtreemfs.exceptions.AddressToUUIDNotFoundException;
import org.xtreemfs.common.libxtreemfs.exceptions.PosixErrorException;
import org.xtreemfs.common.libxtreemfs.jni.generated.FileHandleProxy;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;

public class NativeAdminFileHandle extends NativeFileHandle implements AdminFileHandle {

    private final AdminFileHandle adminFileHandle;

    public NativeAdminFileHandle(FileHandleProxy proxy, AdminFileHandle adminFileHandle) {
        super(proxy);
        this.adminFileHandle = adminFileHandle;
    }

    @Override
    public void close() throws IOException {
        super.close();
        adminFileHandle.close();
    }

    @Override
    public List<Replica> getReplicasList() {
        return adminFileHandle.getReplicasList();
    }

    @Override
    public Replica getReplica(int replicaIndex) {
        return adminFileHandle.getReplica(replicaIndex);
    }

    @Override
    public StripingPolicy getStripingPolicy() {
        return adminFileHandle.getStripingPolicy();
    }

    @Override
    public StripingPolicy getStripingPolicy(int replicaIndex) {
        return adminFileHandle.getStripingPolicy(replicaIndex);
    }

    @Override
    public String getReplicaUpdatePolicy() {
        return adminFileHandle.getReplicaUpdatePolicy();
    }

    @Override
    public String getGlobalFileId() {
        return adminFileHandle.getGlobalFileId();
    }

    @Override
    public boolean checkAndMarkIfReadOnlyReplicaComplete(int replicaIndex, UserCredentials userCredentials)
            throws IOException, AddressToUUIDNotFoundException {
        return adminFileHandle.checkAndMarkIfReadOnlyReplicaComplete(replicaIndex, userCredentials);
    }

    @Override
    public long getNumObjects(UserCredentials userCredentials) throws IOException {
        return adminFileHandle.getNumObjects(userCredentials);
    }

    @Override
    public int checkObjectAndGetSize(int replicaIndex, long objectNo) throws IOException, InvalidChecksumException {
        return adminFileHandle.checkObjectAndGetSize(replicaIndex, objectNo);
    }

    @Override
    public void repairObject(int replicaIndex, long objectNo) throws IOException {
        adminFileHandle.repairObject(replicaIndex, objectNo);
    }

    @Override
    public long getSizeOnOSD() throws IOException {
        return adminFileHandle.getSizeOnOSD();
    }

    @Override
    public void truncate(UserCredentials userCredentials, long newFileSize, boolean truncateOnlyAtMRC)
            throws PosixErrorException, AddressToUUIDNotFoundException, IOException {
        adminFileHandle.truncate(userCredentials, newFileSize, truncateOnlyAtMRC);
    }

}
