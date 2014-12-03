/*
 * Copyright (c) 2012 by Lukas Kairies, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import java.io.IOException;

import org.xtreemfs.common.libxtreemfs.exceptions.AddressToUUIDNotFoundException;
import org.xtreemfs.common.libxtreemfs.exceptions.PosixErrorException;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;

/**
 * Extends the default Volume with additional functions. An admin volume object can be obtain by opening a
 * volume with an admin client.
 * 
 */
public interface AdminVolume extends Volume {

    /**
     * Opens a file and returns the pointer to a {@link FileHandle} object.
     * 
     * @param userCredentials
     *            Name and Groups of the user.
     * @param path
     *            Path to the file.
     * @param flags
     *            Open flags as specified in xtreemfs::pbrpc::SYSTEM_V_FCNTL.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws {@link IOException}
     * @throws PosixErrorException
     * @throws AddressToUUIDNotFoundException
     * 
     */
    public AdminFileHandle openFile(UserCredentials userCredentials, String path, int flags)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException;

    /**
     * Same as previous openFile() except for the additional mode parameter, which sets the permissions for
     * the file in case SYSTEM_V_FCNTL_H_O_CREAT is specified as flag and the file will be created.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws {@link IOException}
     * @throws PosixErrorException
     * @throws AddressToUUIDNotFoundException
     */
    public AdminFileHandle openFile(UserCredentials userCredentials, String path, int flags, int mode)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException;

    /**
     * Returns the object number for the file at "path".
     * 
     * @param userCredentials
     *            Name and groups of the user.
     * @param path
     *            Path to the file.
     * 
     * @throws IOException
     */
    public long getNumObjects(UserCredentials userCredentials, String path) throws IOException;

    /**
     * Same as unlink(userCredentials, newFileSize) but with the option to unlink the file only at the MRC.
     * 
     * @param userCredentials
     *            Name and Groups of the user.
     * @param path
     *            Path to the file.
     * @param unlinkOnlyAtMrc
     *            true if the file should be unlinked only at the MRC, otherwise false.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws {@link IOException}
     * @throws PosixErrorException
     * @throws AddressToUUIDNotFoundException
     */
    public abstract void unlink(UserCredentials userCredentials, String path, boolean unlinkOnlyAtMrc)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException;
}
