/*
 * Copyright (c) 2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.xtreemfs.common.libxtreemfs.RPCCaller.CallGenerator;
import org.xtreemfs.common.libxtreemfs.exceptions.VolumeNotFoundException;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.dir.DIRClient;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.AuthType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
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

/**
 * 
 * <br>
 * Sep 2, 2011
 */
public class ClientImplementation extends Client {

    /**
     * Contains all addresses of the DIR Service
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
     * The RPC Client processes requests from a queue and executes callbacks in its thread.
     */
    private RPCNIOSocketClient                     networkClient             = null;

    /**
     * Random, non-persistent UUID to distinguish locks of different clients.
     */
    private String                                 clientUUID                = null;

    /**
     * 
     */
    public ClientImplementation(String dirAddress, UserCredentials userCredentials, SSLOptions sslOptions,
            Options options) {
        dirServiceUserCredentials = userCredentials;
        dirServiceSSLOptions = sslOptions;
        this.options = options;

        dirServiceAddresses = new UUIDIterator();

        dirServiceAddresses.addUUID(dirAddress);

        // Set bogus auth object
        authBogus = Auth.newBuilder().setAuthType(AuthType.AUTH_NONE).build();

        // Currently no AUTH is required to access the DIR
        dirServiceAuth = Auth.newBuilder().setAuthType(AuthType.AUTH_NONE).build();

        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "Created a new libxtreemfs Client "
                    + "object (version %s)", options.getVersion());
        }

        listOpenVolumes = new ConcurrentLinkedQueue<Volume>();
    }

    public void start() throws Exception {
        networkClient = new RPCNIOSocketClient(dirServiceSSLOptions, options.getRequestTimeout_s(),
                options.getConnectTimeout_s());
        networkClient.start();
        networkClient.waitForStartup();

        dirServiceClient = new DIRServiceClient(networkClient, null);

        clientUUID = Helper.generateVersion4UUID();
        assert (clientUUID != null && clientUUID != "");

        // Create nonSingleton uuidResolver to resolve UUIDs.

        // TODO: Add all dirServiceAddresses to the array
        InetSocketAddress[] isas = new InetSocketAddress[dirServiceAddresses.size()];
        isas[0] = Helper.stringToInetSocketAddress(dirServiceAddresses.getUUID(),
                GlobalTypes.PORTS.DIR_PBRPC_PORT_DEFAULT.getNumber());

        DIRClient dirClient = new DIRClient(dirServiceClient, isas, options.getMaxTries(),
                options.getRetryDelay_s() * 1000);

        uuidResolver = org.xtreemfs.common.uuids.UUIDResolver.startNonSingelton(dirClient, 3600, 1000);

    }

    public synchronized void shutdown() {
        for (Volume volume : listOpenVolumes) {
            volume.close();
        }

        // Shutdown UUID resolver
        org.xtreemfs.common.uuids.UUIDResolver.shutdown(uuidResolver);

        if (dirServiceClient != null) {
            try {
                networkClient.shutdown();
                networkClient.waitForShutdown();
            } catch (Exception e) {
                // TODO: handle exception
            	e.printStackTrace();
            }
        }
    }

    protected void closeVolume(Volume volume) {
        boolean removed = listOpenVolumes.remove(volume);
        assert (removed);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xtreemfs.common.libxtreemfs.Client#openVolume(java.lang.String,
     * org.xtreemfs.foundation.SSLOptions, org.xtreemfs.common.libxtreemfs.Options)
     */
    @Override
    public Volume openVolume(String volumeName, SSLOptions sslOptions, Options options) throws Exception {

        UUIDIterator mrcUuidIterator = new UUIDIterator();
        volumeNameToMRCUUID(volumeName, mrcUuidIterator);

        VolumeImplementation volume = new VolumeImplementation(this, clientUUID, mrcUuidIterator, volumeName,
                sslOptions, options);
        volume.start();

        listOpenVolumes.add(volume);

        return volume;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xtreemfs.common.libxtreemfs.Client#createVolume(java.lang.String,
     * org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth, org.xtreemfs.common.auth.UserCredentials,
     * java.lang.String, int, java.lang.String, java.lang.String,
     * org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.AccessControlPolicyType,
     * org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType, int, int, java.util.List)
     */
    @Override
    public void createVolume(String mrcAddress, Auth auth, UserCredentials userCredentials,
            String volumeName, int mode, String ownerUsername, String ownerGroupname,
            AccessControlPolicyType accessPolicyType, StripingPolicyType defaultStripingPolicyType,
            int defaultStripeSize, int defaultStripeWidth, List<KeyValuePair> volumeAttributes)
            throws IOException {
        UUIDIterator iteratorWithAddresses = new UUIDIterator();
        iteratorWithAddresses.addUUID(mrcAddress);

        createVolume(iteratorWithAddresses, auth, userCredentials, volumeName, mode, ownerUsername,
                ownerGroupname, accessPolicyType, defaultStripingPolicyType, defaultStripeSize,
                defaultStripeWidth, volumeAttributes);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xtreemfs.common.libxtreemfs.Client#createVolume(java.util.List,
     * org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth, org.xtreemfs.common.auth.UserCredentials,
     * java.lang.String, int, java.lang.String, java.lang.String,
     * org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.AccessControlPolicyType,
     * org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType, int, int, java.util.List)
     */
    @Override
    public void createVolume(List<String> mrcAddresses, Auth auth, UserCredentials userCredentials,
            String volumeName, int mode, String ownerUsername, String ownerGroupname,
            AccessControlPolicyType accessPolicyType, StripingPolicyType defaultStripingPolicyType,
            int defaultStripeSize, int defaultStripeWidth, List<KeyValuePair> volumeAttributes)
            throws IOException {
        UUIDIterator iteratorWithAddresses = new UUIDIterator();
        iteratorWithAddresses.addUUIDs(mrcAddresses);
        createVolume(iteratorWithAddresses, auth, userCredentials, volumeName, mode, ownerUsername, ownerGroupname,
                accessPolicyType, defaultStripingPolicyType, defaultStripeSize, defaultStripeWidth,
                volumeAttributes);
    }

    private void createVolume(UUIDIterator mrcAddresses, Auth auth, UserCredentials userCredentials,
            String volumeName, int mode, String ownerUsername, String ownerGroupname,
            AccessControlPolicyType accessPolicyType, StripingPolicyType defaultStripingPolicyType,
            int defaultStripeSize, int defaultStripeWidth, List<KeyValuePair> volumeAttributes)
            throws IOException {
        final MRCServiceClient mrcClient = new MRCServiceClient(networkClient, null);

        StripingPolicy sp = StripingPolicy.newBuilder().setType(defaultStripingPolicyType)
                .setStripeSize(defaultStripeSize).setWidth(defaultStripeWidth).build();

        org.xtreemfs.pbrpc.generatedinterfaces.MRC.Volume volume = org.xtreemfs.pbrpc.generatedinterfaces.MRC.Volume
                .newBuilder().setName(volumeName).setMode(mode).setOwnerUserId(ownerUsername)
                .setOwnerGroupId(ownerGroupname).setAccessControlPolicy(accessPolicyType)
                .setDefaultStripingPolicy(sp).addAllAttrs(volumeAttributes).setId("").build();

        RPCCaller.<org.xtreemfs.pbrpc.generatedinterfaces.MRC.Volume, emptyResponse> syncCall(SERVICES.MRC,
                userCredentials, auth, options, this, mrcAddresses, true, volume,
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
    public void deleteVolume(String mrcAddress, Auth auth, UserCredentials userCredentials, String volumeName)
            throws IOException {
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
            String volumeName) throws IOException {
        UUIDIterator iteratorWithAddresses = new UUIDIterator();
        iteratorWithAddresses.addUUIDs(mrcAddresses);

        deleteVolume(iteratorWithAddresses, auth, userCredentials, volumeName);
    }

    private void deleteVolume(UUIDIterator mrcAddresses, Auth auth, UserCredentials userCredentials,
            String volumeName) throws IOException {
        final MRCServiceClient mrcServiceClient = new MRCServiceClient(networkClient, null);

        RPCCaller.<String, emptyResponse> syncCall(SERVICES.MRC, userCredentials, auth, options, this,
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
    public Volumes listVolumes(String mrcAddress) throws IOException {

        final MRCServiceClient mrcServiceClient = new MRCServiceClient(networkClient, null);

        UUIDIterator iteratorWithAddresses = new UUIDIterator();
        iteratorWithAddresses.addUUID(mrcAddress);

        // use bogus user credentials
        UserCredentials userCredentials = UserCredentials.newBuilder().setUsername("xtreemfs").build();

        Volumes volumes = RPCCaller.<emptyRequest, Volumes> syncCall(SERVICES.MRC, userCredentials,
                authBogus, options, this, iteratorWithAddresses, true, emptyRequest.getDefaultInstance(),
                new CallGenerator<emptyRequest, Volumes>() {
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
     * @see org.xtreemfs.common.libxtreemfs.Client#getUUIDResolver()
     */
    @Override
    public UUIDResolver getUUIDResolver() {
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xtreemfs.common.libxtreemfs.UUIDResolver#uuidToAddress(java.lang.String, java.lang.String)
     */
    @Override
    public String uuidToAddress(String uuid) {

        // The uuid must never be empty.
        assert (!uuid.isEmpty());

        String address = "";

        ServiceUUID serviceUuid = new ServiceUUID(uuid, uuidResolver);
        try {
            serviceUuid.resolve();
            address = serviceUuid.getAddress().getHostName() + ":" + serviceUuid.getAddress().getPort();
        } catch (UnknownUUIDException e) {
            // TODO: Throw new Exception with same name like in CPP implementation.
        }

        return address;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xtreemfs.common.libxtreemfs.UUIDResolver#volumeNameToMRCUUID(java.lang.String,
     * java.lang.String)
     */
    @Override
    public String volumeNameToMRCUUID(String volumeName) throws VolumeNotFoundException {
        assert (!volumeName.isEmpty());

        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this, "Searching MRC for volume %s",
                    volumeName);
        }

        // Check if there is an '@' in the volume. Everything behind the '@' is the snapshot,
        // cut if off.
        String parsedVolumeName = parseVolumeName(volumeName);

        ServiceSet sSet = RPCCaller.<String, ServiceSet> syncCall(SERVICES.DIR, dirServiceUserCredentials,
                dirServiceAuth, options, this, dirServiceAddresses, true, parsedVolumeName,
                new CallGenerator<String, ServiceSet>() {

                    @Override
                    public RPCResponse<ServiceSet> executeCall(InetSocketAddress server, Auth authHeader,
                            UserCredentials userCreds, String input) throws IOException {
                        return dirServiceClient.xtreemfs_service_get_by_name(server, authHeader, userCreds,
                                input);
                    }
                });

        // check if there is an service which is a VOLUME and then filter the MRC of this volume
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
                // don't check other services if there are any left when an mrcUuid is already determined.
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
     * @see org.xtreemfs.common.libxtreemfs.UUIDResolver#volumeNameToMRCUUID(java.lang.String,
     * org.xtreemfs.common.libxtreemfs.UUIDIterator)
     */
    @Override
    public void volumeNameToMRCUUID(String volumeName, UUIDIterator uuidIterator)
            throws VolumeNotFoundException {

        assert (uuidIterator != null);
        assert (!volumeName.isEmpty());

        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this, "Searching MRC for volume %s",
                    volumeName);
        }

        // Check if there is a @ in the volume_name.
        // Everything behind the @ has to be removed as it identifies the snapshot.
        String parsedVolumeName = parseVolumeName(volumeName);

        ServiceSet sSet = RPCCaller.<String, ServiceSet> syncCall(SERVICES.DIR, dirServiceUserCredentials,
                dirServiceAuth, options, this, dirServiceAddresses, true, parsedVolumeName,
                new CallGenerator<String, ServiceSet>() {
                    @Override
                    public RPCResponse<ServiceSet> executeCall(InetSocketAddress server, Auth authHeader,
                            UserCredentials userCreds, String input) throws IOException {
                        return dirServiceClient.xtreemfs_service_get_by_name(server, authHeader, userCreds,
                                input);
                    }
                });

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
}
