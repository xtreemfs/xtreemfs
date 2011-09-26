/*
 * Copyright (c) 2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import org.xtreemfs.common.libxtreemfs.exceptions.VolumeNotFoundException;

/**
 * 
 * <br>
 * Sep 5, 2011
 */
public interface UUIDResolver {

    // TODO: Exceptions!

    /**
     * Resolves the address (ip-address:port) for a given UUID.
     * 
     * @return String
     *                  The resolved address as String.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws UnknownAddressSchemeException
     */
    public String uuidToAddress(String uuid);

    /**
     * Resolves the UUID for a given volume name.
     * 
     * @param  String
     *          Name of the volume.
     * @return String
     *                  UUID of the MRC the volume 'volumeName' is registered.
     *                  
     * @throws VolumeNotFoundException
     */
    public String volumeNameToMRCUUID(String volumeName) throws VolumeNotFoundException;

    /**
     * Resolves the list of UUIDs of the MRC replicas and adds them to the uuid_iterator object.
     * 
     * @throws VolumeNotFoundException
     */
    public void volumeNameToMRCUUID(String volumeName, UUIDIterator uuidIterator)
            throws VolumeNotFoundException;
}
