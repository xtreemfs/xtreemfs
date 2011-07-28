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
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.util.OutputUtils;
import org.xtreemfs.osd.drain.OSDDrainException.ErrorState;
import org.xtreemfs.osd.replication.ObjectSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.AddressMappingSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceDataMap;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceStatus;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.serviceRegisterResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replicas;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_check_file_existsRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_check_file_existsResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_get_suitable_osdsResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_set_read_only_xattrResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_set_replica_update_policyResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectData;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectList;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_internal_get_fileid_listResponse;

/**
 * Class that provides function to remove a OSD by moving all his files to other
 * OSDs.
 * 
 * @author bzcseife
 * 
 * <br>
 *         Mar 17, 2011
 */
public class OSDDrain {
    
    /**
     * Container class for all information you need to move a file from one OSD
     * to another.
     * 
     * <br>
     * Mar 17, 2011
     */
    public class FileInformation {
        public String            fileID;
        
        public InetSocketAddress mrcAddress;
        
        public FileCredentials   fileCredentials;
        
        public Replica           newReplica;
        
        // origReplica is necessary to restore the replicas if there is an error
        // while removing the
        // original replica
        public Replica           oldReplica;
        
        public Boolean           wasAlreadyReadOnly;
        
        public String            oldReplicationPolicy;
    }
    
    private DIRServiceClient      dirClient;
    
    private OSDServiceClient      osdClient;
    
    private ServiceUUID           osdUUID;
    
    private MRCServiceClient      mrcClient;
    
    // private List<InetSocketAddress> mrcAddresses;
    
    private List<FileInformation> fileInfos;
    
    private UserCredentials       userCreds = RPCAuthentication.userService;
    
    private Auth                  password;
    
    private UUIDResolver          resolver;
    
    public OSDDrain(DIRServiceClient dirClient, OSDServiceClient osdClient, MRCServiceClient mrcClient,
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
     */
    public void drain() {
        
        try {
            // set OSDServiceStatus to prevent further writing on this OSD
            this.setServiceStatus(ServiceStatus.SERVICE_STATUS_TO_BE_REMOVED);
            
            // get all files the OSD has
            fileInfos = this.getFileListOfOSD();
            
            // get address of MRC which is responsible for every file
            this.updateMRCAddresses(fileInfos);
            
            // remove fileIDs which has no entry on MRC. Can happen because
            // object files on OSDs
            // will be deleted delayed.
            
            fileInfos = this.removeNonExistingFileIDs(fileInfos);
            
            // set ReplicationUpdatePolicy to RONLY
            fileInfos = this.setReplicationUpdatePolicyRonly(fileInfos);
            
            // set Files read-only
            fileInfos = this.setFilesReadOnlyAttribute(fileInfos, true);
            
            // create replications
            fileInfos = this.createReplicasForFiles(fileInfos);
            
            // start replication
            fileInfos = this.startReplication(fileInfos);
            
            // wait for replication to be finished
            fileInfos = this.waitForReplicationToComplete(fileInfos);
            
            // remove replicas
            this.removeOriginalFromReplica(fileInfos);
            
            // set every file to read/write again which wasn't set to read-only
            // before
            LinkedList<FileInformation> toSetROList = new LinkedList<FileInformation>();
            for (FileInformation fileInfo : fileInfos) {
                if (!fileInfo.wasAlreadyReadOnly)
                    toSetROList.add(fileInfo);
            }
            this.setFilesReadOnlyAttribute(toSetROList, false);
            
            // set ReplicationUpdatePolicy to original value
            this.setReplicationPolicyToOriginal(fileInfos);
            
            // TODO: delete all files on osd
            
            // shutdown osd
            this.shutdownOsd();
            
        } catch (OSDDrainException e) {
            this.handleException(e, true);
        }
        
    }
    
    /**
     * Sets a new status to the Service with uuid. Throws Exception if something
     * went wrong and does nothing if the current status is equivalent to the
     * new status.
     * 
     * @param uuid
     * @param status
     * @throws Exception
     */
    public void setServiceStatus(DIR.ServiceStatus status) throws OSDDrainException {
        RPCResponse<ServiceSet> resp = null;
        ServiceSet sSet = null;
        try {
            resp = dirClient.xtreemfs_service_get_by_uuid(null, RPCAuthentication.authNone,
                RPCAuthentication.userService, osdUUID.toString());
            sSet = resp.get();
        } catch (Exception e) {
            Logging.logError(Logging.LEVEL_WARN, this, e);
            throw new OSDDrainException(e.getMessage(), ErrorState.SET_SERVICE_STATUS);
        } finally {
            if (resp != null)
                resp.freeBuffers();
        }
        
        if (sSet.getServicesCount() == 0) {
            System.out.println("no OSD with UUID " + this.osdUUID + " registered at directory service");
            System.exit(1);
        }
        
        Service serv = sSet.getServices(0);
        String serviceStatus = KeyValuePairs.getValue(serv.getData().getDataList(),
            HeartbeatThread.STATUS_ATTR);
        
        if (serviceStatus == null) {
            System.out.println("Service " + this.osdUUID + " is not registred at DIR.");
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
        
        ServiceDataMap dataMap = ServiceDataMap.newBuilder().addAllData(data2).build();
        
        serv = serv.toBuilder().setData(dataMap).build();
        
        RPCResponse<serviceRegisterResponse> resp2 = null;
        try {
            resp2 = dirClient.xtreemfs_service_register(null, RPCAuthentication.authNone,
                RPCAuthentication.userService, serv);
            resp2.get();
        } catch (Exception e) {
            if (Logging.isDebug()) {
                Logging.logError(Logging.LEVEL_WARN, this, e);
            }
            throw new OSDDrainException(e.getMessage(), ErrorState.SET_SERVICE_STATUS);
        } finally {
            if (resp2 != null)
                resp2.freeBuffers();
        }
    }
    
    /**
     * Returns a {@link LinkedList} of all fileIDs the OSD which will be removed
     * has
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
     * Get for every fileID in fileInfos the mrcAddress of the MRC responsible
     * for this file.
     * 
     * @param fileInfos
     */
    public void updateMRCAddresses(List<FileInformation> fileInfos) throws OSDDrainException {
        for (FileInformation fileInfo : fileInfos) {
            
            String volumeUUID = fileInfo.fileID.substring(0, fileInfo.fileID.indexOf(':'));
            
            RPCResponse<ServiceSet> r1 = null;
            ServiceSet sSet = null;
            String mrcUUIDString = null;
            try {
                r1 = dirClient.xtreemfs_service_get_by_uuid(null, password, userCreds, volumeUUID);
                sSet = r1.get();
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
            } finally {
                if (r1 != null) {
                    r1.freeBuffers();
                }
            }
            
            RPCResponse<AddressMappingSet> r2 = null;
            AddressMappingSet ams = null;
            try {
                r2 = dirClient.xtreemfs_address_mappings_get(null, password, userCreds, mrcUUIDString);
                ams = r2.get();
                
                assert (ams != null);
                assert (ams.getMappings(0).getUuid().equalsIgnoreCase(mrcUUIDString));
                InetAddress inetAddr = InetAddress.getByName(ams.getMappings(0).getAddress());
                fileInfo.mrcAddress = new InetSocketAddress(inetAddr, ams.getMappings(0).getPort());
            } catch (Exception e) {
                if (Logging.isDebug()) {
                    Logging.logError(Logging.LEVEL_WARN, this, e);
                }
                throw new OSDDrainException(e.getMessage(), ErrorState.UPDATE_MRC_ADDRESSES);
            } finally {
                if (r2 != null) {
                    r2.freeBuffers();
                }
            }
            
        }
    }
    
    /**
     * Creates a new List<File> without fileIDs which are available on the OSD
     * but have no corresponding metadata entry on the MRC. This can happen
     * because on file deletion the object file will be removed later than the
     * metadata.
     * 
     * @param fileInfos
     * @return List of FilInformation with non-existing files removed.
     */
    public List<FileInformation> removeNonExistingFileIDs(List<FileInformation> fileInfos) {
        
        List<FileInformation> returnList = new LinkedList<FileInformation>();
        
        // Map with VolumeName as key and sublist of fileInfos as value. Used to
        // decrease the amount
        // of MRC queries.
        Map<String, List<FileInformation>> callMap = new HashMap<String, List<FileInformation>>();
        // Map to store VolID-> MRCAddress Mapping to know which MRC has to be
        // called.
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
            String bitMap = null;
            try {
                r = mrcClient.xtreemfs_check_file_exists(volIDMrcAddressMapping.get(entry.getKey()),
                    password, userCreds, fileExistsRequest.build());
                bitMap = r.get().getBitmap();
            } catch (Exception e) {
                if (Logging.isDebug()) {
                    Logging.logError(Logging.LEVEL_WARN, this, e);
                }
            } finally {
                if (r != null) {
                    r.freeBuffers();
                }
            }
            
            assert (bitMap != "2");
            
            for (int i = 0; i < bitMap.length(); i++) {
                if (bitMap.charAt(i) == '1')
                    returnList.add(entry.getValue().get(i));
            }
        }
        return returnList;
    }
    
    /**
     * Create a new Replica for every fileID in fileIDList on a new OSD. OSDs
     * will be chosen by get_suitable_osds MRC call.
     * 
     * @param fileIDList
     * @throws Exception
     */
    public List<FileInformation> createReplicasForFiles(List<FileInformation> fileInfos)
        throws OSDDrainException {
        
        LinkedList<FileInformation> finishedFileInfos = new LinkedList<FileInformation>();
        
        for (FileInformation fileInfo : fileInfos) {
            
            // get Striping Policy
            RPCResponse<Replicas> replResp = null;
            Replicas reps = null;
            try {
                replResp = mrcClient.xtreemfs_replica_list(fileInfo.mrcAddress, password, userCreds,
                    fileInfo.fileID, null, null);
                reps = replResp.get();
            } catch (Exception e) {
                if (Logging.isDebug()) {
                    Logging.logError(Logging.LEVEL_WARN, this, e);
                }
                throw new OSDDrainException(e.getMessage(), ErrorState.CREATE_REPLICAS, fileInfos,
                    finishedFileInfos);
            } finally {
                if (replResp != null)
                    replResp.freeBuffers();
            }
            
            Replica rep = reps.getReplicas(0);
            StripingPolicy sp = rep.getStripingPolicy();
            
            // get suitable OSD for new replica
            RPCResponse<xtreemfs_get_suitable_osdsResponse> sor = null;
            xtreemfs_get_suitable_osdsResponse suitOSDResp = null;
            try {
                sor = mrcClient.xtreemfs_get_suitable_osds(fileInfo.mrcAddress, password, userCreds,
                    fileInfo.fileID, null, null, 1);
                suitOSDResp = sor.get();
            } catch (Exception e) {
                if (Logging.isDebug()) {
                    Logging.logError(Logging.LEVEL_WARN, this, e);
                }
                throw new OSDDrainException(e.getMessage(), ErrorState.CREATE_REPLICAS, fileInfos,
                    finishedFileInfos);
            } finally {
                if (sor != null)
                    sor.freeBuffers();
            }
            
            if (suitOSDResp.getOsdUuidsCount() == 0) {
                throw new OSDDrainException("no suitable OSDs to replicate file with id" + fileInfo.fileID,
                    ErrorState.CREATE_REPLICAS, fileInfos, finishedFileInfos);
            }
            
            // build new Replica
            // TODO: set stripe-width to 1 or decide what to do with
            // stripe-width greater than 1 (if
            // stripe-width is greater than 1 all OSDs used in one of the other
            // replicas couldn't
            // be used again)
            Replica replica = Replica.newBuilder().addOsdUuids(suitOSDResp.getOsdUuids(0))
                    .setReplicationFlags(
                        ReplicationFlags.setRandomStrategy(ReplicationFlags.setFullReplica(0)))
                    .setStripingPolicy(sp).build();
            fileInfo.newReplica = replica;
            
            // add Replica
            RPCResponse<?> repAddResp = null;
            try {
                repAddResp = mrcClient.xtreemfs_replica_add(fileInfo.mrcAddress, password, userCreds,
                    fileInfo.fileID, null, null, replica);
                repAddResp.get();
                finishedFileInfos.add(fileInfo);
            } catch (Exception e) {
                if (Logging.isDebug()) {
                    Logging.logError(Logging.LEVEL_WARN, this, e);
                }
                throw new OSDDrainException(e.getMessage(), ErrorState.CREATE_REPLICAS, fileInfos,
                    finishedFileInfos);
            } finally {
                if (repAddResp != null)
                    repAddResp.freeBuffers();
            }
        }
        return finishedFileInfos;
    }
    
    /**
     * Set ReplicationUpdatePolicy to RONLY for all file in fileInfos
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
                
                fileInfo.oldReplicationPolicy = this.changeReplicationUpdatePolicy(fileInfo,
                    ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY);
                finishedFileInfos.add(fileInfo);
            } catch (Exception e) {
                throw new OSDDrainException(e.getMessage(), ErrorState.SET_UPDATE_POLICY, fileInfos,
                    finishedFileInfos);
            }
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
     * Set all files in fileIDList to read-only to be able to RO-replicate them
     * 
     * @param fileIDList
     * @param mode
     *            true if you want to set the files to read-only mode, false
     *            otherwise
     * @throws Exception
     * 
     * @return {@link LinkedList} with all fileIDs which already were set to the
     *         desired mode
     */
    public List<FileInformation> setFilesReadOnlyAttribute(List<FileInformation> fileInfos, boolean mode)
        throws OSDDrainException {
        
        // List of files which where set to read-only successfully
        List<FileInformation> finishedFileInfos = new LinkedList<FileInformation>();
        
        for (FileInformation fileInfo : fileInfos) {
            
            RPCResponse<xtreemfs_set_read_only_xattrResponse> resp = null;
            xtreemfs_set_read_only_xattrResponse setResponse = null;
            try {
                resp = mrcClient.xtreemfs_set_read_only_xattr(fileInfo.mrcAddress, password, userCreds,
                    fileInfo.fileID, mode);
                setResponse = resp.get();
            } catch (Exception e) {
                if (Logging.isDebug()) {
                    Logging.logError(Logging.LEVEL_WARN, this, e);
                }
                throw new OSDDrainException(e.getMessage(), ErrorState.SET_RONLY, fileInfos,
                    finishedFileInfos);
            } finally {
                if (resp != null)
                    resp.freeBuffers();
            }
            
            if (setResponse.getWasSet() == true) {
                fileInfo.wasAlreadyReadOnly = false;
            } else {
                fileInfo.wasAlreadyReadOnly = true;
            }
            finishedFileInfos.add(fileInfo);
        }
        
        return finishedFileInfos;
    }
    
    /**
     * Read one byte from every file in fileInfo list to trigger replication.
     * The byte will be read from the first object on OSD(s) containing the new
     * replica.
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
            // is assigned to to
            // trigger replication
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
     * Polls MRC regularly to discover if replication is complete. Blocks until
     * this event happens.
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
                        oSet = new ObjectSet(sp.getWidth(), osdRelPos, serializedBitSet);
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
                "waiting 10secs for replication to be finished");
            try {
                // wait 10sec until next poll
                Thread.sleep(10000);
            } catch (Exception e) {
                // ignore
            }
        }
        return finishedFileInfos;
    }
    
    /**
     * Set ReplicationUpdatePolicy to the initial value according to
     * FileInformation.oldReplicationPolicy for all files in fileInfos
     * 
     * @return
     */
    public List<FileInformation> setReplicationPolicyToOriginal(List<FileInformation> fileInfos)
        throws OSDDrainException {
        
        // List of files which ReplicationUpdatePolicy is already set
        // successfully
        List<FileInformation> finishedFileInfos = new LinkedList<FileInformation>();
        
        for (FileInformation fileInfo : fileInfos) {
            try {
                fileInfo.oldReplicationPolicy = this.changeReplicationUpdatePolicy(fileInfo,
                    fileInfo.oldReplicationPolicy);
                finishedFileInfos.add(fileInfo);
            } catch (Exception e) {
                throw new OSDDrainException(e.getMessage(), ErrorState.UNSET_UPDATE_POLICY, fileInfos,
                    finishedFileInfos);
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
        
        for (FileInformation fileInfo : fileInfos) {
            RPCResponse<FileCredentials> resp = null;
            try {
                resp = mrcClient.xtreemfs_replica_remove(fileInfo.mrcAddress, password, userCreds,
                    fileInfo.fileID, null, null, osdUUID.toString());
                resp.get();
                finishedFileInfos.add(fileInfo);
            } catch (Exception e) {
                if (Logging.isDebug()) {
                    Logging.logError(Logging.LEVEL_WARN, this, e);
                }
                throw new OSDDrainException(e.getMessage(), ErrorState.REMOVE_REPLICAS, fileInfos,
                    finishedFileInfos);
            } finally {
                if (resp != null)
                    resp.freeBuffers();
            }
        }
    }
    
    /**
     * Shuts down the OSD which should be removed.
     */
    public void shutdownOsd() throws OSDDrainException {
        
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
     * Create original replicas again if the where removed before. Only used if
     * an error occurs while removing original replicas.
     */
    
    private void revertRemoveOriginalReplicas(List<FileInformation> fileInfos) throws OSDDrainException {
        
        List<FileInformation> finishedFileInfos = new LinkedList<FileInformation>();
        
        for (FileInformation fileInfo : fileInfos) {
            
            RPCResponse<?> r = null;
            try {
                r = mrcClient.xtreemfs_replica_add(fileInfo.mrcAddress, password, userCreds, fileInfo.fileID,
                    null, null, fileInfo.oldReplica);
                r.get();
                finishedFileInfos.add(fileInfo);
            } catch (Exception e) {
                throw new OSDDrainException(e.getMessage(), ErrorState.CREATE_REPLICAS, fileInfos,
                    finishedFileInfos);
            }
        }
        
    }
    
    /**
     * Remove the newly created replicas. Only used if an error occurs while
     * creating the replicas.
     * 
     * @param fileInfos
     * @throws OSDDrainException
     */
    private void removeNewReplicas(List<FileInformation> fileInfos) throws OSDDrainException {
        
        List<FileInformation> finishedFileInfos = new LinkedList<FileInformation>();
        
        for (FileInformation fileInfo : fileInfos) {
            
            RPCResponse<FileCredentials> r = null;
            try {
                r = mrcClient.xtreemfs_replica_remove(fileInfo.mrcAddress, password, userCreds,
                    fileInfo.fileID, null, null, fileInfo.newReplica.getOsdUuids(0));
                r.get();
                finishedFileInfos.add(fileInfo);
            } catch (Exception e) {
                throw new OSDDrainException(e.getMessage(), ErrorState.CREATE_REPLICAS, fileInfos,
                    finishedFileInfos);
            }
        }
    }
    
    /**
     * Handles all errors that can occur while removing an OSD. On error it
     * tries to revert all changes that are done since the error occurred by
     * recursively calling itself. If another error occurs on this procedure it
     * prints changes that were made and not be reverted correctly.
     * 
     * @param ex
     *            - {@link OSDDrainException} that should be handled
     * @param printError
     *            - true if an error Message should be printed. First call
     *            should be true, all other recursive calls should be false.
     */
    public void handleException(OSDDrainException ex, boolean printError) {
        switch (ex.getErrorState()) {
        case INITIALIZATION:
            if (printError)
                Logging.logMessage(Logging.LEVEL_ERROR, Category.tool, this,
                    "Failed to initialize connection");
            if (Logging.isDebug()) {
                Logging.logError(Logging.LEVEL_DEBUG, this, ex);
            }
            break;
        
        case GET_FILE_LIST:
            if (printError)
                Logging.logMessage(Logging.LEVEL_ERROR, Category.tool, this,
                    "Failed to get filelist from OSD");
            if (Logging.isDebug()) {
                Logging.logError(Logging.LEVEL_DEBUG, this, ex);
            }
            break;
        
        case UPDATE_MRC_ADDRESSES:
            if (printError)
                Logging.logMessage(Logging.LEVEL_ERROR, Category.tool, this,
                    "Failed to get all MRC Addresses from DIR");
            if (Logging.isDebug()) {
                Logging.logError(Logging.LEVEL_DEBUG, this, ex);
            }
            break;
        
        case REMOVE_NON_EXISTING_IDS:
            if (printError)
                Logging.logMessage(Logging.LEVEL_ERROR, Category.tool, this,
                    "Failed to check if files exist on MRC");
            if (Logging.isDebug())
                Logging.logError(Logging.LEVEL_DEBUG, this, ex);
            break;
        
        case SET_SERVICE_STATUS:
            if (printError)
                Logging.logMessage(Logging.LEVEL_ERROR, Category.tool, this,
                    "ERROR: failed to set ServiceStatus for OSD");
            if (Logging.isDebug())
                Logging.logError(Logging.LEVEL_DEBUG, this, ex);
            break;
        
        case SET_UPDATE_POLICY:
            if (printError)
                Logging.logMessage(Logging.LEVEL_ERROR, Category.tool, this,
                    "Failed to set ReplicationUpdatePolicies");
            if (Logging.isDebug())
                Logging.logError(Logging.LEVEL_DEBUG, this, ex);
            
            Logging.logMessage(Logging.LEVEL_INFO, Category.tool, this,
                "Trying to revert ReplicationUpdatePolicy changes...");
            try {
                this.setReplicationPolicyToOriginal(ex.getFileInfosCurrent());
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
            if (printError)
                Logging.logMessage(Logging.LEVEL_ERROR, Category.tool, this, "Failed to set files read-only");
            if (Logging.isDebug())
                Logging.logError(Logging.LEVEL_DEBUG, this, ex);
            
            Logging.logMessage(Logging.LEVEL_INFO, Category.tool, this,
                "Trying to revert read-only mode changes...");
            try {
                this.setFilesReadOnlyAttribute(ex.getFileInfosCurrent(), false);
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
            
            this.handleException(new OSDDrainException(ex.getMessage(), ErrorState.SET_UPDATE_POLICY, ex
                    .getFileInfosAll(), ex.getFileInfosAll()), false);
            break;
        
        case CREATE_REPLICAS:
            if (printError)
                Logging.logMessage(Logging.LEVEL_ERROR, Category.tool, this, "Failed to create new replicas");
            if (Logging.isDebug())
                Logging.logError(Logging.LEVEL_DEBUG, this, ex);
            
            Logging
                    .logMessage(Logging.LEVEL_INFO, Category.tool, this,
                        "Trying to revert replica changes...");
            try {
                this.removeNewReplicas(ex.getFileInfosCurrent());
                Logging.logMessage(Logging.LEVEL_INFO, Category.tool, this, "DONE reverting replica changes");
            } catch (OSDDrainException ode) {
                List<FileInformation> failedFileInfos = ode.getFileInfosAll();
                failedFileInfos.removeAll(ode.getFileInfosCurrent());
                String error = "From following files the newly created replicas couldn't be removed:";
                for (FileInformation fileInfo : failedFileInfos) {
                    error = error + "\n " + fileInfo.fileID;
                }
                Logging.logMessage(Logging.LEVEL_ERROR, Category.tool, this, error);
            }
            
            this.handleException(new OSDDrainException(ex.getMessage(), ErrorState.SET_RONLY, ex
                    .getFileInfosAll(), ex.getFileInfosAll()), false);
            break;
        
        case WAIT_FOR_REPLICATION:
            if (printError)
                Logging.logMessage(Logging.LEVEL_ERROR, Category.tool, this, "Failed to replicate files");
            if (Logging.isDebug())
                Logging.logError(Logging.LEVEL_DEBUG, this, ex);
            
            Logging.logMessage(Logging.LEVEL_INFO, Category.tool, this,
                "Trying to remove yet replicated files...");
            try {
                this.removeNewReplicas(ex.getFileInfosAll());
                Logging.logMessage(Logging.LEVEL_INFO, Category.tool, this,
                    "DONE removing yet replicated files");
            } catch (OSDDrainException ode) {
                List<FileInformation> failedFileInfos = ode.getFileInfosAll();
                failedFileInfos.removeAll(ode.getFileInfosCurrent());
                String error = "From following files the newly created replicas couldn't be removed:";
                for (FileInformation fileInfo : failedFileInfos) {
                    error = error + "\n " + fileInfo.fileID;
                }
                Logging.logMessage(Logging.LEVEL_ERROR, Category.tool, this, error);
            }
            this.handleException(new OSDDrainException(ex.getMessage(), ErrorState.SET_RONLY, ex
                    .getFileInfosAll(), ex.getFileInfosAll()), false);
            break;
        
        case REMOVE_REPLICAS:
            if (printError)
                Logging.logMessage(Logging.LEVEL_ERROR, Category.tool, this,
                    "Failed to remove original replicas");
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
                String error = "From the following files the changes to the original"
                    + "replica couldn't be reverted:";
                for (FileInformation fileInfo : failedFileInfos) {
                    error = error + "\n " + fileInfo.fileID;
                }
                Logging.logMessage(Logging.LEVEL_ERROR, Category.tool, this, error);
            }
            this.handleException(new OSDDrainException(ex.getMessage(), ErrorState.CREATE_REPLICAS, ex
                    .getFileInfosAll(), ex.getFileInfosAll()), false);
            break;
        
        case UNSET_RONLY:
            if (printError)
                Logging.logMessage(Logging.LEVEL_ERROR, Category.tool, this,
                    "Failed to set files back from read-only mode");
            if (Logging.isDebug())
                Logging.logError(Logging.LEVEL_DEBUG, this, ex);
            
            Logging.logMessage(Logging.LEVEL_INFO, Category.tool, this,
                "Trying to revert changes to read-only mode...");
            try {
                this.setFilesReadOnlyAttribute(ex.getFileInfosCurrent(), true);
                Logging.logMessage(Logging.LEVEL_INFO, Category.tool, this,
                    "DONE reverting changes to read-only mode");
            } catch (OSDDrainException roe) {
                List<FileInformation> failedFileInfos = roe.getFileInfosAll();
                failedFileInfos.removeAll(roe.getFileInfosCurrent());
                String error = "Following files couldn't set back to read-only mode:";
                for (FileInformation fileInfo : failedFileInfos) {
                    error = error + "\n " + fileInfo.fileID;
                }
                Logging.logMessage(Logging.LEVEL_ERROR, Category.tool, this, error);
            }
            this.handleException(new OSDDrainException(ex.getMessage(), ErrorState.UNSET_RONLY, ex
                    .getFileInfosAll(), ex.getFileInfosAll()), false);
            break;
        
        case UNSET_UPDATE_POLICY:
            if (printError)
                Logging.logMessage(Logging.LEVEL_ERROR, Category.tool, this,
                    "Failed to set ReplicationUpdatePolicy back to the original ones");
            if (Logging.isDebug()) {
                Logging.logError(Logging.LEVEL_DEBUG, this, ex);
            }
            
            Logging.logMessage(Logging.LEVEL_INFO, Category.tool, this,
                "Trying to revert changes to ReplicationUpdatePolicies...");
            try {
                this.setReplicationUpdatePolicyRonly(ex.getFileInfosCurrent());
                Logging.logMessage(Logging.LEVEL_INFO, Category.tool, this,
                    "DONE reverting changes to ReplicationUpdatePolicies");
            } catch (OSDDrainException roe) {
                List<FileInformation> failedFileInfos = roe.getFileInfosAll();
                failedFileInfos.removeAll(roe.getFileInfosCurrent());
                String error = "For the following files the changes to "
                    + "ReplicationUpdatePolicy couldn't be reverted:";
                for (FileInformation fileInfo : failedFileInfos) {
                    error = error + "\n " + fileInfo.fileID;
                }
                Logging.logMessage(Logging.LEVEL_ERROR, Category.tool, this, error);
            }
            this.handleException(new OSDDrainException(ex.getMessage(), ErrorState.UNSET_UPDATE_POLICY, ex
                    .getFileInfosAll(), ex.getFileInfosAll()), false);
            break;
        
        case SHUTDOWN_OSD:
            if (printError)
                Logging.logMessage(Logging.LEVEL_WARN, Category.tool, this,
                    "Couldn't shut down OSD with UUID=" + this.osdUUID.toString()
                        + "  but all files are removed this OSD. It's safe to shutdown the OSD now.");
            if (Logging.isDebug()) {
                Logging.logError(Logging.LEVEL_DEBUG, this, ex);
            }
            break;
        
        default:
            break;
        }
    }
}
