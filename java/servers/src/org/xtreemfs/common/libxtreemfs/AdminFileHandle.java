/*
 * Copyright (c) 2011 by Lukas Kairies, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import java.io.IOException;
import java.util.List;

import org.xtreemfs.common.clients.InvalidChecksumException;
import org.xtreemfs.common.libxtreemfs.exceptions.AddressToUUIDNotFoundException;
import org.xtreemfs.common.libxtreemfs.exceptions.PosixErrorException;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;

public interface AdminFileHandle extends FileHandle {

    /**
     * Returns a list with all replicas of the File.
     * 
     */
    public List<Replica> getReplicasList();

    /**
     * Returns the replica with index "replicaIndex".
     * 
     * @param replicaIndex
     *            Index of the replica.
     */
    public Replica getReplica(int replicaIndex);

    /**
     * Returns the striping policy of the file. If the file is replicated, the striping policy of the first
     * replica is returned.
     */
    public StripingPolicy getStripingPolicy();

    /**
     * Returns the striping policy of the replica with index "replicaIndex".
     * 
     * @param replicaIndex
     */
    public StripingPolicy getStripingPolicy(int replicaIndex);

    /**
     * Returns the replica update policy of the file as a String. Constants for replica update policies are
     * defined in {@link org.xtreemfs.common.ReplicaUpdatePolicies}.
     * 
     */
    public String getReplicaUpdatePolicy();

    /**
     * Returns the global ID of the file.
     * 
     */
    public String getGlobalFileId();

    /**
     * Checks if a read-only replica with index "replicaIndex" is a complete replica and marks it as complete
     * if not done yet.
     * 
     * @param replicaIndex
     *            Index of the replica.
     */
    public boolean checkAndMarkIfReadOnlyReplicaComplete(int replicaIndex, UserCredentials userCredentials)
            throws IOException, AddressToUUIDNotFoundException;

    /**
     * Returns the number of objects.
     * 
     * @param userCredentials
     *            Name and Groups of the user.
     *            
     * @throws IOException
     */
    public long getNumObjects(UserCredentials userCredentials) throws IOException;

    /**
     * Checks the object's checksum and returns the total number of bytes (data + sparse data) or throws an
     * InvalidChecksumException.
     * 
     * @param replicaIndex
     *            Replica from which the object will be checked.
     * @param objectNo
     *            Object which will be checked.
     * 
     * @throws InvalidChecksumException
     * @throws IOException
     */
    public int checkObjectAndGetSize(int replicaIndex, long objectNo) throws IOException,
            InvalidChecksumException;

    /**
     * Repairs the Object with number "objectNo" of the replica with index "replicaIndex".
     * 
     * @param replicaIndex
     *            Index of the Replica in the xlocset from which the object will be repaired.
     * @param objectNo
     *            Object which will be repaired.
     */
    public void repairObject(int replicaIndex, long objectNo) throws IOException;

    /**
     * 
     * Returns file size on the OSDs.
     * 
     * @throws IOException
     */
    public long getSizeOnOSD() throws IOException;

    /**
     * Same as truncate(userCredentials, newFileSize) but with the option to truncate the file only at the
     * MRC.
     * 
     * @param userCredentials
     *            Name and Groups of the user.
     * @param newFileSize
     *            New size of the file.
     * @param truncateOnlyAtMRC
     *            true if the file should be truncated only at the MRC, otherwise false.
     * 
     * @throws PosixErrorException
     * @throws AddressToUUIDNotFoundException
     * @throws IOException
     */
    public void truncate(UserCredentials userCredentials, long newFileSize, boolean truncateOnlyAtMRC)
            throws PosixErrorException, AddressToUUIDNotFoundException, IOException;

}
