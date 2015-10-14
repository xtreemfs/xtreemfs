/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.clients;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.xtreemfs.common.KeyValuePairs;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.dir.DIRClient;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceType;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.AccessControlPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Volumes;
import org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;

/**
 * 
 * @author bjko
 */
public class Client {
    
    private final RPCNIOSocketClient  mdClient, osdClient;
    
    private final InetSocketAddress[] dirAddress;
    
    private DIRClient          dirClient;
    
    private final UUIDResolver        uuidRes;
    
    private final Map<String, Volume>       volumeMap;
    
    public Client(InetSocketAddress[] dirAddresses, int requestTimeout, int connectionTimeout, SSLOptions ssl)
            throws Exception {
        this.dirAddress = dirAddresses;
        mdClient = new RPCNIOSocketClient(ssl, requestTimeout, connectionTimeout, "Client (dir)");
        osdClient = new RPCNIOSocketClient(ssl, requestTimeout, connectionTimeout, "Client (osd)");
        DIRServiceClient dirRpcClient = new DIRServiceClient(mdClient, dirAddress[0]);
        dirClient = new DIRClient(dirRpcClient, dirAddress, 100, 1000 * 15);
        TimeSync.initializeLocal(0);
        uuidRes = UUIDResolver.startNonSingelton(dirClient, 3600, 1000);
        volumeMap = new HashMap<String, Volume>();
    }
    
    public Volume getVolume(String volumeName, UserCredentials credentials) throws IOException {
        try {    
            String lookupVolumeName = volumeName;
            int snapNameIndex = volumeName.indexOf('@');
            if (snapNameIndex != -1)
                lookupVolumeName = volumeName.substring(0, snapNameIndex);
            
            final ServiceSet s = dirClient.xtreemfs_service_get_by_name(null, RPCAuthentication.authNone,
                RPCAuthentication.userService, lookupVolumeName);
            if (s.getServicesCount() == 0) {
                throw new IOException("volume '" + lookupVolumeName + "' does not exist");
            }
            final Service vol = s.getServices(0);
            final String mrcUUIDstr = KeyValuePairs.getValue(vol.getData().getDataList(), "mrc");
            final ServiceUUID mrc = new ServiceUUID(mrcUUIDstr, uuidRes);
            
            UserCredentials uc = credentials;
            if (uc == null) {
                List<String> grps = new ArrayList(1);
                grps.add("test");
                uc = UserCredentials.newBuilder().setUsername("test").addGroups("test").build();
            }
            
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "volume %s on MRC %s/%s", volumeName, mrcUUIDstr,
                mrc.getAddress());
            
            Volume v = volumeMap.get(volumeName);
            if (v == null) {
                v = new Volume(new OSDServiceClient(osdClient, null), new MRCServiceClient(mdClient, mrc
                        .getAddress()), volumeName, uuidRes, uc);
                volumeMap.put(volumeName, v);
            }
            return v;
            
        } catch (InterruptedException ex) {
            throw new IOException("operation was interrupted", ex);
        }
    }
    
    public void createVolume(String volumeName, Auth authentication, UserCredentials credentials,
        StripingPolicy sp, AccessControlPolicyType accessCtrlPolicy, int permissions) throws IOException {
        RPCResponse r2 = null;
        try {
            ServiceSet mrcs = dirClient.xtreemfs_service_get_by_type(null, RPCAuthentication.authNone, credentials,
                ServiceType.SERVICE_TYPE_MRC);
            
            if (mrcs.getServicesCount() == 0) {
                throw new IOException("no MRC available for volume creation");
            }
            
            String uuid = mrcs.getServices(0).getUuid();
            ServiceUUID mrcUUID = new ServiceUUID(uuid, uuidRes);
            
            MRCServiceClient m = new MRCServiceClient(mdClient, mrcUUID.getAddress());
            r2 = m.xtreemfs_mkvol(null, authentication, credentials, accessCtrlPolicy, sp, "", permissions,
                volumeName, credentials.getUsername(), credentials.getGroups(0),
                new LinkedList<KeyValuePair>(), 0);
            r2.get();
            
        } catch (InterruptedException ex) {
            throw new IOException("operation was interrupted", ex);
        } finally {
            if (r2 != null)
                r2.freeBuffers();
        }
    }
    
    public void createVolume(String volumeName, Auth authentication, UserCredentials credentials,
        StripingPolicy sp, AccessControlPolicyType accessCtrlPolicy, int permissions, String mrcUUID)
        throws IOException {
        
        RPCResponse<?> r = null;
        try {
            ServiceUUID uuid = new ServiceUUID(mrcUUID, uuidRes);
            uuid.resolve();
            
            MRCServiceClient m = new MRCServiceClient(mdClient, uuid.getAddress());
            r = m.xtreemfs_mkvol(uuid.getAddress(), authentication, credentials, accessCtrlPolicy, sp, "", permissions,
                volumeName, credentials.getUsername(), credentials.getGroups(0),
                new LinkedList<KeyValuePair>(), 0);
            r.get();
            
        } catch (InterruptedException ex) {
            throw new IOException("operation was interrupted", ex);
        } catch (UnknownUUIDException ex) {
            throw new IOException("mrc UUID was unknown", ex);
        } finally {
            if (r != null)
                r.freeBuffers();
        }
        
    }
    
    public void deleteVolume(String volumeName, Auth authentication, UserCredentials credentials)
        throws IOException {
        RPCResponse r2 = null;
        assert (credentials != null);
        try {
            final ServiceSet s = dirClient.xtreemfs_service_get_by_name(null, RPCAuthentication.authNone, credentials,
                volumeName);
            if (s.getServicesCount() == 0) {
                throw new IOException("volume '" + volumeName + "' does not exist");
            }
            final Service vol = s.getServices(0);
            final String mrcUUIDstr = KeyValuePairs.getValue(vol.getData().getDataList(), "mrc");
            final ServiceUUID mrc = new ServiceUUID(mrcUUIDstr, uuidRes);
            
            MRCServiceClient m = new MRCServiceClient(mdClient, mrc.getAddress());
            r2 = m.xtreemfs_rmvol(null, authentication, credentials, volumeName);
            r2.get();
            
        } catch (InterruptedException ex) {
            throw new IOException("operation was interrupted", ex);
        } finally {
            if (r2 != null)
                r2.freeBuffers();
        }
    }
    
    public String[] listVolumeNames(UserCredentials credentials) throws IOException {
        assert (credentials != null);
        try {
            final ServiceSet s = dirClient.xtreemfs_service_get_by_type(null, RPCAuthentication.authNone, credentials, ServiceType.SERVICE_TYPE_VOLUME);
            String[] volNames = new String[s.getServicesCount()];
            for (int i = 0; i < volNames.length; i++)
                volNames[i] = s.getServices(i).getName();
            
            return volNames;
            
        } catch (InterruptedException ex) {
            throw new IOException("operation was interrupted", ex);
        }
    }
    
    public String[] listVolumeNames(String mrcUUID, UserCredentials credentials) throws IOException {
        RPCResponse<Volumes> r = null;
        assert (credentials != null);
        try {
            final ServiceUUID mrc = new ServiceUUID(mrcUUID, uuidRes);
            
            MRCServiceClient m = new MRCServiceClient(mdClient, mrc.getAddress());
            r = m.xtreemfs_lsvol(null, RPCAuthentication.authNone, credentials);
            Volumes vols = r.get();
            
            String[] volNames = new String[vols.getVolumesCount()];
            for(int i = 0; i < volNames.length; i++)
                volNames[i] = vols.getVolumes(i).getName();
            
            return volNames;
            
        } catch (InterruptedException ex) {
            throw new IOException("operation was interrupted", ex);
        } finally {
            if (r != null)
                r.freeBuffers();
        }
    }
    
    public ServiceSet getRegistry() throws IOException {
        try {
            return dirClient.xtreemfs_service_get_by_type(null, RPCAuthentication.authNone,
                RPCAuthentication.userService, ServiceType.SERVICE_TYPE_MIXED);
        } catch (InterruptedException ex) {
            throw new IOException("operation was interrupted", ex);
        } 
    }
    
    public void start() throws Exception {
        mdClient.start();
        mdClient.waitForStartup();
        osdClient.start();
        osdClient.waitForStartup();
    }
    
    public synchronized void stop() {
        if (dirClient != null) {
            try {
                mdClient.shutdown();
                osdClient.shutdown();
                mdClient.waitForShutdown();
                osdClient.waitForShutdown();
                
                for (Volume v : volumeMap.values())
                    v.shutdown();
                
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                dirClient = null;
            }
        }
    }
    
    @Override
    public void finalize() {
        stop();
    }
}
