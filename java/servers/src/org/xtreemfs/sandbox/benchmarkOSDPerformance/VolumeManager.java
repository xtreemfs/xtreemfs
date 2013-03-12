/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *               
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox.benchmarkOSDPerformance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import org.xtreemfs.common.libxtreemfs.AdminClient;
import org.xtreemfs.common.libxtreemfs.Volume;
import org.xtreemfs.common.libxtreemfs.exceptions.PosixErrorException;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR;

/**
 * Singleton Volume Manager.
 * 
 * @author jensvfischer
 */
public class VolumeManager {

    static final String          VOLUME_BASE_NAME = "performanceTest";

    private static VolumeManager volumeManager    = null;
    ConnectionData               connection;
    AdminClient                  client;
    int                          currentPosition;
    LinkedList<Volume>           volumes;
    LinkedList<Volume>           createdVolumes;

    public static void init(ConnectionData connection) throws Exception {
        if (volumeManager == null) {
            volumeManager = new VolumeManager(connection);
        }
    }

    public static VolumeManager getInstance() throws Exception {
        if (volumeManager == null)
            throw new RuntimeException("Volume Manager not initialized");
        return volumeManager;
    }

    private VolumeManager(ConnectionData connection) throws Exception {
        this.connection = connection;
        currentPosition = 0;
        this.client = BenchmarkClientFactory.getNewClient(connection);
        this.volumes = new LinkedList<Volume>();
        this.createdVolumes = new LinkedList<Volume>();
    }

    Volume getNextVolume() {
        return volumes.get(currentPosition++);
    }

    /* reset the position counter. Needs to be called if u want to reuse the volumes for another benchmark */
    void reset() {
        currentPosition = 0;
    }

    void createDefaultVolumes(int numberOfVolumes) throws IOException {
        for (int i = 0; i < numberOfVolumes; i++) {
            Volume volume = createAndOpenVolume(VOLUME_BASE_NAME + i);
            addToVolumes(volume);
        }
    }

    private void addToVolumes(Volume volume){
        if ( ! volumes.contains(volume) )
            volumes.add(volume);
    }

    void openVolumes(String... volumeName) throws IOException {
        this.volumes = new LinkedList<Volume>();
        for (String each : volumeName) {
            volumes.add(createAndOpenVolume(each));
        }
    }

    private Volume createAndOpenVolume(String volumeName) throws IOException {
        Volume volume = null;
        try {
            client.createVolume(connection.mrcAddress, connection.auth, connection.userCredentials, volumeName);
            volume = client.openVolume(volumeName, connection.sslOptions, connection.options);
            createdVolumes.add(volume);
        } catch (PosixErrorException e) {
            if (e.getPosixError() == POSIXErrno.POSIX_ERROR_EEXIST) {
                volume = client.openVolume(volumeName, connection.sslOptions, connection.options);
            }
        }
        return volume;
    }

    void deleteCreatedVolumes() throws IOException {
        for (Volume volume : createdVolumes)
            deleteVolumeIfExisting(volume);
    }

    void deleteVolumes(String... volumeName) throws IOException {
        for (String each : volumeName) {
            deleteVolumeIfExisting(each);
        }
    }

    void deleteDefaultVolumes(int numberOfVolumes) throws IOException {
        for (int i = 0; i < numberOfVolumes; i++) {
            deleteVolumeIfExisting(VOLUME_BASE_NAME + i);
        }
    }

    void deleteVolumeIfExisting(Volume volume) throws IOException {
        deleteVolumeIfExisting(volume.getVolumeName());
    }

    void deleteVolumeIfExisting(String volumeName) throws IOException {
        if (new ArrayList<String>(Arrays.asList(client.listVolumeNames())).contains(volumeName)) {
            client.deleteVolume(connection.auth, connection.userCredentials, volumeName);
            Logging.logMessage(Logging.LEVEL_INFO, Logging.Category.tool, this, "Deleting volume %s", volumeName);
        }
    }

    /* Performs cleanup on a OSD (because deleting the volume does not delete the files in the volume) */
    void scrub() throws Exception {

        String pwd = connection.osdPassword;
        LinkedList<String> uuids = getOSDUUIDs();

        for (String osd : uuids) {
            Logging.logMessage(Logging.LEVEL_INFO, this, "Starting cleanup of OSD with UUID %s", osd,
                    Logging.Category.tool);
            client.startCleanUp(osd, pwd, true, true, true);
        }

        boolean cleanUpIsRunning = true;
        while (cleanUpIsRunning) {
            cleanUpIsRunning = false;
            for (String osd : uuids) {
                cleanUpIsRunning = cleanUpIsRunning || client.isRunningCleanUp(osd, pwd);
            }
            Thread.sleep(300);
        }

        for (String osd : uuids) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "Cleanup Result: %s", client.getCleanUpResult(osd, pwd),
                    Logging.Category.tool);
            Logging.logMessage(Logging.LEVEL_INFO, this, "Finished cleanup of OSD with UUID %s", osd,
                    Logging.Category.tool);
        }

    }

    LinkedList<String> getOSDUUIDs() throws IOException {
        LinkedList<String> uuids = new LinkedList<String>();
        for (DIR.Service service : client.getServiceByType(DIR.ServiceType.SERVICE_TYPE_OSD).getServicesList()) {
            uuids.add(service.getUuid());
        }
        return uuids;
    }

}
