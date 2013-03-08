/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *               
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox.benchmarkOSDPerformance;

import java.io.IOException;
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
    LinkedList<Volume>           volumes;
    LinkedList<Volume>           createdVolumes;

    private VolumeManager() throws Exception {
        this.connection = Controller.getConnectionData();
        this.client = BenchmarkClientFactory.getNewClient(connection);
        this.volumes = new LinkedList<Volume>();
        this.createdVolumes = new LinkedList<Volume>();
    }

    public static synchronized VolumeManager getInstance() throws Exception {
        if (volumeManager == null) {
            volumeManager = new VolumeManager();
        }
        return volumeManager;
    }


    /* Performs cleanup on a OSD (because deleting the volume does not delete the files in the volume) */
    void scrub() throws Exception {

        String pwd = connection.osdPassword;
        LinkedList<String> uuids = getOSDUUIDs();

        for (String osd : uuids) {
            Logging.logMessage(Logging.LEVEL_INFO, this, "Starting cleanup of osd %s", osd, Logging.Category.tool);
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
            Logging.logMessage(Logging.LEVEL_INFO, this, "Finished cleanup of %s", osd, Logging.Category.tool);
        }

    }

    LinkedList<String> getOSDUUIDs() throws IOException {
        LinkedList<String> uuids = new LinkedList<String>();
        for (DIR.Service service : client.getServiceByType(DIR.ServiceType.SERVICE_TYPE_OSD).getServicesList()) {
            uuids.add(service.getUuid());
        }
        return uuids;
    }

    Volume getNextVolume() {
        return volumes.poll();
    }

    void deleteCreatedVolumes() throws IOException {
        for (Volume volume : createdVolumes)
            deleteVolume(volume);
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

    void createDefaultVolumes(int numberOfVolumes) throws IOException {
        for (int i = 0; i < numberOfVolumes; i++) {
            volumes.add(createAndOpenVolume(VOLUME_BASE_NAME + i));
        }
    }

    void openVolumes(String... volumeName) throws IOException {
        this.volumes = new LinkedList<Volume>();
        for (String each : volumeName) {
            volumes.add(createAndOpenVolume(each));
        }
    }

    void deleteDefaultVolumes(int numberOfVolumes) throws IOException {
        for (int i = 0; i < numberOfVolumes; i++) {
            deleteVolume(VOLUME_BASE_NAME + i);
        }
    }

    void deleteVolumes(String... volumeName) throws IOException {
        for (String each : volumeName) {
            deleteVolume(each);
        }
    }

    void deleteVolume(String volumeName) throws IOException {
        client.deleteVolume(connection.mrcAddress, connection.auth, connection.userCredentials, volumeName);
    }

    void deleteVolume(Volume volume) throws IOException {
        client.deleteVolume(connection.mrcAddress, connection.auth, connection.userCredentials, volume.getVolumeName());
    }

}
