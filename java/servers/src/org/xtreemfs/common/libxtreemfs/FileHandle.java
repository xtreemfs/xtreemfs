/*
 * Copyright (c) 2008-2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import java.io.IOException;

import org.xtreemfs.common.libxtreemfs.exceptions.AddressToUUIDNotFoundException;
import org.xtreemfs.common.libxtreemfs.exceptions.PosixErrorException;
import org.xtreemfs.common.libxtreemfs.exceptions.UUIDNotInXlocSetException;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Stat;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.Lock;

/**
 * FileHandle represents an open file.
 */
public interface FileHandle {

    /**
     * Read from a file 'count' bytes starting at 'offset' into 'buf'.
     * 
     * @param userCredentials
     *            Name and Groups of the user.
     * @param data
     *            [out] Byte array to be filled with read data.
     * @param count
     *            Number of requested bytes.
     * @param offset
     *            Offset in bytes.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws IOException
     * @throws PosixErrorException
     * 
     * @return Number of bytes read.
     */
    public int read(UserCredentials userCredentials, byte[] data, int count, long offset)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException;

    /**
     * Read from a file 'count' bytes starting at 'offset' into 'buf'.
     *
     * @param userCredentials
     *            Name and Groups of the user.
     * @param data
     *            [out] Byte array to be filled with read data.
     * @param dataOffset
     *            Offset in data array. This is the position of the first bytes in the data array that should
     *            be read.
     * @param count
     *            Number of requested bytes.
     * @param offset
     *            Offset in bytes. At this position in the file the data will be read.
     *
     * @throws AddressToUUIDNotFoundException
     * @throws IOException
     * @throws PosixErrorException
     *
     * @return Number of bytes read.
     */
    public int read(UserCredentials userCredentials, byte[] data, int dataOffset, int count, long offset)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException;

    /**
     * Write to a file 'count' bytes at file offset 'offset' from 'buf'.
     * 
     * @attention If asynchronous writes are enabled (which is the default unless the file was opened with
     *            O_SYNC or async writes were disabled globally), no possible write errors can be returned as
     *            write() does return immediately after putting the write request into the send queue instead
     *            of waiting until the result was received. In this case, only after calling flush() or
     *            close() occurred write errors are returned to the user.
     * 
     * @param userCredentials
     *            Name and Groups of the user.
     * @param data
     *            [] [in] Byte array which contains data to be written.
     * @param count
     *            Number of bytes to be written from buf.
     * @param offset
     *            Offset in bytes.
     * 
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws IOException
     * @throws PosixErrorException
     * 
     * @return Number of bytes written (see @attention above).
     */
    public int write(UserCredentials userCredentials, byte[] data, int count, long offset)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException;

    /**
     * Write to a file 'count' bytes at file offset 'offset' from 'buf'.
     * 
     * @attention If asynchronous writes are enabled (which is the default unless the file was opened with
     *            O_SYNC or async writes were disabled globally), no possible write errors can be returned as
     *            write() does return immediately after putting the write request into the send queue instead
     *            of waiting until the result was received. In this case, only after calling flush() or
     *            close() occurred write errors are returned to the user.
     * 
     * @param userCredentials
     *            Name and Groups of the user.
     * 
     * @param data
     *            [] [in] Byte array which contains data to be written.
     * @param dataOffset
     *            Offset in data array. This is the position of the first bytes in the data array that should
     *            be written.
     * @param count
     *            Number of bytes to be written from buf.
     * @param offset
     *            Offset in bytes. At this position in the file the data will be written.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws IOException
     * @throws PosixErrorException
     * 
     * @return Number of bytes written (see @attention above).
     */
    public int write(UserCredentials userCredentials, byte[] data, int dataOffset, int count,
            long offset) throws IOException, PosixErrorException, AddressToUUIDNotFoundException;

    /**
     * Flushes pending writes and file size updates (corresponds to a fsync() system call).
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws IOException
     * @throws PosixErrorException
     */
    public void flush() throws IOException, PosixErrorException, AddressToUUIDNotFoundException;

    /**
     * Truncates the file to "newFileSize_ bytes".
     * 
     * @param userCredentials
     *            Name and Groups of the user.
     * @param newFileSize
     *            New size of the file.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws IOException
     * @throws PosixErrorException
     **/
    public void truncate(UserCredentials userCredentials, long newFileSize) throws IOException,
            PosixErrorException, AddressToUUIDNotFoundException;

    /**
     * Retrieve the attributes of this file and writes the result in "stat".
     * 
     * @param userCredentials
     *            Name and Groups of the user.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws IOException
     * @throws PosixErrorException
     */
    public Stat getAttr(UserCredentials userCredentials) throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException;

    /**
     * Sets a lock on the specified file region and returns the resulting Lock object.
     * 
     * If the acquisition of the lock fails, PosixErrorException will be thrown and posix_errno() will return
     * POSIX_ERROR_EAGAIN.
     * 
     * @param userCredentials
     *            Name and Groups of the user.
     * @param processId
     *            ID of the process to which the lock belongs.
     * @param offset
     *            Start of the region to be locked in the file.
     * @param length
     *            Length of the region.
     * @param exclusive
     *            shared/read lock (false) or write/exclusive (true)?
     * @param waitForLock
     *            if true, blocks until lock acquired.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws IOException
     * @throws PosixErrorException
     * 
     * @remark Ownership is transferred to the caller.
     */
    public Lock acquireLock(UserCredentials userCredentials, int processId, long offset,
            long length, boolean exclusive, boolean waitForLock) throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException;

    /**
     * Checks if the requested lock does not result in conflicts. If true, the returned Lock object contains
     * the requested 'process_id' in 'client_pid', otherwise the Lock object is a copy of the conflicting
     * lock.
     * 
     * @param userCredentials
     *            Name and Groups of the user.
     * @param processId
     *            ID of the process to which the lock belongs.
     * @param offset
     *            Start of the region to be locked in the file.
     * @param length
     *            Length of the region.
     * @param exclusive
     *            shared/read lock (false) or write/exclusive (true)?
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws IOException
     * @throws PosixErrorException
     * 
     * @remark Ownership is transferred to the caller.
     */
    public Lock checkLock(UserCredentials userCredentials, int processId, long offset, long length,
            boolean exclusive) throws IOException, PosixErrorException, AddressToUUIDNotFoundException;

    /**
     * Releases "lock".
     * 
     * @param userCredentials
     *            Name and Groups of the user.
     * @param processId
     *            ID of the process to which the lock belongs.
     * @param offset
     *            Start of the region to be locked in the file.
     * @param length
     *            Length of the region.
     * @param exclusive
     *            shared/read lock (false) or write/exclusive (true)?
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws IOException
     * @throws PosixErrorException
     */
    public void releaseLock(UserCredentials userCredentials, int processId, long offset,
            long length, boolean exclusive) throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException;

    /**
     * Releases "lock" (parameters given in Lock object).
     * 
     * @param userCredentials
     *            Name and Groups of the user.
     * @param lock
     *            Lock to be released.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws IOException
     * @throws PosixErrorException
     */
    public void releaseLock(UserCredentials userCredentials, Lock lock) throws IOException,
            PosixErrorException, AddressToUUIDNotFoundException;

    /**
     * Releases the lock possibly hold by "processId". Use this before closing a file to ensure POSIX
     * semantics:
     * 
     * "All locks associated with a file for a given process shall be removed when a file descriptor for that
     * file is closed by that process or the process holding that file descriptor terminates."
     * (http://pubs.opengroup.org/onlinepubs/009695399/functions/fcntl.html)
     * 
     * @param processId
     *            ID of the process whose lock shall be released.
     * 
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws IOException
     * @throws PosixErrorException
     */
    public void releaseLockOfProcess(int processId) throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException;

    /**
     * Triggers the replication of the replica on the OSD with the UUID "osd_uuid" if the replica is a full
     * replica (and not a partial one).
     * 
     * The Replica had to be added beforehand and "osd_uuid" has to be included in the XlocSet of the file.
     * 
     * @param userCredentials
     *            Name and Groups of the user.
     * @param osdUuid
     *            UUID of the OSD where the replica is located.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws IOException
     * @throws PosixErrorException
     * @throws UUIDNotInXlocSetException
     */
    public void pingReplica(UserCredentials userCredentials, String osdUuid) throws IOException,
            AddressToUUIDNotFoundException;

    /**
     * Closes the open file handle (flushing any pending data).
     * 
     * @attention Please execute ReleaseLockOfProcess() first if there're multiple open file handles for the
     *            same file and you want to ensure the POSIX semantics that with the close of a file handle
     *            the lock (XtreemFS allows only one per tuple (client UUID, Process ID)) of the process will
     *            be closed. If you do not care about this, you don't have to release any locks on your own as
     *            all locks will be automatically released if the last open file handle of a file will be
     *            closed.
     *
     * @throws IOException
     */
    public void close() throws IOException;
}
