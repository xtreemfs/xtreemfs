package org.xtreemfs.common.libxtreemfs;

import java.io.IOException;

import org.xtreemfs.common.libxtreemfs.exceptions.AddressToUUIDNotFoundException;
import org.xtreemfs.common.libxtreemfs.exceptions.PosixErrorException;
import org.xtreemfs.common.libxtreemfs.swig.FileHandleProxy;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.Lock;

public class FileHandleNative extends FileHandleProxy implements FileHandle {

    public FileHandleNative(FileHandleProxy f) {
        super(FileHandleProxy.getCPtr(f), false);
    }

    @Override
    public int read(UserCredentials userCredentials, byte[] data, int count, long offset) throws IOException,
            PosixErrorException, AddressToUUIDNotFoundException {
        // UserCredentials are not used internally.
        return read(data, count, offset);
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
        return write(data, count, offset);
    }

    @Override
    @Deprecated
    public int write(UserCredentials userCredentials, byte[] data, int dataOffset, int count, long offset)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        throw new RuntimeException("dataOffset param not supported in C++");
    }

    @Override
    public void pingReplica(UserCredentials userCredentials, String osdUuid) throws IOException,
            AddressToUUIDNotFoundException {
        // UserCredentials are not used internally.
        pingReplica(osdUuid);
    }

    @Override
    public Lock acquireLock(UserCredentials userCredentials, int processId, long offset, long length,
            boolean exclusive, boolean waitForLock) throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException {
        // UserCredentials are not used internally.
        return acquireLock(processId, offset, length, exclusive, waitForLock);
    }

    @Override
    public Lock checkLock(UserCredentials userCredentials, int processId, long offset, long length, boolean exclusive)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        // UserCredentials are not used internally.
        return checkLock(processId, offset, length, exclusive);
    }

    @Override
    public void releaseLock(UserCredentials userCredentials, int processId, long offset, long length, boolean exclusive)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        // UserCredentials are not used internally.
        releaseLock(processId, offset, length, exclusive);
    }

    @Override
    public void releaseLock(UserCredentials userCredentials, Lock lock) throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException {
        // UserCredentials are not used internally.
        releaseLock(lock);
    }
}
