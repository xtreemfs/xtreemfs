/*
 * Copyright (c) 2015 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs.jni;

import java.io.IOException;

import org.xtreemfs.common.libxtreemfs.AdminFileHandle;
import org.xtreemfs.common.libxtreemfs.AdminVolume;
import org.xtreemfs.common.libxtreemfs.exceptions.AddressToUUIDNotFoundException;
import org.xtreemfs.common.libxtreemfs.exceptions.PosixErrorException;
import org.xtreemfs.common.libxtreemfs.jni.generated.FileHandleProxy;
import org.xtreemfs.common.libxtreemfs.jni.generated.VolumeProxy;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.XATTR_FLAGS;

public class NativeAdminVolume extends NativeVolume implements AdminVolume {
    private final AdminVolume adminVolume;

    public NativeAdminVolume(NativeAdminClient client, VolumeProxy proxy, AdminVolume adminVolume, String volumeName) {
        super(client, proxy, volumeName);
        this.adminVolume = adminVolume;
    }

    @Override
    public AdminFileHandle openFile(UserCredentials userCredentials, String path, int flags) throws IOException,
            PosixErrorException, AddressToUUIDNotFoundException {
        return openFile(userCredentials, path, flags, 0);
    }

    @Override
    public NativeAdminFileHandle openFile(UserCredentials userCredentials, String path, int flags, int mode)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        FileHandleProxy fileHandleProxy = proxy.openFileProxy(userCredentials, path, flags, mode);
        AdminFileHandle adminFileHandle = adminVolume.openFile(userCredentials, path, flags, mode);

        NativeAdminFileHandle fileHandleNative = new NativeAdminFileHandle(fileHandleProxy, adminFileHandle);
        return fileHandleNative;
    }

    @Override
    public long getNumObjects(UserCredentials userCredentials, String path) throws IOException {
        return adminVolume.getNumObjects(userCredentials, path);
    }

    @Override
    public void unlink(UserCredentials userCredentials, String path, boolean unlinkOnlyAtMrc) throws IOException,
            PosixErrorException, AddressToUUIDNotFoundException {
        adminVolume.unlink(userCredentials, path, unlinkOnlyAtMrc);
    }

    @Override
    public void setXAttr(UserCredentials userCredentials, Auth auth, String path, String name, String value,
            XATTR_FLAGS flags) throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        adminVolume.setXAttr(userCredentials, auth, path, name, value, flags);
    }

}
