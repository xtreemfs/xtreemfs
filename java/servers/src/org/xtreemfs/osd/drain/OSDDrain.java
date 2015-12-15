/*
 * Copyright (c) 2011 by Paul Seiferth,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.drain;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.xtreemfs.common.HeartbeatThread;
import org.xtreemfs.common.KeyValuePairs;
import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.common.xloc.ReplicationFlags;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.dir.DIRClient;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.osd.drain.OSDDrainException.ErrorState;
import org.xtreemfs.osd.replication.ObjectSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.AddressMappingSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceDataMap;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceStatus;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XLocSet;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_check_file_existsRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_check_file_existsResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_check_file_existsResponse.FILE_STATE;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_get_suitable_osdsRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_get_suitable_osdsResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_get_xlocsetRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_replica_addRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_replica_addResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_replica_removeRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_replica_removeResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_set_read_only_xattrResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_set_replica_update_policyResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectData;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectList;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_internal_get_fileid_listResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;

/**
 * Class that provides function to remove a OSD by moving all his files to other OSDs.
 * 
 * @author bzcseife
 * 
 * <br>
 *         Mar 17, 2011
 */
public class OSDDrain {

    /**
     * Container class for all information you need to move a file from one OSD to another.
     * 
     * <br>
     * Mar 17, 2011
     */
    public static class FileInformation {
        public String            fileID;

        public InetSocketAddress mrcAddress;

        public FileCredentials   fileCredentials;

        public Replica           newReplica;

        // origReplica is necessary to restore the replicas if there is an error
        // while removing the original replica
        public Replica           oldReplica;

        public Boolean           wasAlreadyReadOnly;

        public String            oldReplicationPolicy;
        
        // Flag to determine if consistency is preserved by the MRC on adding/removing replicas.
        public boolean           isReplicaChangeCoordinated = false;

        // Since adding and removing replicas is async, it has to be waited for the xlocset installation.
        public int               expectedXLocSetVersion     = 0;
    }

    private DIRClient             dirClient;

    private OSDServiceClient      osdClient;

    private ServiceUUID           osdUUID;

    private MRCServiceClient      mrcClient;

    // private List<InetSocketAddress> mrcAddresses;

    private List<FileInformation> fileInfos;

    private UserCredentials       userCreds = RPCAuthentication.userService;

    private Auth                  password;

    private UUIDResolver          resolver;

    private final int             WAIT_FOR_REPLICA_COMPLETE_DELAY_S      = 5;

    private final int             WAIT_FOR_XLOC_SET_INSTALLATION_DELAY_S = 5;

    public OSDDrain(DIRClient dirClient, OSDServiceClient osdClient, MRCServiceClient mrcClient,
            ServiceUUID osdUUID, Auth password, UserCredentials usercreds, UUIDResolver resolver)
            throws Exception {

        this.dirClient = dirClient;
        this.osdClient = osdClient;
        this.osdUUID = osdUUID;
        this.mrcClient = mrcClient;
        this.password = password;
        this.userCreds = usercreds;
        this.resolver = resolver;

    }

    /**
     * Try to remove the OSD.
     * 
     * @param shutdown
     *            If true the OSD will be shut down. Otherwise it will be locked for assigning of new files
     *            but is still running.
     */
    public void drain(boolean shutdown) {

        try {
            // set OSDServiceStatus to prevent further writing on this OSD
            this.setServiceStatus(ServiceStatus.SERVICE_STATUS_TO_BE_REMOVED);

            // get all files the OSD has
            fileInfos = this.getFileListOfOSD();

            // get address of MRC which is responsible for every file
            this.updateMRCAddresses(fileInfos);

            // remove fileIDs which has no entry on MRC. Can happen because
            // object files on OSDs will be deleted delayed.
            fileInfos = this.removeNonExistingFileIDs(fileInfos);

            // get the current replica configuration
            fileInfos = this.getReplicaInfo(fileInfos);

            // Handle r/w coordinated files and remove them from the file info list.
            fileInfos = drainCoordinatedFiles(fileInfos);

            // set ReplicationUpdatePolicy to RONLY
            fileInfos = this.setReplicationUpdatePolicyRonly(fileInfos);

            // set Files read-only
            fileInfos = this.setFilesReadOnlyAttribute(fileInfos);

            // create replications
            fileInfos = this.createReplicasForFiles(fileInfos);

            // start replication
            fileInfos = this.startReplication(fileInfos);

            // wait for replication to be finished
            fileInfos = this.waitForReplicationToComplete(fileInfos);

            // remove replicas
            this.removeOriginalFromReplica(fileInfos);

            // set every file to read/write again which wasn't set to read-only before
            this.resetFilesReadOnlyAttribute(fileInfos);

            // set ReplicationUpdatePolicy to original value
            this.resetReplicationUpdatePolicy(fileInfos);

            // TODO: delete all files on osd

            // shutdown osd
            if (shutdown) {
                this.shutdownOsd();
            } else {
                System.out.println("The OSD is now locked and objects stored on it copied to other OSDs."
                        + " It is save to shutdown this OSD now!");
            }

        } catch (OSDDrainException e) {
            this.handleException(e, true);
            // set Service Status back to availalbe when an error occurs.
            try {
                this.setServiceStatus(ServiceStatus.SERVICE_STATUS_AVAIL);
            } catch (OSDDrainException e1) {
                this.handleException(e1, true);
                System.out.println("Service Status couldn't set back to AVAILABLE. You have to do"
                        + " this yourself.");
            }
        }

    }

    /**
     * Sets a new status to the Service with uuid. Throws Exception if something went wrong and does nothing
     * if the current status is equivalent to the new status.
     * 
     * @param uuid
     * @param status
     * @throws Exception
     */
    public void setServiceStatus(DIR.ServiceStatus status) throws OSDDrainException {
        ServiceSet sSet = null;
        try {
            sSet = dirClient.xtreemfs_service_get_by_uuid(null, RPCAuthentication.authNone,
                    RPCAuthentication.userService, osdUUID.toString());
        } catch (Exception e) {
            Logging.logError(Logging.LEVEL_WARN, this, e);
            throw new OSDDrainException(e.getMessage(), ErrorState.SET_SERVICE_STATUS);
        }

        if (sSet.getServicesCount() == 0) {
            System.out.println("no OSD with UUID " + this.osdUUID + " registered at directory service");
            System.exit(1);
        }

        Service serv = sSet.getServices(0);
        String serviceStatus = KeyValuePairs.getValue(serv.getData().getDataList(),
                HeartbeatThread.STATUS_ATTR);

        if (serviceStatus == null) {
            System.out.println("Service " + this.osdUUID + " is not registered at DIR.");
            System.exit(3);
        }

        if (Integer.valueOf(serviceStatus) == status.getNumber()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.tool, status,
                    "Service %s is already in status %s ", this.osdUUID, status.name());
            return;
        }

        // Build new Service with status= SERVICE_STATUS_TO_BE_REMOVED and
        // update DIR
        List<KeyValuePair> data = serv.getData().getDataList();

        List<KeyValuePair> data2 = new LinkedList<KeyValuePair>();

        for (KeyValuePair kvp : data) {
            data2.add(KeyValuePair.newBuilder().setKey(kvp.getKey()).setValue(kvp.getValue()).build());
        }

        KeyValuePairs.putValue(data2, HeartbeatThread.STATUS_ATTR, Integer.toString(status.ordinal()));
        KeyValuePairs.putValue(data2, HeartbeatThread.DO_NOT_SET_LAST_UPDATED, Boolean.toString(true));

        ServiceDataMap dataMap = ServiceDataMap.newBuilder().addAllData(data2).build();

        serv = serv.toBuilder().setData(dataMap).build();

        try {
            dirClient.xtreemfs_service_register(null, RPCAuthentication.authNone,
                    RPCAuthentication.userService, serv);
        } catch (Exception e) {
            if (Logging.isDebug()) {
                Logging.logError(Logging.LEVEL_WARN, this, e);
            }
            throw new OSDDrainException(e.getMessage(), ErrorState.SET_SERVICE_STATUS);
        }
    }

    /**
     * Returns a {@link LinkedList} of all fileIDs the OSD which will be removed has
     * 
     * @return {@link LinkedList}
     * @throws Exception
     */
    public LinkedList<FileInformation> getFileListOfOSD() throws OSDDrainException {

        LinkedList<FileInformation> osdFileList = new LinkedList<FileInformation>();

        RPCResponse<xtreemfs_internal_get_fileid_listResponse> resp = null;
        xtreemfs_internal_get_fileid_listResponse fileIDList = null;
        try {
            resp = osdClient.xtreemfs_internal_get_fileid_list(osdUUID.getAddress(),
                    RPCAuthentication.authNone, RPCAuthentication.userService);
            fileIDList = resp.get();
        } catch (Exception e) {
            if (Logging.isDebug()) {
                Logging.logError(Logging.LEVEL_WARN, this, e);
            }
            throw new OSDDrainException(e.getMessage(), ErrorState.GET_FILE_LIST);
        } finally {
            if (resp != null)
                resp.freeBuffers();
        }

        for (String fileID : fileIDList.getFileIdsList()) {
            FileInformation info = new FileInformation();
            info.fileID = fileID;
            osdFileList.push(info);
        }

        return osdFileList;
    }

    /**
     * Get for every fileID in fileInfos the mrcAddress of the MRC responsible for this file.
     * 
     * @param fileInfos
     */
    public void updateMRCAddresses(List<FileInformation> fileInfos) throws OSDDrainException {
        for (FileInformation fileInfo : fileInfos) {

            String volumeUUID = fileInfo.fileID.substring(0, fileInfo.fileID.indexOf(':'));

            ServiceSet sSet = null;
            String mrcUUIDString = null;
            try {
                sSet = dirClient.xtreemfs_service_get_by_uuid(null, password, userCreds, volumeUUID);
                for (KeyValuePair kvp : sSet.getServices(0).getData().getDataList()) {
                    if (kvp.getKey().equals("mrc"))
                        mrcUUIDString = kvp.getValue();
                }
                assert (mrcUUIDString != null);
            } catch (Exception e) {
                if (Logging.isDebug()) {
                    Logging.logError(Logging.LEVEL_WARN, this, e);
                }
                throw new OSDDrainException(e.getMessage(), ErrorState.UPDATE_MRC_ADDRESSES);
            }

            try {
                AddressMappingSet ams = dirClient.xtreemfs_address_mappings_get(null, password, userCreds,
                        mrcUUIDString);

                assert (ams != null);
                assert (ams.getMappings(0).getUuid().equalsIgnoreCase(mrcUUIDString));
                InetAddress inetAddr = InetAddress.getByName(ams.getMappings(0).getAddress());
                fileInfo.mrcAddress = new InetSocketAddress(inetAddr, ams.getMappings(0).getPort());
            } catch (Exception e) {
                if (Logging.isDebug()) {
                    Logging.logError(Logging.LEVEL_WARN, this, e);
                }
                throw new OSDDrainException(e.getMessage(), ErrorState.UPDATE_MRC_ADDRESSES);
            }

        }
    }

    /**
     * Creates a new List<File> without fileIDs which are available on the OSD but have no corresponding
     * metadata entry on the MRC. This can happen because on file deletion the object file will be removed
     * later than the metadata.
     * 
     * @param fileInfos
     * @return List of FilInformation with non-existing files removed.
     */
    public List<FileInformation> removeNonExistingFileIDs(List<FileInformation> fileInfos) {

        List<FileInformation> returnList = new LinkedList<FileInformation>();

        // Map with VolumeName as key and sublist of fileInfos as value. Used to decrease the amount of MRC queries.
        Map<String, List<FileInformation>> callMap = new HashMap<String, List<FileInformation>>();
        // Map to store VolID-> MRCAddress Mapping to know which MRC has to be called.
        Map<String, InetSocketAddress> volIDMrcAddressMapping = new HashMap<String, InetSocketAddress>();

        for (FileInformation fileInfo : fileInfos) {
            String volumeID = fileInfo.fileID.substring(0, fileInfo.fileID.indexOf(':'));
            if (!callMap.containsKey(volumeID)) {
                callMap.put(volumeID, new LinkedList<FileInformation>());
                volIDMrcAddressMapping.put(volumeID, fileInfo.mrcAddress);
            }

            callMap.get(volumeID).add(fileInfo);
        }

        xtreemfs_check_file_existsRequest.Builder fileExistsRequest = null;
        for (Map.Entry<String, List<FileInformation>> entry : callMap.entrySet()) {
            fileExistsRequest = xtreemfs_check_file_existsRequest.newBuilder().setVolumeId(entry.getKey())
                    .setOsdUuid(osdUUID.toString());

            for (FileInformation fi : entry.getValue()) {
                fileExistsRequest.addFileIds(fi.fileID.substring(fi.fileID.indexOf(":") + 1));
            }

            RPCResponse<xtreemfs_check_file_existsResponse> r = null;
            xtreemfs_check_file_existsResponse response = null;
            try {
                r = mrcClient.xtreemfs_check_file_exists(volIDMrcAddressMapping.get(entry.getKey()),
                        password, userCreds, fileExistsRequest.build());
                response = r.get();
            } catch (Exception e) {
                if (Logging.isDebug()) {
                    Logging.logError(Logging.LEVEL_WARN, this, e);
                }
            } finally {
                if (r != null) {
                    r.freeBuffers();
                }
            }

            assert (response.getVolumeExists());

            for (int i = 0; i < response.getFileStatesCount(); i++) {
                if (response.getFileStates(i) == FILE_STATE.REGISTERED) {
                    returnList.add(entry.getValue().get(i));
                }
            }
        }
        return returnList;
    }

    /**
     * Get the current replica information from the MRC for every fileID in fileIDList.
     * 
     * @param fileInfos
     * @throws OSDDrainException
     */
    public List<FileInformation> getReplicaInfo(List<FileInformation> fileInfos) throws OSDDrainException {
        LinkedList<FileInformation> finishedFileInfos = new LinkedList<FileInformation>();

        for (FileInformation fileInfo : fileInfos) {

            // get Striping Policy
            RPCResponse<XLocSet> xlocsetResp = null;
            XLocSet xlocset = null;
            try {
                
                xtreemfs_get_xlocsetRequest xlocReq = xtreemfs_get_xlocsetRequest.newBuilder()
                        .setFileId(fileInfo.fileID).build();
                xlocsetResp = mrcClient
                        .xtreemfs_get_xlocset(fileInfo.mrcAddress, password, userCreds, xlocReq);
                xlocset = xlocsetResp.get();
            } catch (Exception e) {
                if (Logging.isDebug()) {
                    Logging.logError(Logging.LEVEL_WARN, this, e);
                }
                throw new OSDDrainException(e.getMessage(), ErrorState.GET_REPLICA_INFO, fileInfos,
                        finishedFileInfos);
            } finally {
                if (xlocsetResp != null)
                    xlocsetResp.freeBuffers();
            }

            // TODO(jdillmann): Use centralized method to check if a lease is required.
            fileInfo.isReplicaChangeCoordinated = (xlocset.getReplicasCount() > 1 
                    && ReplicaUpdatePolicies.isRwReplicated(xlocset.getReplicaUpdatePolicy()));

            // find the replica for the given UUID
            for (Replica replica : xlocset.getReplicasList()) {
                if (replica.getOsdUuidsList().contains(osdUUID.toString())) {
                    fileInfo.oldReplica = replica;
                }
            }
            assert (fileInfo.oldReplica != null);

            finishedFileInfos.add(fileInfo);
        }
        
        return finishedFileInfos;
    }

    /**
     * Handle files that are guaranteed to retain safe when adding or removing replicas
     * because they are coordinated by the MRC. At the moment this is done for r/w replicated
     * files.
     * @param fileInfos List of files to move from the OSD.
     * @return List of files not coordinated by the MRC.
     */
    public List<FileInformation> drainCoordinatedFiles(List<FileInformation> fileInfos) throws OSDDrainException {
        LinkedList<FileInformation> uncoordinatedFiles = new LinkedList<FileInformation>();
        LinkedList<FileInformation> finishedFiles = new LinkedList<FileInformation>();

        for (FileInformation fileInfo : fileInfos) {
            // Filter uncoordinated files.
            if (!fileInfo.isReplicaChangeCoordinated) {
                uncoordinatedFiles.add(fileInfo);
                continue;
            }

            // Create a new replica.
            Replica replica;
            try {
                replica = createReplicaForFile(fileInfo);
                fileInfo.newReplica = replica;
            } catch (OSDDrainException e) {
                String message = "Could not create a replica for file with id: " + fileInfo.fileID + "\n"
                        + "It is safe to call xtfs_remove_osd again.\n"
                        + "Original error was:\n" + e.getMessage();
                throw new OSDDrainException(message, ErrorState.DRAIN_COORDINATED);
            }

            // Add the replica.
            try {
                addReplicaToFile(fileInfo, fileInfo.newReplica);
            } catch (OSDDrainException e) {
                String message = "Could not add replica for file with id: " + fileInfo.fileID + "\n"
                        + "It is safe to call xtfs_remove_osd again.\n"
                        + "Original error was:\n" + e.getMessage();
                throw new OSDDrainException(message, ErrorState.DRAIN_COORDINATED);
            }

            // Remove the replica on the drained OSD.
            try {
                removeReplica(fileInfo, fileInfo.oldReplica);
            } catch (OSDDrainException e) {
                // In case the old replica could not be removed inform the user to intervene manually before redraining.
                // TODO(jdillmann): resolve fileID to path
                String message = "Could not remove the replica for file with id: " + fileInfo.fileID
                        + " from the OSD: " + osdUUID.toString() + "\n"
                        + "It is NOT SAFE to call xtfs_remove_osd again. Please remove the replica manually before "
                        + "continuing.\n"
                        + "Original error was:\n" + e.getMessage();
                throw new OSDDrainException(message, ErrorState.DRAIN_COORDINATED);
            }

            finishedFiles.add(fileInfo);
        }

        return uncoordinatedFiles;
    }

    /**
     * Create a new Replica for every fileID in fileIDList on a new OSD. OSDs will be chosen by get_suitable_osds MRC
     * call.
     *
     * @param fileIDList
     * @throws Exception
     */
    public List<FileInformation> createReplicasForFiles(List<FileInformation> fileInfos) throws OSDDrainException {

        LinkedList<FileInformation> finishedFileInfos = new LinkedList<FileInformation>();

        for (FileInformation fileInfo : fileInfos) {
            try {
                Replica replica = createReplicaForFile(fileInfo);
                fileInfo.newReplica = replica;

                addReplicaToFile(fileInfo, replica);
                finishedFileInfos.add(fileInfo);

            } catch (OSDDrainException e) {
                throw new OSDDrainException(e.getMessage(), ErrorState.CREATE_REPLICAS, fileInfos, finishedFileInfos);
            }
        }

        return finishedFileInfos;
    }

    private Replica createReplicaForFile(FileInformation fileInfo) throws OSDDrainException {
        // Get a suitable OSD for the new replica.
        RPCResponse<xtreemfs_get_suitable_osdsResponse> suitable_osdsResponseRPCResponse = null;
        xtreemfs_get_suitable_osdsResponse suitable_osdsResponse;
        try {
            xtreemfs_get_suitable_osdsRequest suitable_osdsRequest;
            suitable_osdsRequest = xtreemfs_get_suitable_osdsRequest.newBuilder()
                                                                    .setFileId(fileInfo.fileID)
                                                                    .setNumOsds(1)
                                                                    .build();
            suitable_osdsResponseRPCResponse = mrcClient.xtreemfs_get_suitable_osds(
                    fileInfo.mrcAddress, password, userCreds, suitable_osdsRequest);
            suitable_osdsResponse = suitable_osdsResponseRPCResponse.get();
        } catch (Exception e) {
            if (Logging.isDebug()) {
                Logging.logError(Logging.LEVEL_WARN, this, e);
            }
            throw new OSDDrainException("Could not get suitable OSDs for file with id: " + fileInfo.fileID,
                                        ErrorState.CREATE_REPLICAS);
        } finally {
            if (suitable_osdsResponseRPCResponse != null) {
                suitable_osdsResponseRPCResponse.freeBuffers();
            }
        }

        if (suitable_osdsResponse.getOsdUuidsCount() == 0) {
            throw new OSDDrainException("No suitable OSDs to replicate file with id: " + fileInfo.fileID,
                                        ErrorState.CREATE_REPLICAS);
        }

        // build new Replica
        // TODO: set stripe-width to 1 or decide what to do with stripe-width greater than 1 (if stripe-width is
        // greater than 1 all OSDs used in one of the other replicas couldn't be used again)
        // current solution is to use '1' for the striping width
        StripingPolicy oldSP = fileInfo.oldReplica.getStripingPolicy();
        StripingPolicy newSP = StripingPolicy.newBuilder()
                                             .setType(oldSP.getType())
                                             .setStripeSize(oldSP.getStripeSize())
                                             .setWidth(1)
                                             .build();
        Replica.Builder replica = Replica.newBuilder()
                                         .addOsdUuids(suitable_osdsResponse.getOsdUuids(0))
                                         .setStripingPolicy(newSP);

        if (fileInfo.isReplicaChangeCoordinated) {
            replica.setReplicationFlags(0);
        } else {
            replica.setReplicationFlags(ReplicationFlags.setRandomStrategy(ReplicationFlags.setFullReplica(0)));
        }

        return replica.build();
    }

    private void addReplicaToFile(FileInformation fileInfo, Replica replica) throws OSDDrainException {
        RPCResponse<xtreemfs_replica_addResponse> response = null;
        try {
            xtreemfs_replica_addRequest replica_addRequest = xtreemfs_replica_addRequest.newBuilder()
                                                                                        .setFileId(fileInfo.fileID)
                                                                                        .setNewReplica(replica)
                                                                                        .build();
            response = mrcClient.xtreemfs_replica_add(fileInfo.mrcAddress, password, userCreds,
                                                      replica_addRequest);
            xtreemfs_replica_addResponse response2 = response.get();
            fileInfo.expectedXLocSetVersion = response2.getExpectedXlocsetVersion();
            waitForXLocSetInstallation(fileInfo);
        } catch (Exception e) {
            if (Logging.isDebug()) {
                Logging.logError(Logging.LEVEL_WARN, this, e);
            }
            throw new OSDDrainException(
                    "Could not add replica to file with id: " + fileInfo.fileID + "\n" 
                    + "Original error was:\n" + e.getMessage(),
                    ErrorState.CREATE_REPLICAS);
        } finally {
            if (response != null) {
                response.freeBuffers();
            }
        }
    }

    void waitForXLocSetInstallation(FileInformation fileInfo) throws OSDDrainException {
        if (fileInfo.expectedXLocSetVersion == 0) {
            return;
        }


        xtreemfs_get_xlocsetRequest.Builder reqBuilder = xtreemfs_get_xlocsetRequest.newBuilder();
        reqBuilder.setFileId(fileInfo.fileID);

        try {
            boolean finished = false;
            while (!finished) {
                RPCResponse<XLocSet> response = null;
                XLocSet xLocSet;
                try {
                    response = mrcClient.xtreemfs_get_xlocset(fileInfo.mrcAddress, password, userCreds,
                            reqBuilder.build());
                    xLocSet = response.get();
                } finally {
                    response.freeBuffers();
                }

                if (xLocSet.getVersion() == fileInfo.expectedXLocSetVersion) {
                    fileInfo.expectedXLocSetVersion = 0;
                    finished = true;
                } else if (xLocSet.getVersion() > fileInfo.expectedXLocSetVersion) {
                    throw new OSDDrainException("Could not install the new xLocSet for file " + fileInfo.fileID,
                            ErrorState.WAIT_FOR_XLOCSET_INSTALLATION);
                }

                if (!finished) {
                    Thread.sleep(WAIT_FOR_XLOC_SET_INSTALLATION_DELAY_S * 1000);
                }
            }
        } catch (OSDDrainException e) {
            throw e;
        } catch (Exception e) {
            throw new OSDDrainException("Error while waiting for the xLocSet installation for file " + fileInfo.fileID,
                    ErrorState.WAIT_FOR_XLOCSET_INSTALLATION);
        }
    }

    /**
     * Set ReplicationUpdatePolicy to RONLY for all files in fileInfos. <br>
     *
     * @param fileInfos
     * @return
     * @throws Exception
     */
    public List<FileInformation> setReplicationUpdatePolicyRonly(List<FileInformation> fileInfos)
            throws OSDDrainException {

        List<FileInformation> finishedFileInfos = new LinkedList<FileInformation>();

        for (FileInformation fileInfo : fileInfos) {
            try {
                fileInfo.oldReplicationPolicy = changeReplicationUpdatePolicy(fileInfo,
                                                                              ReplicaUpdatePolicies
                                                                                      .REPL_UPDATE_PC_RONLY);
                finishedFileInfos.add(fileInfo);
            } catch (Exception e) {
                throw new OSDDrainException(e.getMessage(), ErrorState.SET_UPDATE_POLICY, fileInfos,
                                            finishedFileInfos);
            }
        }

        return finishedFileInfos;
    }

    /**
     * Set ReplicationUpdatePolicy to the initial value according to FileInformation.oldReplicationPolicy for all files
     * in fileInfos.
     *
     * @return
     */
    public List<FileInformation> resetReplicationUpdatePolicy(List<FileInformation> fileInfos)
            throws OSDDrainException {

        // List of files which ReplicationUpdatePolicy is already set successfully
        List<FileInformation> finishedFileInfos = new LinkedList<FileInformation>();
        // List of files which could not be reset.
        List<FileInformation> erroneousFiles = new LinkedList<FileInformation>();

        for (FileInformation fileInfo : fileInfos) {
            try {
                fileInfo.oldReplicationPolicy = this.changeReplicationUpdatePolicy(fileInfo,
                                                                                   fileInfo.oldReplicationPolicy);
                finishedFileInfos.add(fileInfo);
            } catch (Exception e) {
                erroneousFiles.add(fileInfo);
            }
        }

        if (!erroneousFiles.isEmpty()) {
            throw new OSDDrainException("Failed to reset read only attribute for some files.",
                                        ErrorState.UNSET_UPDATE_POLICY, fileInfos, finishedFileInfos);
        }

        return finishedFileInfos;
    }

    private String changeReplicationUpdatePolicy(FileInformation fileInfo, String policy) throws Exception {

        RPCResponse<xtreemfs_set_replica_update_policyResponse> respRepl = null;
        xtreemfs_set_replica_update_policyResponse replSetResponse = null;
        try {
            respRepl = mrcClient.xtreemfs_set_replica_update_policy(fileInfo.mrcAddress, password, userCreds,
                                                                    fileInfo.fileID, policy);
            replSetResponse = respRepl.get();
        } catch (Exception ioe) {
            if (Logging.isDebug()) {
                Logging.logError(Logging.LEVEL_WARN, this, ioe);
            }
            throw ioe;
        } finally {
            if (respRepl != null)
                respRepl.freeBuffers();
        }

        return replSetResponse.getOldUpdatePolicy();
    }

    /**
     * Set all files in fileIDList to read-only to be able to RO-replicate them. <br>
     *
     * @param fileIDList
     * @param mode
     *            true if you want to set the files to read-only mode, false otherwise
     * @throws Exception
     *
     * @return {@link LinkedList} with all fileIDs which already were set to the desired mode
     */
    public List<FileInformation> setFilesReadOnlyAttribute(List<FileInformation> fileInfos) throws OSDDrainException {

        // List of files which where set to read-only successfully
        List<FileInformation> finishedFileInfos = new LinkedList<FileInformation>();

        for (FileInformation fileInfo : fileInfos) {
            try {
                fileInfo.wasAlreadyReadOnly = setFileReadOnlyAttribute(fileInfo, true);
            } catch (OSDDrainException e) {
                throw new OSDDrainException(e.getMessage(), ErrorState.SET_RONLY, fileInfos, finishedFileInfos);
            }
            finishedFileInfos.add(fileInfo);
        }

        return finishedFileInfos;
    }

    /**
     * Reset the files read only attributes to the original value
     * @param fileInfos
     * @return
     * @throws OSDDrainException
     */
    public List<FileInformation> resetFilesReadOnlyAttribute(List<FileInformation> fileInfos) throws OSDDrainException {
        // List of files which where reset successfully.
        List<FileInformation> finishedFileInfos = new LinkedList<FileInformation>();
        // List of files which could not be reset.
        List<FileInformation> erroneousFiles = new LinkedList<FileInformation>();

        for (FileInformation fileInfo : fileInfos) {
            // Skip files that have been read only before draining.
            if (fileInfo.wasAlreadyReadOnly) {
                finishedFileInfos.add(fileInfo);
                continue;
            }

            try {
                setFileReadOnlyAttribute(fileInfo, false);
                finishedFileInfos.add(fileInfo);
            } catch (OSDDrainException e) {
                erroneousFiles.add(fileInfo);
            }
        }

        if (!erroneousFiles.isEmpty()) {
            throw new OSDDrainException("Failed to reset read only attribute for some files.", ErrorState.UNSET_RONLY,
                                        fileInfos, finishedFileInfos);
        }

        return finishedFileInfos;
    }

    /**
     * Set the read-only attribute for a single file.
     *
     * @param fileInfo
     * @param mode
     * @return true if the read-only attribute had been already set to the requested mode.
     * @throws OSDDrainException
     */
    boolean setFileReadOnlyAttribute(FileInformation fileInfo, boolean mode) throws OSDDrainException {
        RPCResponse<xtreemfs_set_read_only_xattrResponse> response = null;
        xtreemfs_set_read_only_xattrResponse setResponse = null;
        try {
            response = mrcClient
                    .xtreemfs_set_read_only_xattr(fileInfo.mrcAddress, password, userCreds, fileInfo.fileID, mode);
            setResponse = response.get();
        } catch (Exception e) {
            if (Logging.isDebug()) {
                Logging.logError(Logging.LEVEL_WARN, this, e);
            }
            throw new OSDDrainException(e.getMessage(), ErrorState.SET_RONLY);
        } finally {
            if (response != null)
                response.freeBuffers();
        }

        return (!setResponse.getWasSet());
    }

    /**
     * Read one byte from every file in fileInfo list to trigger replication. The byte will be read from the first
     * object on OSD(s) containing the new replica.
     *
     * @param fileInfos
     * @return
     * @throws Exception
     */
    public List<FileInformation> startReplication(List<FileInformation> fileInfos) throws OSDDrainException {

        List<FileInformation> finishedFileInfos = new LinkedList<FileInformation>();

        for (FileInformation fileInfo : fileInfos) {
            // get FileCredentials to be able to read from the file
            RPCResponse<FileCredentials> r1 = null;
            try {
                r1 = mrcClient.xtreemfs_get_file_credentials(fileInfo.mrcAddress, password, userCreds,
                                                             fileInfo.fileID);
                fileInfo.fileCredentials = r1.get();
            } catch (Exception e) {
                if (Logging.isDebug()) {
                    Logging.logError(Logging.LEVEL_WARN, this, e);
                }
                throw new OSDDrainException(e.getMessage(), ErrorState.WAIT_FOR_REPLICATION, fileInfos,
                                            finishedFileInfos);
            } finally {
                if (r1 != null)
                    r1.freeBuffers();
            }


            // read a single Byte from one object of every OSD the new replica
            // is assigned to to trigger replication
            StripingPolicyImpl spol = StripingPolicyImpl.getPolicy(fileInfo.newReplica, 0);
            for (int i = 0; i < fileInfo.newReplica.getOsdUuidsCount(); i++) {

                Iterator<Long> objs = spol.getObjectsOfOSD(i, 0, Long.MAX_VALUE);
                long obj = objs.next();

                RPCResponse<ObjectData> r2 = null;
                try {
                    InetSocketAddress osd = new ServiceUUID(fileInfo.newReplica.getOsdUuids(i), resolver)
                            .getAddress();
                    r2 = osdClient.read(osd, password, userCreds, fileInfo.fileCredentials, fileInfo.fileID,
                                        obj, 0, 0, 1);
                    r2.get();
                } catch (Exception e) {
                    if (Logging.isDebug()) {
                        Logging.logError(Logging.LEVEL_WARN, this, e);
                    }
                    throw new OSDDrainException(e.getMessage(), ErrorState.WAIT_FOR_REPLICATION, fileInfos,
                                                finishedFileInfos);
                } finally {
                    if (r2 != null)
                        r2.freeBuffers();
                }

            }

            finishedFileInfos.add(fileInfo);
        }
        return fileInfos;
    }

    /**
     * Polls OSDs regularly to discover if replication is complete. Blocks until this event happens.
     *
     * @param fileIDList
     * @throws Exception
     */
    public List<FileInformation> waitForReplicationToComplete(List<FileInformation> fileInfos)
            throws OSDDrainException {

        List<FileInformation> finishedFileInfos = new LinkedList<FileInformation>();
        List<FileInformation> toBeRemovedFileInfos = new LinkedList<FileInformation>();

        while (!fileInfos.isEmpty()) {
            for (FileInformation fileInfo : fileInfos) {
                String fileID = fileInfo.fileID;
                FileCredentials fc = fileInfo.fileCredentials;
                Replica replica = fileInfo.newReplica;

                boolean isReplicated = true;

                StripingPolicyImpl sp = StripingPolicyImpl.getPolicy(replica, 0);
                long lastObjectNo = sp.getObjectNoForOffset(fc.getXlocs().getReadOnlyFileSize() - 1);

                int osdRelPos = 0;
                for (String osdUUID : replica.getOsdUuidsList()) {

                    RPCResponse<ObjectList> r = null;
                    ObjectSet oSet = null;
                    try {
                        InetSocketAddress osdAddress = new ServiceUUID(osdUUID, resolver).getAddress();

                        r = osdClient.xtreemfs_internal_get_object_set(osdAddress, password, userCreds, fc,
                                                                       fileID);
                        ObjectList ol = r.get();

                        byte[] serializedBitSet = ol.getSet().toByteArray();
                        oSet = new ObjectSet(ol.getStripeWidth(), ol.getFirst(), serializedBitSet);
                    } catch (Exception e) {
                        if (Logging.isDebug()) {
                            Logging.logError(Logging.LEVEL_WARN, this, e);
                        }
                        List<FileInformation> allInfos = new LinkedList<FileInformation>();
                        allInfos.addAll(fileInfos);
                        allInfos.addAll(finishedFileInfos);
                        throw new OSDDrainException(e.getMessage(), ErrorState.WAIT_FOR_REPLICATION,
                                                    finishedFileInfos, allInfos);
                    } finally {
                        if (r != null) {
                            r.freeBuffers();
                        }
                    }
                    for (long objNo = osdRelPos; objNo <= lastObjectNo; objNo += sp.getWidth()) {
                        if (oSet.contains(objNo) == false)
                            isReplicated = false;
                    }
                }

                // TODO: Set is replicated Flag if replication is complete
                if (isReplicated) {
                    toBeRemovedFileInfos.add(fileInfo);
                    finishedFileInfos.add(fileInfo);
                }

            }

            fileInfos.removeAll(toBeRemovedFileInfos);
            toBeRemovedFileInfos.clear();

            if (fileInfos.isEmpty())
                return finishedFileInfos;

            Logging.logMessage(Logging.LEVEL_INFO, Category.tool, this,
                               "waiting %d secs for replication to be finished", WAIT_FOR_REPLICA_COMPLETE_DELAY_S);
            try {
                // wait until next poll
                Thread.sleep(WAIT_FOR_REPLICA_COMPLETE_DELAY_S * 1000);
            } catch (Exception e) {
                // ignore
            }
        }
        return finishedFileInfos;
    }

    /**
     * removes replicas of all file in fileIDList which are on osdUUID
     *
     * @param fileIDList
     * @param mrc
     * @param osdUUID
     * @throws InterruptedException
     * @throws IOException
     */
    public void removeOriginalFromReplica(List<FileInformation> fileInfos) throws OSDDrainException {

        List<FileInformation> finishedFileInfos = new LinkedList<FileInformation>();
        List<FileInformation> erroneousFiles = new LinkedList<FileInformation>();

        for (FileInformation fileInfo : fileInfos) {
            try {
                removeReplica(fileInfo, fileInfo.oldReplica);
                finishedFileInfos.add(fileInfo);
            } catch (OSDDrainException e) {
                erroneousFiles.add(fileInfo);
            }
        }

        if (!erroneousFiles.isEmpty()) {
            throw new OSDDrainException("Failed to remove original replicas for some files.\n"
                                                + "It is NOT SAFE to call xtfs_remove_osd again. Please remove the "
                                                + "replicas manually before continuing.",
                                        ErrorState.REMOVE_REPLICAS, fileInfos, finishedFileInfos);
        }
    }

    private void removeReplica(FileInformation fileInfo, Replica replica) throws OSDDrainException {
        RPCResponse<xtreemfs_replica_removeResponse> response = null;

        String headOSD = replica.getOsdUuids(0);
        xtreemfs_replica_removeRequest replica_removeRequest;
        replica_removeRequest = xtreemfs_replica_removeRequest.newBuilder()
                                                              .setFileId(fileInfo.fileID)
                                                              .setOsdUuid(headOSD)
                                                              .build();
        try {
            response = mrcClient
                    .xtreemfs_replica_remove(fileInfo.mrcAddress, password, userCreds, replica_removeRequest);
            xtreemfs_replica_removeResponse response2 = response.get();
            fileInfo.expectedXLocSetVersion = response2.getExpectedXlocsetVersion();
            waitForXLocSetInstallation(fileInfo);
        } catch (Exception e) {
            if (Logging.isDebug()) {
                Logging.logError(Logging.LEVEL_WARN, this, e);
            }
            throw new OSDDrainException("Could not remove replica from file with id" + fileInfo.fileID + "\n"
                    + "Original error was:\n" + e.getMessage(), ErrorState.REMOVE_REPLICAS);
        } finally {
            if (response != null)
                response.freeBuffers();
        }
        
    }


    /**
     * Shuts down the OSD which should be removed.
     */
    void shutdownOsd() throws OSDDrainException {

        RPCResponse<?> r = null;
        try {
            r = osdClient.xtreemfs_shutdown(osdUUID.getAddress(), password, userCreds);
            r.get();
        } catch (Exception e) {
            if (Logging.isDebug()) {
                Logging.logError(Logging.LEVEL_WARN, this, e);
            }
            throw new OSDDrainException(e.getMessage(), ErrorState.SHUTDOWN_OSD);
        } finally {
            if (r != null)
                r.freeBuffers();
        }
    }

    /**
     * Create original replicas again if the where removed before. Only used if an error occurs while removing
     * original replicas.
     */
    private void revertRemoveOriginalReplicas(List<FileInformation> fileInfos) throws OSDDrainException {

        List<FileInformation> finishedFileInfos = new LinkedList<FileInformation>();
        List<FileInformation> erroneousFiles = new LinkedList<FileInformation>();

        for (FileInformation fileInfo : fileInfos) {
            try {
                if (fileInfo.oldReplica != null) {
                    addReplicaToFile(fileInfo, fileInfo.oldReplica);
                }
                finishedFileInfos.add(fileInfo);

            } catch (OSDDrainException e) {
                erroneousFiles.add(fileInfo);
            }
        }

        if (!erroneousFiles.isEmpty()) {
            throw new OSDDrainException("Failed to recreate original replicas for some files.\n"
                                                + "It is NOT SAFE to call xtfs_remove_osd again. Please ensure the "
                                                + "files are properly replicated before continuing.",
                                        ErrorState.CREATE_REPLICAS, fileInfos, finishedFileInfos);
        }
    }

    /**
     * Remove the newly created replicas. Only used if an error occurs while creating the replicas.
     *
     * @param fileInfos
     * @throws OSDDrainException
     */
    private void removeNewReplicas(List<FileInformation> fileInfos) throws OSDDrainException {

        List<FileInformation> finishedFileInfos = new LinkedList<FileInformation>();
        List<FileInformation> erroneousFiles = new LinkedList<FileInformation>();

        for (FileInformation fileInfo : fileInfos) {
            try {
                if (fileInfo.newReplica != null) {
                    removeReplica(fileInfo, fileInfo.newReplica);
                }
                finishedFileInfos.add(fileInfo);

            } catch (OSDDrainException e) {
                erroneousFiles.add(fileInfo);
            }
        }

        if (!erroneousFiles.isEmpty()) {
            throw new OSDDrainException("Failed to remove new replicas for some files.\n"
                                                + "It is NOT SAFE to call xtfs_remove_osd again. Please remove the "
                                                + "replicas manually before continuing.",
                                        ErrorState.CREATE_REPLICAS, fileInfos, finishedFileInfos);
        }
    }

    /**
     * Handles all errors that can occur while removing an OSD. On error it tries to revert all changes that
     * are done since the error occurred by recursively calling itself. If another error occurs on this
     * procedure it prints changes that were made and not be reverted correctly.
     * 
     * @param ex
     *            - {@link OSDDrainException} that should be handled
     * @param printError
     *            - true if an error Message should be printed. First call should be true, all other recursive
     *            calls should be false.
     */
    public void handleException(OSDDrainException ex, boolean printError) {
        switch (ex.getErrorState()) {
        case INITIALIZATION:
            if (printError) {
                Logging.logMessage(Logging.LEVEL_ERROR, Category.tool, this,
                        "Failed to initialize connection");
                printError();
            }
            if (Logging.isDebug()) {
                Logging.logError(Logging.LEVEL_DEBUG, this, ex);
            }
            break;

        case GET_FILE_LIST:
            if (printError) {
                Logging.logMessage(Logging.LEVEL_ERROR, Category.tool, this,
                        "Failed to get filelist from OSD");
                printError();
            }
            if (Logging.isDebug()) {
                Logging.logError(Logging.LEVEL_DEBUG, this, ex);
            }
            break;

        case UPDATE_MRC_ADDRESSES:
            if (printError) {
                Logging.logMessage(Logging.LEVEL_ERROR, Category.tool, this,
                        "Failed to get all MRC Addresses from DIR");
                printError();
            }
            if (Logging.isDebug()) {
                Logging.logError(Logging.LEVEL_DEBUG, this, ex);
            }
            break;

        case REMOVE_NON_EXISTING_IDS:
            if (printError) {
                Logging.logMessage(Logging.LEVEL_ERROR, Category.tool, this,
                        "Failed to check if files exist on MRC");
                printError();
            }
            if (Logging.isDebug())
                Logging.logError(Logging.LEVEL_DEBUG, this, ex);
            break;

        case GET_REPLICA_INFO:
            if (printError) {
                Logging.logMessage(Logging.LEVEL_ERROR, Category.tool, this,
                        "Failed to to get replica info from MRC");
                printError();
            }
            if (Logging.isDebug())
                Logging.logError(Logging.LEVEL_DEBUG, this, ex);
            break;

        case SET_SERVICE_STATUS:
            if (printError) {
                Logging.logMessage(Logging.LEVEL_ERROR, Category.tool, this,
                        "ERROR: failed to set ServiceStatus for OSD");
                printError();
            }
            if (Logging.isDebug())
                Logging.logError(Logging.LEVEL_DEBUG, this, ex);
            break;

        case DRAIN_COORDINATED:
            if (printError) {
                Logging.logMessage(Logging.LEVEL_ERROR, Category.tool, this, ex.getMessage());
                printError();
            }
            if (Logging.isDebug())
                Logging.logError(Logging.LEVEL_DEBUG, this, ex);
            break;

        case SET_UPDATE_POLICY:
            if (printError) {
                Logging.logMessage(Logging.LEVEL_ERROR, Category.tool, this,
                        "Failed to set ReplicationUpdatePolicies");
                printError();
            }
            if (Logging.isDebug())
                Logging.logError(Logging.LEVEL_DEBUG, this, ex);

            Logging.logMessage(Logging.LEVEL_INFO, Category.tool, this,
                    "Trying to revert ReplicationUpdatePolicy changes...");
            try {
                this.resetReplicationUpdatePolicy(ex.getFileInfosCurrent());
                Logging.logMessage(Logging.LEVEL_INFO, Category.tool, this,
                        "DONE reverting ReplicationUpdatePolicy changes");
            } catch (OSDDrainException ode) {
                List<FileInformation> failedFileInfos = ode.getFileInfosAll();
                failedFileInfos.removeAll(ode.getFileInfosCurrent());
                String error = "Following files couldn't set back its originial "
                        + "ReplicationUpdatePolicy due to errors:";
                for (FileInformation fileInfo : failedFileInfos) {
                    error = error + "\n " + fileInfo.fileID;
                }
                Logging.logMessage(Logging.LEVEL_ERROR, Category.tool, this, error);
            }
            break;

        case SET_RONLY:
            if (printError) {
                Logging.logMessage(Logging.LEVEL_ERROR, Category.tool, this, "Failed to set files read-only");
                printError();
            }
            if (Logging.isDebug())
                Logging.logError(Logging.LEVEL_DEBUG, this, ex);

            Logging.logMessage(Logging.LEVEL_INFO, Category.tool, this,
                    "Trying to revert read-only mode changes...");
            try {
                resetFilesReadOnlyAttribute(ex.getFileInfosCurrent());
                Logging.logMessage(Logging.LEVEL_INFO, Category.tool, this,
                        "DONE reverting read-only mode changes");
            } catch (OSDDrainException ode) {
                List<FileInformation> failedFileInfos = ode.getFileInfosAll();
                failedFileInfos.removeAll(ode.getFileInfosCurrent());
                String error = "Following files couldn't set back its originial read-only mode:";
                for (FileInformation fileInfo : failedFileInfos) {
                    error = error + "\n " + fileInfo.fileID;
                }
                Logging.logMessage(Logging.LEVEL_ERROR, Category.tool, this, error);
            }

            this.handleException(
                    new OSDDrainException(ex.getMessage(), ErrorState.SET_UPDATE_POLICY,
                            ex.getFileInfosAll(), ex.getFileInfosAll()), false);
            break;

        case CREATE_REPLICAS:
            if (printError) {
                Logging.logMessage(Logging.LEVEL_ERROR, Category.tool, this, "Failed to create new replicas");
                printError();
            }
            if (Logging.isDebug())
                Logging.logError(Logging.LEVEL_DEBUG, this, ex);

            Logging.logMessage(Logging.LEVEL_INFO, Category.tool, this, "Trying to revert replica changes...");
            try {
                this.removeNewReplicas(ex.getFileInfosCurrent());
                Logging.logMessage(Logging.LEVEL_INFO, Category.tool, this, "DONE reverting replica changes");
            } catch (OSDDrainException ode) {
                List<FileInformation> failedFileInfos = ode.getFileInfosAll();
                failedFileInfos.removeAll(ode.getFileInfosCurrent());
                String error = ex.getMessage() +  "\n"
                        + "From following files the newly created replicas couldn't be removed:";
                for (FileInformation fileInfo : failedFileInfos) {
                    error = error + "\n " + fileInfo.fileID;
                }
                Logging.logMessage(Logging.LEVEL_ERROR, Category.tool, this, error);
            }

            this.handleException(
                    new OSDDrainException(ex.getMessage(), ErrorState.SET_RONLY, ex.getFileInfosAll(), ex
                            .getFileInfosAll()), false);
            break;

        case WAIT_FOR_REPLICATION:
            if (printError) {

                Logging.logMessage(Logging.LEVEL_ERROR, Category.tool, this, "Failed to replicate files");
                printError();
            }
            if (Logging.isDebug())
                Logging.logError(Logging.LEVEL_DEBUG, this, ex);

            Logging.logMessage(Logging.LEVEL_INFO, Category.tool, this, "Trying to remove yet replicated files...");
            try {
                this.removeNewReplicas(ex.getFileInfosAll());
                Logging.logMessage(Logging.LEVEL_INFO, Category.tool, this, "DONE removing yet replicated files");
            } catch (OSDDrainException ode) {
                List<FileInformation> failedFileInfos = ode.getFileInfosAll();
                failedFileInfos.removeAll(ode.getFileInfosCurrent());
                String error = ex.getMessage() +  "\n"
                        + "From following files the newly created replicas couldn't be removed:";
                for (FileInformation fileInfo : failedFileInfos) {
                    error = error + "\n " + fileInfo.fileID;
                }
                Logging.logMessage(Logging.LEVEL_ERROR, Category.tool, this, error);
            }
            this.handleException(
                    new OSDDrainException(ex.getMessage(), ErrorState.SET_RONLY, ex.getFileInfosAll(), ex
                            .getFileInfosAll()), false);

            break;

        case REMOVE_REPLICAS:
            if (printError) {
                Logging.logMessage(Logging.LEVEL_ERROR, Category.tool, this,
                        "Failed to remove original replicas");
                printError();
            }
            if (Logging.isDebug())
                Logging.logError(Logging.LEVEL_DEBUG, this, ex);

            Logging.logMessage(Logging.LEVEL_INFO, Category.tool, this,
                    "Trying to revert original replica changes...");
            try {
                this.revertRemoveOriginalReplicas(ex.getFileInfosCurrent());
                Logging.logMessage(Logging.LEVEL_INFO, Category.tool, this,
                        "DONE removing original replicate changes");
            } catch (OSDDrainException ode) {
                List<FileInformation> failedFileInfos = ode.getFileInfosAll();
                failedFileInfos.removeAll(ode.getFileInfosCurrent());
                String error = ex.getMessage() + "\n"
                        + "From the following files the changes to the original replica couldn't be reverted:";
                for (FileInformation fileInfo : failedFileInfos) {
                    error = error + "\n " + fileInfo.fileID;
                }
                Logging.logMessage(Logging.LEVEL_ERROR, Category.tool, this, error);
            }
            this.handleException(
                    new OSDDrainException(ex.getMessage(), ErrorState.CREATE_REPLICAS, ex.getFileInfosAll(),
                            ex.getFileInfosAll()), false);
            break;

        case UNSET_RONLY: {
            if (printError) {
                Logging.logMessage(Logging.LEVEL_ERROR, Category.tool, this,
                                   "Failed to set files back from read-only mode");
                printError();
            }
            if (Logging.isDebug())
                Logging.logError(Logging.LEVEL_DEBUG, this, ex);

            List<FileInformation> failedFileInfos = ex.getFileInfosAll();
            failedFileInfos.removeAll(ex.getFileInfosCurrent());
            String error = "Following files couldn't set back to read-only mode:";
            for (FileInformation fileInfo : failedFileInfos) {
                error = error + "\n " + fileInfo.fileID;
            }
            Logging.logMessage(Logging.LEVEL_ERROR, Category.tool, this, error);

            this.handleException(
                    new OSDDrainException(ex.getMessage(), ErrorState.REMOVE_REPLICAS, ex.getFileInfosAll(),
                                          ex.getFileInfosAll()), false);
            break;
        }

        case UNSET_UPDATE_POLICY: {
            if (printError) {
                Logging.logMessage(Logging.LEVEL_ERROR, Category.tool, this,
                                   "Failed to set ReplicationUpdatePolicy back to the original ones");
                printError();
            }

            if (Logging.isDebug()) {
                Logging.logError(Logging.LEVEL_DEBUG, this, ex);
            }

            List<FileInformation> failedFileInfos = ex.getFileInfosAll();
            failedFileInfos.removeAll(ex.getFileInfosCurrent());
            String error = "For the following files the changes to ReplicationUpdatePolicy couldn't be reverted:";
            for (FileInformation fileInfo : failedFileInfos) {
                error = error + "\n " + fileInfo.fileID;
            }
            Logging.logMessage(Logging.LEVEL_ERROR, Category.tool, this, error);

            // Rollback only the failed files.
            this.handleException(
                    new OSDDrainException(ex.getMessage(), ErrorState.REMOVE_REPLICAS, failedFileInfos,
                                          failedFileInfos), false);

            break;
        }

        case SHUTDOWN_OSD:
            if (printError) {
                Logging.logMessage(Logging.LEVEL_WARN, Category.tool, this,
                        "Couldn't shut down OSD with UUID=" + this.osdUUID.toString()
                                + "  but all object files are moved to other OSDs. It's safe to shutdown this OSD now.");
                System.out.println("WARNING: Couldn't shut down OSD with UUID=" + this.osdUUID.toString()
                        + "  but all object files are moved to other OSDs. It's safe to shutdown this OSD now.");
            }
            if (Logging.isDebug()) {
                Logging.logError(Logging.LEVEL_DEBUG, this, ex);
            }
            break;

        default:
            break;
        }
    }

    private void printError() {
        System.err.println("ERROR: An error occured during the OSD drain process. See logging output"
                + "for details. It is NOT save to shutdown the OSD.");
    }
}
