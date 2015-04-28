/*
 * Copyright (c) 2015 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import java.io.IOException;

import org.xtreemfs.common.libxtreemfs.exceptions.AddressToUUIDNotFoundException;
import org.xtreemfs.common.libxtreemfs.exceptions.PosixErrorException;
import org.xtreemfs.common.libxtreemfs.swig.FileHandleProxy;
import org.xtreemfs.common.libxtreemfs.swig.VolumeProxy;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;

public class AdminNativeVolume extends NativeVolume implements AdminVolume {
    private final AdminVolume adminVolume;

    public AdminNativeVolume(VolumeProxy proxy, AdminVolume adminVolume, String volumeName) {
        super(proxy, volumeName);
        this.adminVolume = adminVolume;
    }

    @Override
    public AdminFileHandle openFile(UserCredentials userCredentials, String path, int flags) throws IOException,
            PosixErrorException, AddressToUUIDNotFoundException {
        return openFile(userCredentials, path, flags, 0);
    }

    @Override
    public AdminNativeFileHandle openFile(UserCredentials userCredentials, String path, int flags, int mode)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        FileHandleProxy fileHandleProxy = proxy.openFileProxy(userCredentials, path, flags, mode);
        AdminFileHandle adminFileHandle = adminVolume.openFile(userCredentials, path, flags, mode);

        AdminNativeFileHandle fileHandleNative = new AdminNativeFileHandle(fileHandleProxy, adminFileHandle);
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

}
