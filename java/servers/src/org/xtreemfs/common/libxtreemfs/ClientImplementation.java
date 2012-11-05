/*
 * Copyright (c) 2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.xtreemfs.common.HeartbeatThread;
import org.xtreemfs.common.KeyValuePairs;
import org.xtreemfs.common.libxtreemfs.RPCCaller.CallGenerator;
import org.xtreemfs.common.libxtreemfs.exceptions.AddressToUUIDNotFoundException;
import org.xtreemfs.common.libxtreemfs.exceptions.PosixErrorException;
import org.xtreemfs.common.libxtreemfs.exceptions.VolumeNotFoundException;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.dir.DIRClient;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.AuthPassword;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.AuthType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceDataMap;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceStatus;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceType;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.AccessControlPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SERVICES;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Volumes;
import org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_cleanup_get_resultsResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_cleanup_is_runningResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_cleanup_statusResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;

/**
 * Standard implementation of the client. Used only internally.
 */
public class ClientImplementation implements UUIDResolver, Client, AdminClient {

    /**
     * Contains all addresses of the DIR Service.
     */
    private UUIDIterator                           dirServiceAddresses       = null;

    /**
     * Auth of type AUTH_NONE which is required for most operations which do not check the authentication data
     * (except Create, Delete, ListVolume(s)).
     */
    private Auth                                   authBogus                 = null;

    /** The auth_type of this object will always be set to AUTH_NONE. */
    // TODO: change this when the DIR service supports real auth.
    private Auth                                   dirServiceAuth;

    /** These credentials will be used for messages to the DIR service. */
    private UserCredentials                        dirServiceUserCredentials = null;

    private SSLOptions                             dirServiceSSLOptions      = null;

    /** Options class which contains the log_level string and logfile path. */
    private Options                                options                   = null;

    private ConcurrentLinkedQueue<Volume>          listOpenVolumes           = null;

    private org.xtreemfs.common.uuids.UUIDResolver uuidResolver              = null;

    /**
     * A DIRServiceClient is a wrapper for an RPC Client.
     */
    private DIRServiceClient                       dirServiceClient          = null;

    /**
     * A OSDServiceClient is a wrapper for an RPC Client.
     */
    private OSDServiceClient                       osdServiceClient          = null;

    /**
     * DIRClient for communication between Client and DIR.
     */
    private DIRClient                              dirClient                 = null;

    /**
     * The RPC Client processes requests from a queue and executes callbacks in its thread.
     */
    private RPCNIOSocketClient                     networkClient             = null;

    /**
     * Random, non-persistent UUID to distinguish locks of different clients.
     */
    private String                                 clientUUID                = null;

    /**
     * Array with all DIR addresses.
     */
    private String[]                               dirAddresses              = null;

    protected ClientImplementation(String[] dirAddresses, UserCredentials userCredentials,
            SSLOptions sslOptions, Options options) {
        this.dirServiceUserCredentials = userCredentials;
        this.dirServiceSSLOptions = sslOptions;
        this.options = options;

        this.dirAddresses = dirAddresses;

        this.dirServiceAddresses = new UUIDIterator();

        for (String address : dirAddresses) {
            this.dirServiceAddresses.addUUID(address);
        }

        // Set bogus auth object
        this.authBogus = Auth.newBuilder().setAuthType(AuthType.AUTH_NONE).build();

        // Currently no AUTH is required to access the DIR
        this.dirServiceAuth = Auth.newBuilder().setAuthType(AuthType.AUTH_NONE).build();

        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "Created a new libxtreemfs Client "
                    + "object (version %s)", options.getVersion());
        }

        this.listOpenVolumes = new ConcurrentLinkedQueue<Volume>();
    }

    @Override
    public void start() throws Exception {
        this.networkClient = new RPCNIOSocketClient(this.dirServiceSSLOptions,
                this.options.getRequestTimeout_s() * 1000, this.options.getLingerTimeout_s() * 1000, "Client");
        this.networkClient.start();
        this.networkClient.waitForStartup();

        TimeSync tsInstance = TimeSync.initializeLocal(50);
        tsInstance.waitForStartup();

        this.dirServiceClient = new DIRServiceClient(this.networkClient, null);

        osdServiceClient = new OSDServiceClient(networkClient, null);

        this.clientUUID = Helper.generateVersion4UUID();
        assert (this.clientUUID != null && this.clientUUID != "");

        // Create nonSingleton uuidResolver to resolve UUIDs.

        InetSocketAddress[] isas = new InetSocketAddress[dirAddresses.length];

        for (int i = 0; i < dirAddresses.length; i++) {
            isas[i] = Helper.stringToInetSocketAddress(dirAddresses[i],
                    GlobalTypes.PORTS.DIR_PBRPC_PORT_DEFAULT.getNumber());
        }

        dirClient = new DIRClient(this.dirServiceClient, isas, this.options.getMaxTries(),
                this.options.getRetryDelay_s() * 1000);

        this.uuidResolver = org.xtreemfs.common.uuids.UUIDResolver.startNonSingelton(dirClient, 3600, 1000);

    }

    @Override
    public synchronized void shutdown() {
        for (Volume volume : this.listOpenVolumes) {
            volume.close();
        }

        // Shutdown UUID resolver
        org.xtreemfs.common.uuids.UUIDResolver.shutdown(this.uuidResolver);

        if (this.dirServiceClient != null) {
            try {
                this.networkClient.shutdown();
                this.networkClient.waitForShutdown();
            } catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
            }
        }
    }

    protected void closeVolume(Volume volume) {
        boolean removed = this.listOpenVolumes.remove(volume);
        assert (removed);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xtreemfs.common.libxtreemfs.Client#openVolume(java.lang.String,
     * org.xtreemfs.foundation.SSLOptions, org.xtreemfs.common.libxtreemfs.Options)
     */
    @Override
    public AdminVolume openVolume(String volumeName, SSLOptions sslOptions, Options options)
            throws AddressToUUIDNotFoundException, VolumeNotFoundException, IOException {
        UUIDIterator mrcUuidIterator = new UUIDIterator();
        volumeNameToMRCUUID(volumeName, mrcUuidIterator);

        VolumeImplementation volume = new VolumeImplementation(this, this.clientUUID, mrcUuidIterator,
                volumeName, sslOptions, options);
        volume.start();

        this.listOpenVolumes.add(volume);
        return volume;
    }

    public void createVolume(String mrcAddress, Auth auth, UserCredentials userCredentials, String volumeName)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException {

        List<KeyValuePair> volumeAttributes = new ArrayList<KeyValuePair>();
        createVolume(mrcAddress, auth, userCredentials, volumeName, 511, "", "",
                AccessControlPolicyType.ACCESS_CONTROL_POLICY_POSIX,
                StripingPolicyType.STRIPING_POLICY_RAID0, 128, 1, volumeAttributes);
    };

    public void createVolume(List<String> mrcAddresses, Auth auth, UserCredentials userCredentials,
            String volumeName) throws IOException, PosixErrorException, AddressToUUIDNotFoundException {

        List<KeyValuePair> volumeAttributes = new ArrayList<KeyValuePair>();
        createVolume(mrcAddresses, auth, userCredentials, volumeName, 511, "", "",
                AccessControlPolicyType.ACCESS_CONTROL_POLICY_POSIX,
                StripingPolicyType.STRIPING_POLICY_RAID0, 128, 1, volumeAttributes);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xtreemfs.common.libxtreemfs.Client#createVolume(java.lang.String,
     * org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth, org.xtreemfs.common.auth.UserCredentials,
     * java.lang.String, int, java.lang.String, java.lang.String, org.xtreemfs.pbrpc.generatedinterfaces
     * .GlobalTypes.AccessControlPolicyType,
     * org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType, int, int, java.util.List)
     */
    @Override
    public void createVolume(String mrcAddress, Auth auth, UserCredentials userCredentials,
            String volumeName, int mode, String ownerUsername, String ownerGroupname,
            AccessControlPolicyType accessPolicyType, StripingPolicyType defaultStripingPolicyType,
            int defaultStripeSize, int defaultStripeWidth, List<KeyValuePair> volumeAttributes)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        UUIDIterator iteratorWithAddresses = new UUIDIterator();
        iteratorWithAddresses.addUUID(mrcAddress);

        createVolume(iteratorWithAddresses, auth, userCredentials, volumeName, mode, ownerUsername,
                ownerGroupname, accessPolicyType, defaultStripingPolicyType, defaultStripeSize,
                defaultStripeWidth, volumeAttributes);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xtreemfs.common.libxtreemfs.Client#createVolume(java.lang.String,
     * org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth, org.xtreemfs.common.auth.UserCredentials,
     * java.lang.String, int, java.lang.String, java.lang.String, org.xtreemfs.pbrpc.generatedinterfaces
     * .GlobalTypes.AccessControlPolicyType,
     * org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType, int, int, java.util.List)
     */
    @Override
    public void createVolume(Auth auth, UserCredentials userCredentials, String volumeName, int mode,
            String ownerUsername, String ownerGroupname, AccessControlPolicyType accessPolicyType,
            StripingPolicyType defaultStripingPolicyType, int defaultStripeSize, int defaultStripeWidth,
            List<KeyValuePair> volumeAttributes) throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException {

        // access the list of MRCs
        ServiceSet mrcs = RPCCaller.<String, ServiceSet> syncCall(SERVICES.DIR,
                this.dirServiceUserCredentials, this.dirServiceAuth, this.options, this,
                this.dirServiceAddresses, true, null, new CallGenerator<String, ServiceSet>() {

                    @Override
                    public RPCResponse<ServiceSet> executeCall(InetSocketAddress server, Auth authHeader,
                            UserCredentials userCreds, String input) throws IOException {
                        return ClientImplementation.this.dirServiceClient.xtreemfs_service_get_by_type(
                                server, authHeader, userCreds, ServiceType.SERVICE_TYPE_MRC);
                    }
                });

        if (mrcs.getServicesCount() == 0) {
            throw new IOException("no MRC available for volume creation");
        }

        List<String> mrcAddresses = new ArrayList<String>();
        for (Service uuid : mrcs.getServicesList()) {
            mrcAddresses.add(uuidToAddress(uuid.getUuid()));
        }
        createVolume(mrcAddresses, auth, userCredentials, volumeName, mode, ownerUsername, ownerGroupname,
                accessPolicyType, defaultStripingPolicyType, defaultStripeSize, defaultStripeWidth,
                volumeAttributes);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xtreemfs.common.libxtreemfs.Client#createVolume(java.util.List,
     * org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth, org.xtreemfs.common.auth.UserCredentials,
     * java.lang.String, int, java.lang.String, java.lang.String, org.xtreemfs.pbrpc.generatedinterfaces
     * .GlobalTypes.AccessControlPolicyType,
     * org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType, int, int, java.util.List)
     */
    @Override
    public void createVolume(List<String> mrcAddresses, Auth auth, UserCredentials userCredentials,
            String volumeName, int mode, String ownerUsername, String ownerGroupname,
            AccessControlPolicyType accessPolicyType, StripingPolicyType defaultStripingPolicyType,
            int defaultStripeSize, int defaultStripeWidth, List<KeyValuePair> volumeAttributes)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        UUIDIterator iteratorWithAddresses = new UUIDIterator();
        iteratorWithAddresses.addUUIDs(mrcAddresses);
        createVolume(iteratorWithAddresses, auth, userCredentials, volumeName, mode, ownerUsername,
                ownerGroupname, accessPolicyType, defaultStripingPolicyType, defaultStripeSize,
                defaultStripeWidth, volumeAttributes);
    }

    private void createVolume(UUIDIterator mrcAddresses, Auth auth, UserCredentials userCredentials,
            String volumeName, int mode, String ownerUsername, String ownerGroupname,
            AccessControlPolicyType accessPolicyType, StripingPolicyType defaultStripingPolicyType,
            int defaultStripeSize, int defaultStripeWidth, List<KeyValuePair> volumeAttributes)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        final MRCServiceClient mrcClient = new MRCServiceClient(this.networkClient, null);

        StripingPolicy sp = StripingPolicy.newBuilder().setType(defaultStripingPolicyType)
                .setStripeSize(defaultStripeSize).setWidth(defaultStripeWidth).build();

        org.xtreemfs.pbrpc.generatedinterfaces.MRC.Volume volume = org.xtreemfs.pbrpc.generatedinterfaces.MRC.Volume
                .newBuilder().setName(volumeName).setMode(mode).setOwnerUserId(ownerUsername)
                .setOwnerGroupId(ownerGroupname).setAccessControlPolicy(accessPolicyType)
                .setDefaultStripingPolicy(sp).addAllAttrs(volumeAttributes).setId("").build();

        RPCCaller.<org.xtreemfs.pbrpc.generatedinterfaces.MRC.Volume, emptyResponse> syncCall(SERVICES.MRC,
                userCredentials, auth, this.options, this, mrcAddresses, true, volume,
                new CallGenerator<org.xtreemfs.pbrpc.generatedinterfaces.MRC.Volume, emptyResponse>() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public RPCResponse<emptyResponse> executeCall(InetSocketAddress server, Auth auth,
                            UserCredentials userCreds, org.xtreemfs.pbrpc.generatedinterfaces.MRC.Volume input)
                            throws IOException {
                        return mrcClient.xtreemfs_mkvol(server, auth, userCreds, input);
                    };
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xtreemfs.common.libxtreemfs.Client#deleteVolume(java.lang.String,
     * org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth, org.xtreemfs.common.auth.UserCredentials,
     * java.lang.String)
     */
    @Override
    public void deleteVolume(Auth auth, UserCredentials userCredentials, String volumeName)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException {

        ServiceSet s = RPCCaller.<String, ServiceSet> syncCall(SERVICES.DIR, this.dirServiceUserCredentials,
                this.dirServiceAuth, this.options, this, this.dirServiceAddresses, true, volumeName,
                new CallGenerator<String, ServiceSet>() {

                    @Override
                    public RPCResponse<ServiceSet> executeCall(InetSocketAddress server, Auth authHeader,
                            UserCredentials userCreds, String volumeName) throws IOException {
                        return ClientImplementation.this.dirServiceClient.xtreemfs_service_get_by_name(
                                server, authHeader, userCreds, volumeName);
                    }
                });

        if (s == null || s.getServicesCount() == 0) {
            throw new IOException("volume '" + volumeName + "' does not exist");
        }

        if (s != null) {
            final Service vol = s.getServices(0);
            final String mrcAddress = uuidToAddress(KeyValuePairs
                    .getValue(vol.getData().getDataList(), "mrc"));
            deleteVolume(mrcAddress, auth, userCredentials, volumeName);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xtreemfs.common.libxtreemfs.Client#deleteVolume(java.lang.String,
     * org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth, org.xtreemfs.common.auth.UserCredentials,
     * java.lang.String)
     */
    @Override
    public void deleteVolume(String mrcAddress, Auth auth, UserCredentials userCredentials, String volumeName)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        UUIDIterator iteratorWithAddresses = new UUIDIterator();
        iteratorWithAddresses.addUUID(mrcAddress);

        deleteVolume(iteratorWithAddresses, auth, userCredentials, volumeName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xtreemfs.common.libxtreemfs.Client#deleteVolume(java.util.List,
     * org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth, org.xtreemfs.common.auth.UserCredentials,
     * java.lang.String)
     */
    @Override
    public void deleteVolume(List<String> mrcAddresses, Auth auth, UserCredentials userCredentials,
            String volumeName) throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        UUIDIterator iteratorWithAddresses = new UUIDIterator();
        iteratorWithAddresses.addUUIDs(mrcAddresses);

        deleteVolume(iteratorWithAddresses, auth, userCredentials, volumeName);
    }

    private void deleteVolume(UUIDIterator mrcAddresses, Auth auth, UserCredentials userCredentials,
            String volumeName) throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        final MRCServiceClient mrcServiceClient = new MRCServiceClient(this.networkClient, null);

        RPCCaller.<String, emptyResponse> syncCall(SERVICES.MRC, userCredentials, auth, this.options, this,
                mrcAddresses, true, volumeName, new CallGenerator<String, emptyResponse>() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public RPCResponse<emptyResponse> executeCall(InetSocketAddress server, Auth authHeader,
                            UserCredentials userCreds, String input) throws IOException {
                        return mrcServiceClient.xtreemfs_rmvol(server, authHeader, userCreds, input);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xtreemfs.common.libxtreemfs.Client#listVolumes(java.lang.String)
     */
    @Override
    public Volumes listVolumes(String mrcAddress) throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException {
        UUIDIterator iteratorWithAddresses = new UUIDIterator();
        iteratorWithAddresses.addUUID(mrcAddress);
        return listVolumes(iteratorWithAddresses);
    }

    @Override
    public Volumes listVolumes(List<String> mrcAddresses) throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException {
        UUIDIterator iteratorWithAddresses = new UUIDIterator();
        iteratorWithAddresses.addUUIDs(mrcAddresses);
        return listVolumes(iteratorWithAddresses);
    }

    private Volumes listVolumes(UUIDIterator uuidIteratorWithAddresses) throws IOException,
            PosixErrorException, AddressToUUIDNotFoundException {
        final MRCServiceClient mrcServiceClient = new MRCServiceClient(this.networkClient, null);

        // use bogus user credentials
        UserCredentials userCredentials = UserCredentials.newBuilder().setUsername("xtreemfs").build();

        Volumes volumes = RPCCaller.<emptyRequest, Volumes> syncCall(SERVICES.MRC, userCredentials,
                this.authBogus, this.options, this, uuidIteratorWithAddresses, true,
                emptyRequest.getDefaultInstance(), new CallGenerator<emptyRequest, Volumes>() {
                    @Override
                    public RPCResponse<Volumes> executeCall(InetSocketAddress server, Auth authHeader,
                            UserCredentials userCreds, emptyRequest input) throws IOException {
                        return mrcServiceClient.xtreemfs_lsvol(server, authHeader, userCreds, input);
                    };
                });
        return volumes;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xtreemfs.common.libxtreemfs.Client#listVolumes()
     */
    @Override
    public Volumes listVolumes() throws IOException {
        ServiceSet sSet = RPCCaller.<String, ServiceSet> syncCall(SERVICES.DIR,
                this.dirServiceUserCredentials, this.dirServiceAuth, this.options, this,
                this.dirServiceAddresses, true, null, new CallGenerator<String, ServiceSet>() {

                    @Override
                    public RPCResponse<ServiceSet> executeCall(InetSocketAddress server, Auth authHeader,
                            UserCredentials userCreds, String input) throws IOException {
                        return ClientImplementation.this.dirServiceClient.xtreemfs_service_get_by_type(
                                server, authHeader, userCreds, ServiceType.SERVICE_TYPE_VOLUME);
                    }
                });

        UUIDIterator iteratorWithAddresses = new UUIDIterator();
        for (int i = 0; i < sSet.getServicesCount(); i++) {
            for (KeyValuePair kvp : sSet.getServices(i).getData().getDataList()) {
                if (kvp.getKey().substring(0, 3).equals("mrc")) {
                    String mrcUuid = kvp.getValue();
                    iteratorWithAddresses.addUUID(uuidToAddress(mrcUuid));
                    break;
                }
            }
        }
        return listVolumes(iteratorWithAddresses);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xtreemfs.common.libxtreemfs.Client#listVolumeNames()
     */
    @Override
    public String[] listVolumeNames() throws IOException {
        ServiceSet sSet = RPCCaller.<String, ServiceSet> syncCall(SERVICES.DIR,
                this.dirServiceUserCredentials, this.dirServiceAuth, this.options, this,
                this.dirServiceAddresses, true, null, new CallGenerator<String, ServiceSet>() {

                    @Override
                    public RPCResponse<ServiceSet> executeCall(InetSocketAddress server, Auth authHeader,
                            UserCredentials userCreds, String input) throws IOException {
                        return ClientImplementation.this.dirServiceClient.xtreemfs_service_get_by_type(
                                server, authHeader, userCreds, ServiceType.SERVICE_TYPE_VOLUME);
                    }
                });

        String[] volNames = new String[sSet.getServicesCount()];
        for (int i = 0; i < volNames.length; i++) {
            volNames[i] = sSet.getServices(i).getName();
        }
        return volNames;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xtreemfs.common.libxtreemfs.Client#listServers()
     */
    @Override
    public Map<String, Service> listServers() throws IOException, PosixErrorException {
        // access the list of servers at the DIR
        ServiceSet osds = RPCCaller.<String, ServiceSet> syncCall(SERVICES.DIR,
                this.dirServiceUserCredentials, this.dirServiceAuth, this.options, this,
                this.dirServiceAddresses, true, null, new CallGenerator<String, ServiceSet>() {

                    @Override
                    public RPCResponse<ServiceSet> executeCall(InetSocketAddress server, Auth authHeader,
                            UserCredentials userCreds, String input) throws IOException {
                        return ClientImplementation.this.dirServiceClient.xtreemfs_service_get_by_type(
                                server, authHeader, userCreds, ServiceType.SERVICE_TYPE_MIXED);
                    }
                });

        Map<String, Service> serviceConfigs = new HashMap<String, Service>();
        for (Service service : osds.getServicesList()) {
            if (service.getType() == ServiceType.SERVICE_TYPE_MRC
                    || service.getType() == ServiceType.SERVICE_TYPE_OSD) {
                serviceConfigs.put(uuidToAddress(service.getUuid()), service);
            }
        }

        return serviceConfigs;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xtreemfs.common.libxtreemfs.Client#listOSDsAndAttributes()
     */
    @Override
    public Map<String, Service> listOSDsAndAttributes() throws IOException, PosixErrorException {
        // access the list of OSDs
        ServiceSet osds = RPCCaller.<String, ServiceSet> syncCall(SERVICES.DIR,
                this.dirServiceUserCredentials, this.dirServiceAuth, this.options, this,
                this.dirServiceAddresses, true, null, new CallGenerator<String, ServiceSet>() {

                    @Override
                    public RPCResponse<ServiceSet> executeCall(InetSocketAddress server, Auth authHeader,
                            UserCredentials userCreds, String input) throws IOException {
                        return ClientImplementation.this.dirServiceClient.xtreemfs_service_get_by_type(
                                server, authHeader, userCreds, ServiceType.SERVICE_TYPE_OSD);
                    }
                });

        Map<String, Service> osdConfigs = new HashMap<String, Service>();
        for (Service service : osds.getServicesList()) {
            // // access the config files of each OSD
            // Configuration config = RPCCaller.<String, Configuration> syncCall(SERVICES.DIR,
            // this.dirServiceUserCredentials,
            // this.dirServiceAuth, this.options, this, this.dirServiceAddresses, true, uuid.getUuid(),
            // new CallGenerator<String, Configuration>() {
            // @Override
            // public RPCResponse<Configuration> executeCall(InetSocketAddress server, Auth authHeader,
            // UserCredentials userCreds, String input) throws IOException {
            // return ClientImplementation.this.dirServiceClient.xtreemfs_configuration_get(server,
            // authHeader, userCreds, input);
            // }
            // });
            osdConfigs.put(service.getUuid(), service);
        }

        return osdConfigs;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xtreemfs.common.libxtreemfs.UUIDResolver#uuidToAddress(java.lang. String, java.lang.String)
     */
    @Override
    public String uuidToAddress(String uuid) throws AddressToUUIDNotFoundException {

        // The uuid must never be empty.
        assert (!uuid.isEmpty());

        String address = "";

        ServiceUUID serviceUuid = new ServiceUUID(uuid, this.uuidResolver);
        try {
            serviceUuid.resolve();
            address = serviceUuid.getAddressString();
        } catch (UnknownUUIDException e) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                        "UUID: SERVICE NOT FOUND FOR UUID %S", uuid);
            }
            throw new AddressToUUIDNotFoundException(uuid);
        }

        return address;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xtreemfs.common.libxtreemfs.UUIDResolver#volumeNameToMRCUUID(java .lang.String,
     * java.lang.String)
     */
    @Override
    public String volumeNameToMRCUUID(String volumeName) throws VolumeNotFoundException,
            AddressToUUIDNotFoundException {
        assert (!volumeName.isEmpty());

        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this, "Searching MRC for volume %s",
                    volumeName);
        }

        // Check if there is an '@' in the volume. Everything behind the '@' is
        // the snapshot,
        // cut if off.
        String parsedVolumeName = parseVolumeName(volumeName);
        ServiceSet sSet = null;
        try {
            sSet = RPCCaller.<String, ServiceSet> syncCall(SERVICES.DIR, this.dirServiceUserCredentials,
                    this.dirServiceAuth, this.options, this, this.dirServiceAddresses, true,
                    parsedVolumeName, new CallGenerator<String, ServiceSet>() {

                        @Override
                        public RPCResponse<ServiceSet> executeCall(InetSocketAddress server, Auth authHeader,
                                UserCredentials userCreds, String input) throws IOException {
                            return ClientImplementation.this.dirServiceClient.xtreemfs_service_get_by_name(
                                    server, authHeader, userCreds, input);
                        }
                    });
        } catch (IOException e) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this, "volumeNameToMRCUUID: "
                        + "couldn't resolve mrc UUID for volumeName %s Reason: %s", volumeName,
                        e.getMessage());
                throw new VolumeNotFoundException(parsedVolumeName);
            }
        }

        // check if there is an service which is a VOLUME and then filter the
        // MRC of this volume
        // from its ServiceDataMap.
        String mrcUuid = "";
        for (Service service : sSet.getServicesList()) {
            if (service.getType().equals(ServiceType.SERVICE_TYPE_VOLUME)
                    && service.getName().equals(parsedVolumeName)) {
                for (KeyValuePair kvp : service.getData().getDataList()) {
                    if (kvp.getKey().substring(0, 3).equals("mrc")) {
                        mrcUuid = kvp.getValue();
                        break;
                    }
                }
                // don't check other services if there are any left when an
                // mrcUuid is already determined.
                if (!mrcUuid.isEmpty()) {
                    break;
                }
            }
        }

        // Error Handling
        if (mrcUuid.isEmpty()) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this, "No MRC found for volume $s.",
                        parsedVolumeName);
            }
            throw new VolumeNotFoundException(parsedVolumeName);
        }

        return mrcUuid;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xtreemfs.common.libxtreemfs.UUIDResolver#volumeNameToMRCUUID(java .lang.String,
     * org.xtreemfs.common.libxtreemfs.UUIDIterator)
     */
    @Override
    public void volumeNameToMRCUUID(String volumeName, UUIDIterator uuidIterator)
            throws VolumeNotFoundException, AddressToUUIDNotFoundException {

        assert (uuidIterator != null);
        assert (!volumeName.isEmpty());

        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this, "Searching MRC for volume %s",
                    volumeName);
        }

        // Check if there is a @ in the volume_name.
        // Everything behind the @ has to be removed as it identifies the
        // snapshot.
        String parsedVolumeName = parseVolumeName(volumeName);
        ServiceSet sSet = null;
        try {
            sSet = RPCCaller.<String, ServiceSet> syncCall(SERVICES.DIR, this.dirServiceUserCredentials,
                    this.dirServiceAuth, this.options, this, this.dirServiceAddresses, true,
                    parsedVolumeName, new CallGenerator<String, ServiceSet>() {
                        @Override
                        public RPCResponse<ServiceSet> executeCall(InetSocketAddress server, Auth authHeader,
                                UserCredentials userCreds, String input) throws IOException {
                            return ClientImplementation.this.dirServiceClient.xtreemfs_service_get_by_name(
                                    server, authHeader, userCreds, input);
                        }
                    });
        } catch (IOException e) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this, "volumeNameToMRCUUID: "
                        + "couldn't resolve mrc UUID for volumeName %s Reason: %s", volumeName,
                        e.getMessage());
                throw new VolumeNotFoundException(parsedVolumeName);
            }
        }

        // Iterate over ServiceSet to find an appropriate MRC
        boolean mrcFound = false;
        for (Service service : sSet.getServicesList()) {
            if (service.getType().equals(ServiceType.SERVICE_TYPE_VOLUME)
                    && service.getName().equals(parsedVolumeName)) {
                for (KeyValuePair kvp : service.getData().getDataList()) {
                    if (kvp.getKey().substring(0, 3).equals("mrc")) {
                        if (Logging.isDebug()) {
                            Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this, "MRC with UUID: %s"
                                    + " added (key: %s).", kvp.getValue(), kvp.getKey());
                        }
                        uuidIterator.addUUID(kvp.getValue());
                        mrcFound = true;
                    }
                }
            }
        }

        // Error handling
        if (!mrcFound) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this, "No MRC found for volume $s.",
                        parsedVolumeName);
            }
            throw new VolumeNotFoundException(parsedVolumeName);
        }
    }

    /**
     * If volumeName contains an '@', cut off everything after the at because this belongs to the snapshot.
     * The real volumename is everything before the '@'.
     * 
     * @param volumeName
     *            The volumeName that should be parsed
     * @return String The parsed volume name.
     * 
     */
    private String parseVolumeName(String volumeName) {

        int pos = 0;
        String parsedVolumeName;
        if ((pos = volumeName.indexOf('@')) == -1) {
            parsedVolumeName = volumeName;
        } else {
            parsedVolumeName = volumeName.substring(0, pos);
        }

        return parsedVolumeName;
    }

    // Methods for AdminClient

    /**
     * Returns a InetSocketAddress for the given OSD UUID.
     * 
     * @param uuid
     *            OSD UUID
     */
    private InetSocketAddress osdUUIDToInetSocketAddress(String uuid) throws AddressToUUIDNotFoundException {
        String osdAddr = uuidToAddress(uuid);
        return Helper.stringToInetSocketAddress(osdAddr, 0);
    }

    private Auth StringToAuth(String password) {
        return Auth.newBuilder().setAuthType(AuthType.AUTH_PASSWORD)
                .setAuthPasswd(AuthPassword.newBuilder().setPassword(password).build()).build();
    }

    public void startCleanUp(String osdUUID, String password, boolean remove, boolean deleteVolumes,
            boolean restore) throws IOException {
        RPCResponse<?> r = null;
        InetSocketAddress osdAddr = null;
        try {
            osdAddr = osdUUIDToInetSocketAddress(osdUUID);
            Auth pw = StringToAuth(password);
            r = osdServiceClient.xtreemfs_cleanup_start(osdAddr, pw, RPCAuthentication.userService, remove,
                    deleteVolumes, restore);
            r.get();
        } catch (Exception e) {
            throw new IOException("Cleanup could not be started on the given OSD, because: " + e.getMessage());
        } finally {
            if (r != null) {
                r.freeBuffers();
            }
        }

    }

    public void startVersionCleanUp(String osdUUID, String password) throws IOException {
        RPCResponse<?> r = null;
        InetSocketAddress osdAddr = null;
        try {
            osdAddr = osdUUIDToInetSocketAddress(osdUUID);
            Auth pw = StringToAuth(password);
            r = osdServiceClient.xtreemfs_cleanup_versions_start(osdAddr, pw, RPCAuthentication.userService);
            r.get();
        } catch (Exception e) {
            throw new IOException("Version cleanup could not be started on the given OSD, because: "
                    + e.getMessage());
        } finally {
            if (r != null) {
                r.freeBuffers();
            }
        }
    }

    public void stopCleanUp(String osdUUID, String password) throws IOException {
        RPCResponse<?> r = null;
        InetSocketAddress osdAddr = null;
        try {
            osdAddr = osdUUIDToInetSocketAddress(osdUUID);
            Auth pw = StringToAuth(password);
            r = osdServiceClient.xtreemfs_cleanup_stop(osdAddr, pw, RPCAuthentication.userService);
            r.get();
        } catch (Exception e) {
            throw new IOException("Cleanup could not be stopped on the given OSD, because: " + e.getMessage());
        } finally {
            if (r != null) {
                r.freeBuffers();
            }
        }
    }

    public boolean isRunningCleanUp(String osdUUID, String password) throws IOException {
        RPCResponse<xtreemfs_cleanup_is_runningResponse> r = null;
        InetSocketAddress osdAddr = null;
        try {
            osdAddr = osdUUIDToInetSocketAddress(osdUUID);
            Auth pw = StringToAuth(password);
            r = osdServiceClient.xtreemfs_cleanup_is_running(osdAddr, pw, RPCAuthentication.userService);
            return r.get().getIsRunning();
        } catch (Exception e) {
            Logging.logError(Logging.LEVEL_WARN, null, e);
            throw new IOException("Status-request for cleanup on the given OSD failed, because: "
                    + e.getMessage());
        } finally {
            if (r != null) {
                r.freeBuffers();
            }
        }
    }

    public String getCleanUpState(String osdUUID, String password) throws IOException {
        RPCResponse<xtreemfs_cleanup_statusResponse> r = null;
        InetSocketAddress osdAddr = null;
        try {
            osdAddr = osdUUIDToInetSocketAddress(osdUUID);
            Auth pw = StringToAuth(password);
            r = osdServiceClient.xtreemfs_cleanup_status(osdAddr, pw, RPCAuthentication.userService);
            return r.get().getStatus();
        } catch (Exception e) {
            throw new IOException("Cleanup status could not be retrieved, because: " + e.getMessage());
        } finally {
            if (r != null) {
                r.freeBuffers();
            }
        }
    }

    public List<String> getCleanUpResult(String osdUUID, String password) throws IOException {
        RPCResponse<xtreemfs_cleanup_get_resultsResponse> r = null;
        InetSocketAddress osdAddr = null;
        try {
            osdAddr = osdUUIDToInetSocketAddress(osdUUID);
            Auth pw = StringToAuth(password);
            r = osdServiceClient.xtreemfs_cleanup_get_results(osdAddr, pw, RPCAuthentication.userService);
            return r.get().getResultsList();
        } catch (Exception e) {
            throw new IOException("Cleanup results could not be retrieved, because: " + e.getMessage());
        } finally {
            if (r != null) {
                r.freeBuffers();
            }
        }
    }

    public ServiceSet getServiceByType(ServiceType serviceType) throws IOException {
        try {
            return dirClient.xtreemfs_service_get_by_type(null, RPCAuthentication.authNone,
                    RPCAuthentication.userService, serviceType);
        } catch (Exception ex) {
            throw new IOException("Services could not be retrieved, because:" + ex.getMessage());
        }
    }

    public void setOSDServiceStatus(String osdUUID, ServiceStatus serviceStatus) throws IOException {
        // get OSD services
        ServiceSet osdServiceSet = null;
        try {
            osdServiceSet = dirClient.xtreemfs_service_get_by_uuid(null, RPCAuthentication.authNone,
                    RPCAuthentication.userService, osdUUID);
        } catch (Exception e) {
            throw new IOException("unable to get OSD with UUID " + osdUUID + ", beacuse:" + e.getMessage());
        }
        if (osdServiceSet.getServicesCount() == 0) {
            throw new IOException("No OSD with UUID " + osdUUID + " available");
        }
        Service osdService = osdServiceSet.getServices(0);

        // change service status
        List<KeyValuePair> data = new LinkedList<KeyValuePair>(osdService.getData().getDataList());
        KeyValuePairs
                .putValue(data, HeartbeatThread.STATUS_ATTR, Integer.toString(serviceStatus.getNumber()));
        ServiceDataMap dataMap = ServiceDataMap.newBuilder().addAllData(data).build();
        osdService = osdService.toBuilder().setData(dataMap).build();

        // sent changed service status to DIR
        try {
            dirClient.xtreemfs_service_register(null, RPCAuthentication.authNone,
                    RPCAuthentication.userService, osdService);
        } catch (Exception e) {
            throw new IOException("Cannot set OSD's service status, because: " + e.getMessage());
        }
    }

    public ServiceStatus getOSDServiceStatus(String osdUUID) throws IOException {
        ServiceSet osdServiceSet = null;
        try {
            osdServiceSet = dirClient.xtreemfs_service_get_by_uuid(null, RPCAuthentication.authNone,
                    RPCAuthentication.userService, osdUUID);
        } catch (Exception e) {
            throw new IOException("unable to get OSD with UUID " + osdUUID + ", because:" + e.getMessage());
        }
        if (osdServiceSet.getServicesCount() == 0) {
            throw new IOException("No OSD with UUID " + osdUUID + " available");
        }
        Service osdService = osdServiceSet.getServices(0);
        String hbAttr = KeyValuePairs.getValue(osdService.getData().getDataList(),
                HeartbeatThread.STATUS_ATTR);
        return ServiceStatus.valueOf(Integer.valueOf(hbAttr));
    }

    public Set<String> getRemovedOsds() throws IOException {

        Set<String> removedOSDs = new TreeSet<String>();

        String status_removed = Integer.toString(ServiceStatus.SERVICE_STATUS_REMOVED.getNumber());
        ServiceSet servs = getServiceByType(ServiceType.SERVICE_TYPE_OSD);

        for (Service serv : servs.getServicesList()) {
            String hbAttr = KeyValuePairs.getValue(serv.getData().getDataList(), HeartbeatThread.STATUS_ATTR);
            if ((hbAttr != null) && hbAttr.equalsIgnoreCase(status_removed)) {
                removedOSDs.add(serv.getUuid());
            }
        }
        return removedOSDs;

    }
}
