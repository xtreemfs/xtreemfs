/*
 * Copyright (c) 2015 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs.jni;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.xtreemfs.common.libxtreemfs.Client;
import org.xtreemfs.common.libxtreemfs.Options;
import org.xtreemfs.common.libxtreemfs.exceptions.AddressToUUIDNotFoundException;
import org.xtreemfs.common.libxtreemfs.exceptions.PosixErrorException;
import org.xtreemfs.common.libxtreemfs.exceptions.VolumeNotFoundException;
import org.xtreemfs.common.libxtreemfs.jni.generated.ClientProxy;
import org.xtreemfs.common.libxtreemfs.jni.generated.OptionsProxy;
import org.xtreemfs.common.libxtreemfs.jni.generated.SSLOptionsProxy;
import org.xtreemfs.common.libxtreemfs.jni.generated.ServiceAddresses;
import org.xtreemfs.common.libxtreemfs.jni.generated.StringMap;
import org.xtreemfs.common.libxtreemfs.jni.generated.StringVector;
import org.xtreemfs.common.libxtreemfs.jni.generated.VolumeProxy;
import org.xtreemfs.common.libxtreemfs.jni.generated.xtreemfs_jni;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.AuthType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.AccessControlPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Volumes;

public class NativeClient implements Client {
    
    // Load the native library.
    static {
        NativeHelper.loadLibrary("jni-xtreemfs");
        if (Logging.getLevel() >= 0) {
            xtreemfs_jni.initialize_logger(Logging.getLevel());
        }
    }

    protected final ClientProxy proxy;

    protected final static Auth dirServiceAuth;
    static {
        // TODO: change this when the DIR service supports real auth.
        // @see ClientImplementation.dirServiceAuth
        dirServiceAuth = Auth.newBuilder().setAuthType(AuthType.AUTH_NONE).build();
    }

    public NativeClient(ClientProxy client) {
        proxy = client;
    }

    public static NativeClient createClient(String[] dirServiceAddressesArray, UserCredentials userCredentials,
            SSLOptions sslOptions, Options options) {

        ClientProxy clientProxy = NativeHelper.createClientProxy(dirServiceAddressesArray, userCredentials, sslOptions,
                options);
        NativeClient client = new NativeClient(clientProxy);
        return client;
    }

    @Override
    public void start() throws Exception {
        proxy.start();
    }

    @Override
    public void start(boolean startThreadsAsDaemons) throws Exception {
        if (startThreadsAsDaemons) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this,
                        "Starting the native client with daemon threads is not supported");
            }
        }

        start();
    }

    @Override
    public void shutdown() {
        proxy.shutdown();
    }

    @Override
    public NativeVolume openVolume(String volumeName, SSLOptions sslOptions, Options options)
            throws AddressToUUIDNotFoundException, VolumeNotFoundException, IOException {
        OptionsProxy optionsProxy = NativeHelper.migrateOptions(options);
        SSLOptionsProxy sslOptionsProxy = NativeHelper.migrateSSLOptions(sslOptions);

        VolumeProxy volume = proxy.openVolumeProxy(volumeName, sslOptionsProxy, optionsProxy);
        NativeVolume nativeVolume = new NativeVolume(this, volume, volumeName);

        return nativeVolume;
    }

    @Override
    public void createVolume(String mrcAddress, Auth auth, UserCredentials userCredentials, String volumeName)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        proxy.createVolume(new ServiceAddresses(mrcAddress), auth, userCredentials, volumeName);
    }

    @Override
    public void createVolume(List<String> mrcAddresses, Auth auth, UserCredentials userCredentials, String volumeName)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        proxy.createVolume(new ServiceAddresses(StringVector.from(mrcAddresses)), auth, userCredentials, volumeName);
    }

    @Override
    public void createVolume(Auth auth, UserCredentials userCredentials, String volumeName, int mode,
            String ownerUsername, String ownerGroupname, AccessControlPolicyType accessPolicyType,
            StripingPolicyType defaultStripingPolicyType, int defaultStripeSize, int defaultStripeWidth,
            List<KeyValuePair> volumeAttributes) throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException {
        final int quota = 0;

        StringMap volumeAttributesMap = NativeHelper.keyValueListToMap(volumeAttributes);
        proxy.createVolume(auth, userCredentials, volumeName, mode, ownerUsername, ownerGroupname, accessPolicyType,
                quota, defaultStripingPolicyType, defaultStripeSize, defaultStripeWidth, volumeAttributesMap);

        volumeAttributesMap.delete();
    }

    @Override
    public void createVolume(String mrcAddress, Auth auth, UserCredentials userCredentials, String volumeName,
            int mode, String ownerUsername, String ownerGroupname, AccessControlPolicyType accessPolicyType,
            StripingPolicyType defaultStripingPolicyType, int defaultStripeSize, int defaultStripeWidth,
            List<KeyValuePair> volumeAttributes) throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException {
        final int quota = 0;

        StringMap volumeAttributesMap = NativeHelper.keyValueListToMap(volumeAttributes);
        proxy.createVolume(new ServiceAddresses(mrcAddress), auth, userCredentials, volumeName, mode, ownerUsername,
                ownerGroupname, accessPolicyType, quota, defaultStripingPolicyType, defaultStripeSize,
                defaultStripeWidth, volumeAttributesMap);

        volumeAttributesMap.delete();
    }

    @Override
    public void createVolume(List<String> mrcAddresses, Auth auth, UserCredentials userCredentials, String volumeName,
            int mode, String ownerUsername, String ownerGroupname, AccessControlPolicyType accessPolicyType,
            StripingPolicyType defaultStripingPolicyType, int defaultStripeSize, int defaultStripeWidth,
            List<KeyValuePair> volumeAttributes) throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException {
        final int quota = 0;

        StringMap volumeAttributesMap = NativeHelper.keyValueListToMap(volumeAttributes);
        proxy.createVolume(new ServiceAddresses(StringVector.from(mrcAddresses)), auth, userCredentials, volumeName,
                mode, ownerUsername, ownerGroupname, accessPolicyType, quota, defaultStripingPolicyType,
                defaultStripeSize, defaultStripeWidth, volumeAttributesMap);

        volumeAttributesMap.delete();
    }

    @Override
    public void deleteVolume(String mrcAddress, Auth auth, UserCredentials userCredentials, String volumeName)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        proxy.deleteVolume(new ServiceAddresses(mrcAddress), auth, userCredentials, volumeName);
    }

    @Override
    public void deleteVolume(List<String> mrcAddresses, Auth auth, UserCredentials userCredentials, String volumeName)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        proxy.deleteVolume(new ServiceAddresses(StringVector.from(mrcAddresses)), auth, userCredentials, volumeName);
    }

    @Override
    public void deleteVolume(Auth auth, UserCredentials userCredentials, String volumeName) throws IOException,
            PosixErrorException, AddressToUUIDNotFoundException {
        proxy.deleteVolume(auth, userCredentials, volumeName);
    }

    @Override
    public Volumes listVolumes(String mrcAddress) throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException {
        return proxy.listVolumes(new ServiceAddresses(mrcAddress), dirServiceAuth);
    }

    @Override
    public String[] listVolumeNames() throws IOException {
        StringVector result = proxy.listVolumeNames();
        String[] out = result.toArray();
        result.delete();
        return out;
    }

    @Override
    public Volumes listVolumes(List<String> mrcAddresses) throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException {
        return proxy.listVolumes(new ServiceAddresses(StringVector.from(mrcAddresses)), dirServiceAuth);
    }

    NativeUUIDResolver getUUIDResolver() {
        return new NativeUUIDResolver(proxy.getUUIDResolver());
    }

    @Override
    public Volumes listVolumes() throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        throw new RuntimeException("Not implemented in the C++ library.");
    }

    @Override
    public Map<String, Service> listServers() throws IOException, PosixErrorException {
        throw new RuntimeException("Not implemented in the C++ library.");
    }

    @Override
    public Map<String, Service> listOSDsAndAttributes() throws IOException, PosixErrorException {
        throw new RuntimeException("Not implemented in the C++ library.");
    }
}
