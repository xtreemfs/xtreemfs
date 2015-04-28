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
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Stat;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.Lock;

public class NativeFileHandle implements FileHandle {

    protected final FileHandleProxy proxy;

    public NativeFileHandle(FileHandleProxy proxy) {
        this.proxy = proxy;
    }

    @Override
    public int read(UserCredentials userCredentials, byte[] data, int count, long offset) throws IOException,
            PosixErrorException, AddressToUUIDNotFoundException {
        // UserCredentials are not used internally.
        return proxy.read(data, count, offset);
    }

    @Override
    @Deprecated
    public int read(UserCredentials userCredentials, byte[] data, int dataOffset, int count, long offset)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        throw new RuntimeException("dataOffset param not supported in C++");
    }

    @Override
    public int write(UserCredentials userCredentials, byte[] data, int count, long offset) throws IOException,
            PosixErrorException, AddressToUUIDNotFoundException {
        // UserCredentials are not used internally.
        return proxy.write(data, count, offset);
    }

    @Override
    @Deprecated
    public int write(UserCredentials userCredentials, byte[] data, int dataOffset, int count, long offset)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        throw new RuntimeException("dataOffset param not supported in C++");
    }

    @Override
    public void flush() throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        proxy.flush();
    }

    @Override
    public void truncate(UserCredentials userCredentials, long newFileSize) throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException {
        proxy.truncate(userCredentials, newFileSize);
    }

    @Override
    public Stat getAttr(UserCredentials userCredentials) throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException {
        return proxy.getAttr(userCredentials);
    }

    @Override
    public Lock acquireLock(UserCredentials userCredentials, int processId, long offset, long length,
            boolean exclusive, boolean waitForLock) throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException {
        // UserCredentials are not used internally.
        return proxy.acquireLock(processId, offset, length, exclusive, waitForLock);
    }

    @Override
    public Lock checkLock(UserCredentials userCredentials, int processId, long offset, long length, boolean exclusive)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        // UserCredentials are not used internally.
        return proxy.checkLock(processId, offset, length, exclusive);
    }

    @Override
    public void releaseLock(UserCredentials userCredentials, int processId, long offset, long length, boolean exclusive)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        // UserCredentials are not used internally.
        proxy.releaseLock(processId, offset, length, exclusive);
    }

    @Override
    public void releaseLock(UserCredentials userCredentials, Lock lock) throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException {
        // UserCredentials are not used internally.
        proxy.releaseLock(lock);
    }

    @Override
    public void releaseLockOfProcess(int processId) throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException {
        proxy.releaseLockOfProcess(processId);
    }

    @Override
    public void pingReplica(UserCredentials userCredentials, String osdUuid) throws IOException,
            AddressToUUIDNotFoundException {
        // UserCredentials are not used internally.
        proxy.pingReplica(osdUuid);
    }

    @Override
    public void close() throws IOException {
        proxy.close();
    }

}
