/*
 * Copyright (c) 2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import java.util.List;

import org.xtreemfs.common.libxtreemfs.exceptions.AddressToUUIDNotFoundException;
import org.xtreemfs.common.libxtreemfs.exceptions.VolumeNotFoundException;


public interface UUIDResolver {


    /**
     * Resolves the address (ip-address:port) for a given UUID.
     * 
     * @return String
     *                  The resolved address as String.
     * 

     * @throws AddressToUUIDNotFoundException
     */
    public String uuidToAddress(String uuid) throws AddressToUUIDNotFoundException;

    /**
     * Resolves the UUID for a given volume name.
     * 
     * @param  volumeName
     *          Name of the volume.
     * @return String
     *                  UUID of the MRC the volume 'volumeName' is registered.
     *             
     * @throws AddressToUUIDNotFoundException
     * @throws VolumeNotFoundException
     */
    public String volumeNameToMRCUUID(String volumeName) throws VolumeNotFoundException, AddressToUUIDNotFoundException;

    /**
     * Resolves the list of UUIDs of the MRC replicas and adds them to the uuid_iterator object.
     * 
     * @throws VolumeNotFoundException
     * @throws AddressToUUIDNotFoundException 
     */
    public void volumeNameToMRCUUID(String volumeName, UUIDIterator uuidIterator)
            throws VolumeNotFoundException, AddressToUUIDNotFoundException;

    /**
     * Resolves the list of UUIDs of the MRC replicas.
     * 
     * @param volumeName
     *            Name of the volume.
     * @return List of the UUIDs of the MRC replicas.
     * 
     * @throws VolumeNotFoundException
     * @throws AddressToUUIDNotFoundException
     */
    public List<String> volumeNameToMRCUUIDs(String volumeName) throws VolumeNotFoundException,
            AddressToUUIDNotFoundException;
}
