/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox.benchmark;

import java.io.IOException;
import java.util.*;

import org.xtreemfs.common.libxtreemfs.AdminClient;
import org.xtreemfs.common.libxtreemfs.Volume;
import org.xtreemfs.common.libxtreemfs.exceptions.PosixErrorException;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC;

/**
 * Singleton Volume Manager.
 *
 * @author jensvfischer
 */
public class VolumeManager {

    static final String              VOLUME_BASE_NAME = "benchmark";

    private static VolumeManager     volumeManager    = null;
    Params                           params;
    AdminClient                      client;
    int                              currentPosition;
    LinkedList<Volume>               volumes;
    LinkedList<Volume>               createdVolumes;
    HashMap<Volume, HashSet<String>> createdFiles;
    HashMap<Volume, String[]>        filelistsSequentialBenchmark;
    HashMap<Volume, String[]>        filelistsRandomBenchmark;

    public static void init(Params params) throws Exception {
        if (volumeManager == null) {
            volumeManager = new VolumeManager(params);
        }
    }

    public static VolumeManager getInstance() throws Exception {
        if (volumeManager == null)
            throw new RuntimeException("Volume Manager not initialized");
        return volumeManager;
    }

    private VolumeManager(Params params) throws Exception {
        this.params = params;
        currentPosition = 0;
        this.client = BenchmarkClientFactory.getNewClient(params);
        this.volumes = new LinkedList<Volume>();
        this.createdVolumes = new LinkedList<Volume>();
        this.filelistsSequentialBenchmark = new HashMap<Volume, String[]>(5);
        this.filelistsRandomBenchmark = new HashMap<Volume, String[]>(5);
        this.createdFiles = new HashMap<Volume, HashSet<String>>();
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

    private void addToVolumes(Volume volume) {
        if (!volumes.contains(volume))
            volumes.add(volume);
    }

    void openVolumes(String... volumeName) throws IOException {
        this.volumes = new LinkedList<Volume>();
        for (String each : volumeName) {
            volumes.add(createAndOpenVolume(each));
        }
    }

    private Volume createAndOpenVolume(String volumeName) throws IOException {
        // Todo (jvf) Add Logging Info
        Volume volume = null;
        try {
            List<GlobalTypes.KeyValuePair> volumeAttributes = new ArrayList<GlobalTypes.KeyValuePair>();
            client.createVolume(params.auth, params.userCredentials, volumeName, 511,
                    params.userName, params.group, GlobalTypes.AccessControlPolicyType.ACCESS_CONTROL_POLICY_POSIX,
                    GlobalTypes.StripingPolicyType.STRIPING_POLICY_RAID0, params.getStripeSizeInKiB,
                    params.stripeWidth, volumeAttributes);
            volume = client.openVolume(volumeName, params.sslOptions, params.options);

            // Todo (jvf) Implement in tool and params or delete
//            volume.setOSDSelectionPolicy(params.userCredentials, "1000,3002,4711");
            createdVolumes.add(volume);
            createDirStructure(volume);
        } catch (PosixErrorException e) {
            if (e.getPosixError() == POSIXErrno.POSIX_ERROR_EEXIST) {
                volume = client.openVolume(volumeName, params.sslOptions, params.options);
            }
        }
        return volume;
    }

    private void createDirStructure(Volume volume) {
        // tryToCreateDir(volume, "benchmarks", false);
        tryToCreateDir(volume, "/benchmarks/sequentialBenchmark", true);
        tryToCreateDir(volume, "/benchmarks/randomBenchmark", true);
    }

    private void tryToCreateDir(Volume volume, String dirName, boolean recursive) {
        try {
            volume.createDirectory(params.userCredentials, dirName, 0777, recursive);
            Logging.logMessage(Logging.LEVEL_INFO, Logging.Category.tool, this, "Directory %s on volume %s created.",
                    dirName, volume.getVolumeName());
        } catch (PosixErrorException e) {
            if (e.getPosixError() != POSIXErrno.POSIX_ERROR_EEXIST) {
                Logging.logMessage(Logging.LEVEL_ERROR, Logging.Category.tool, this,
                        "Error while trying to create directory %s. Errormessage: %s", dirName, e.getMessage());
                e.printStackTrace();
            }
        } catch (IOException e) {
            Logging.logMessage(Logging.LEVEL_ERROR, Logging.Category.tool, this,
                    "Error while trying to create directory %s. Errormessage: %s", dirName, e.getMessage());
            e.printStackTrace();

        }
    }

    void setSequentialFilelistForVolume(Volume volume, LinkedList<String> filelist) {
        String[] files = new String[filelist.size()];
        synchronized (this) {
            filelistsSequentialBenchmark.put(volume, filelist.toArray(files));
        }
    }

    void setRandomFilelistForVolume(Volume volume, LinkedList<String> filelist) {
        String[] files = new String[filelist.size()];
        synchronized (this) {
            filelistsRandomBenchmark.put(volume, filelist.toArray(files));
        }
    }

    synchronized String[] getSequentialFilelistForVolume(Volume volume) throws IOException {
        String[] filelist;

        /* null means no filelist from a previous write benchmark has been deposited */
        if (null == filelistsSequentialBenchmark.get(volume)) {
            filelist = inferFilelist(volume, SequentialBenchmark.BENCHMARK_FILENAME);

            if (params.sequentialSizeInBytes == calculateTotalSizeOfFilelist(volume, filelist)) {
                filelistsSequentialBenchmark.put(volume, filelist);
                Logging.logMessage(Logging.LEVEL_INFO, Logging.Category.tool, this,
                        "Succesfully infered filelist on volume %s.", volume.getVolumeName());
            } else {
                Logging.logMessage(Logging.LEVEL_INFO, Logging.Category.tool, this, "Infering filelist failed",
                        volume.getVolumeName());
                throw new IllegalArgumentException("No valid files for benchmark found");
            }

        }
        return filelistsSequentialBenchmark.get(volume);
    }

    synchronized String[] getRandomFilelistForVolume(Volume volume) throws IOException {
        String[] filelist;

        /* null means no filelist from a previous write benchmark has been deposited */
        if (null == filelistsRandomBenchmark.get(volume)) {
            filelist = inferFilelist(volume, FilebasedBenchmark.BENCHMARK_FILENAME);

            if (params.randomSizeInBytes == calculateTotalSizeOfFilelist(volume, filelist)) {
                filelistsRandomBenchmark.put(volume, filelist);
                Logging.logMessage(Logging.LEVEL_INFO, Logging.Category.tool, this,
                        "Succesfully infered filelist on volume %s.", volume.getVolumeName());
            } else {
                Logging.logMessage(Logging.LEVEL_INFO, Logging.Category.tool, this, "Infering filelist failed",
                        volume.getVolumeName());
                throw new IllegalArgumentException("No valid files for benchmark found");
            }

        }
        return filelistsRandomBenchmark.get(volume);
    }

    private String[] inferFilelist(Volume volume, String pathToBasefile) throws IOException {

        Logging.logMessage(Logging.LEVEL_INFO, Logging.Category.tool, this, "Read benchmark without write benchmark. "
                + "Trying to infer a filelist on volume %s", volume.getVolumeName());

        String path = pathToBasefile.substring(0, pathToBasefile.lastIndexOf('/'));
        String filename = pathToBasefile.substring(pathToBasefile.lastIndexOf('/') + 1);

        /* ToDo (jvf) there seems to be a bug if you try to get filelist > 1024 files */
        List<MRC.DirectoryEntry> directoryEntries = volume.readDir(params.userCredentials, path, 0, 0, true)
                .getEntriesList();
        ArrayList<String> entries = new ArrayList<String>(directoryEntries.size());

        for (MRC.DirectoryEntry directoryEntry : directoryEntries) {
            String entry = directoryEntry.getName();
            if (entry.matches(filename + "[0-9]+"))
                entries.add(path + '/' + directoryEntry.getName());
        }
        entries.trimToSize();
        String[] filelist = new String[entries.size()];
        entries.toArray(filelist);
        return filelist;
    }

    private long calculateTotalSizeOfFilelist(Volume volume, String[] filelist) throws IOException {
        long aggregatedSizeInBytes = 0;
        for (String file : filelist) {
            MRC.Stat stat = volume.getAttr(params.userCredentials, file);
            aggregatedSizeInBytes += stat.getSize();
        }
        return aggregatedSizeInBytes;
    }

    // adds the files created by a benchmark to the list of created files
    synchronized void addCreatedFiles(Volume volume, LinkedList<String> newFiles) {
        HashSet<String> filelistForVolume;
        if (createdFiles.containsKey(volume))
            filelistForVolume = createdFiles.get(volume);
        else
            filelistForVolume = new HashSet();
        filelistForVolume.addAll(newFiles);
        createdFiles.put(volume, filelistForVolume);
    }

    void deleteCreatedFiles() {
        for (Volume volume : volumes) {
            HashSet<String> fileListForVolume = createdFiles.get(volume);

            if (null != fileListForVolume) {

                Logging.logMessage(Logging.LEVEL_INFO, Logging.Category.tool, this, "Deleting %s file(s) on volume %s",
                        fileListForVolume.size(), volume.getVolumeName());

                for (String filename : fileListForVolume) {
                    tryToDeleteFile(volume, filename);
                }
            }
        }
    }

    private void tryToDeleteFile(Volume volume, String filename) {
        try {
            volume.unlink(params.userCredentials, filename);
        } catch (IOException e) {
            Logging.logMessage(Logging.LEVEL_ERROR, Logging.Category.tool, this,
                    "IO Error while trying to delete a file. Errormessage: %s", e.getMessage());
        }
    }

    void deleteCreatedVolumes() throws IOException {
        for (Volume volume : createdVolumes) {
            deleteVolumeIfExisting(volume);
        }
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
            client.deleteVolume(params.auth, params.userCredentials, volumeName);
            Logging.logMessage(Logging.LEVEL_INFO, Logging.Category.tool, this, "Deleting volume %s", volumeName);
        }
    }

    /* Performs cleanup on a OSD (because deleting the volume does not delete the files in the volume) */
    void cleanupOSD() throws Exception {

        String pwd = params.osdPassword;
        LinkedList<String> uuids = getOSDUUIDs();

        for (String osd : uuids) {
            Logging.logMessage(Logging.LEVEL_INFO, this, "Starting cleanup of OSD with UUID %s", osd,
                    Logging.Category.tool);
            client.startCleanUp(osd, pwd, true, true, false);
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

    public LinkedList<Volume> getVolumes() {
        return volumes;
    }

}
