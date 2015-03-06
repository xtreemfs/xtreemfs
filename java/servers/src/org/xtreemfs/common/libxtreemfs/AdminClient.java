/*
 * Copyright (c) 2012 by Lukas Kairies, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.xtreemfs.common.libxtreemfs.exceptions.AddressToUUIDNotFoundException;
import org.xtreemfs.common.libxtreemfs.exceptions.VolumeNotFoundException;
import org.xtreemfs.common.libxtreemfs.exceptions.XtreemFSException;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceStatus;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceType;

public interface AdminClient extends Client {

    /**
     * Open an admin volume and use the returned class to access it.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws VolumeNotFoundException
     * @thorws IOException
     * @throws {@link IOException}
     */
    public AdminVolume openVolume(String volumeName, SSLOptions sslOptions, Options options)
            throws AddressToUUIDNotFoundException, VolumeNotFoundException, IOException;

    /**
     * Starts a cleanup on the OSD with the given UUID.
     * 
     * @param osdUUID
     *            UUID of the OSD
     * @param password
     *            admin password
     * @param remove
     *            erase potential zombies
     * @param deleteVolumes
     *            deletes volumes that might be dead
     * @param restore
     *            restore zombies found on the OSD
     * @param removeMetadata
     *            remove metadata from deleted or abandoned files
     * @param metaDataTimeoutS
     *            time in seconds to wait after the last view update before deleting metadata
     * @throws IOException
     */
    public void startCleanUp(String osdUUID, String password, boolean remove, boolean deleteVolumes, boolean restore,
            boolean removeMetadata, int metaDataTimeoutS) throws IOException;

    /**
     * Run a version cleanup (only if file content versioning is enabled).
     * 
     * @param osdUUID
     *            UUID of the OSD
     * @param password
     *            admin password
     * @throws IOException
     */
    public void startVersionCleanUp(String osdUUID, String password) throws IOException;

    /**
     * Suspends the currently running cleanup process.
     * 
     * @param osdUUID
     *            UUID of the OSD
     * @param password
     *            admin password
     * @throws IOException
     */
    public void stopCleanUp(String osdUUID, String password) throws IOException;

    /**
     * Returns true if a cleanup is running.
     * 
     * @param osdUUID
     *            UUID of the OSD
     * @param password
     *            admin password
     * @throws IOException
     */
    public boolean isRunningCleanUp(String osdUUID, String password) throws IOException;

    /**
     * Returns the current cleanup state.
     * 
     * @param osdUUID
     *            UUID of the OSD
     * @param password
     *            admin password
     * @throws IOException
     * 
     */
    public String getCleanUpState(String osdUUID, String password) throws IOException;

    /**
     * Returns the cleanup Result.
     * 
     * @param osdUUID
     *            UUID of the OSD
     * @param password
     *            admin password
     * @throws IOException
     * 
     */
    public List<String> getCleanUpResult(String osdUUID, String password) throws IOException;

    /**
     * Returns a ServiceSet with all services of the given type.
     * 
     * @param serviceType
     * 
     * @return ServiceSet
     * @throws XtreemFSException
     * @throws IOException
     */
    public ServiceSet getServiceByType(ServiceType serviceType) throws IOException;

    /**
     * Returns the Service with the given UUID
     * 
     * @param uuid
     *            UUID of the Service
     * @throws IOException
     */
    public Service getServiceByUUID(String uuid) throws IOException;

    /**
     * Set the service status of the OSD with UUID "osdUUID" to "serviceStatus".
     * 
     * @param osdUUID
     *            UUID of the OSD.
     * @param serviceStatus
     *            service status which will be set.
     * @throws IOException
     */
    public void setOSDServiceStatus(String osdUUID, ServiceStatus serviceStatus) throws IOException;

    /**
     * Returns the current status of the OSD with the UUID "osdUUID".
     * 
     * @param osdUUID
     * @return
     * @throws IOException
     */
    public ServiceStatus getOSDServiceStatus(String osdUUID) throws IOException;

    /**
     * Returns a set of all removed OSDs.
     * 
     * @throws IOException
     */
    public Set<String> getRemovedOsds() throws IOException;

}
