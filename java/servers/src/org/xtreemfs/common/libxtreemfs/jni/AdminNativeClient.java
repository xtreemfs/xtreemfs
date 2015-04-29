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
import java.util.Set;

import org.xtreemfs.common.libxtreemfs.AdminClient;
import org.xtreemfs.common.libxtreemfs.AdminVolume;
import org.xtreemfs.common.libxtreemfs.ClientFactory;
import org.xtreemfs.common.libxtreemfs.Options;
import org.xtreemfs.common.libxtreemfs.exceptions.AddressToUUIDNotFoundException;
import org.xtreemfs.common.libxtreemfs.exceptions.PosixErrorException;
import org.xtreemfs.common.libxtreemfs.exceptions.VolumeNotFoundException;
import org.xtreemfs.common.libxtreemfs.swig.ClientProxy;
import org.xtreemfs.common.libxtreemfs.swig.OptionsProxy;
import org.xtreemfs.common.libxtreemfs.swig.SSLOptionsProxy;
import org.xtreemfs.common.libxtreemfs.swig.VolumeProxy;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceStatus;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.AccessControlPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Volumes;

public class AdminNativeClient extends NativeClient implements AdminClient {

    private final AdminClient adminClient;

    public AdminNativeClient(ClientProxy client, AdminClient adminClient) {
        super(client);
        this.adminClient = adminClient;
    }

    public static AdminNativeClient createClient(String[] dirServiceAddressesArray, UserCredentials userCredentials,
            SSLOptions sslOptions, Options options) {

        ClientProxy clientProxy = NativeHelper.createClientProxy(dirServiceAddressesArray, userCredentials, sslOptions,
                options);
        AdminClient adminClient = ClientFactory.createAdminClient(dirServiceAddressesArray, userCredentials,
                sslOptions, options);
        AdminNativeClient client = new AdminNativeClient(clientProxy, adminClient);
        return client;
    }

    @Override
    public void start() throws Exception {
        super.start();
        adminClient.start();
    };

    @Override
    public void shutdown() {
        super.shutdown();
        adminClient.shutdown();
    }

    @Override
    public AdminNativeVolume openVolume(String volumeName, SSLOptions sslOptions, Options options)
            throws AddressToUUIDNotFoundException, VolumeNotFoundException, IOException {
        OptionsProxy optionsProxy = NativeHelper.migrateOptions(options);
        SSLOptionsProxy sslOptionsProxy = null;
        if (sslOptions != null) {
            // TODO (jdillmann): Merge from sslOptions
            throw new RuntimeException("SSLOptions are not supported yet.");
        }
        VolumeProxy volume = proxy.openVolumeProxy(volumeName, sslOptionsProxy, optionsProxy);
        AdminVolume adminVolume = adminClient.openVolume(volumeName, sslOptions, options);
        AdminNativeVolume nativeVolume = new AdminNativeVolume(volume, adminVolume, volumeName);
        return nativeVolume;
    }

    @Override
    public void startCleanUp(String osdUUID, String password, boolean remove, boolean deleteVolumes, boolean restore,
            boolean removeMetadata, int metaDataTimeoutS) throws IOException {
        adminClient.startCleanUp(osdUUID, password, remove, deleteVolumes, restore, removeMetadata, metaDataTimeoutS);
    }

    @Override
    public void startVersionCleanUp(String osdUUID, String password) throws IOException {
        adminClient.startVersionCleanUp(osdUUID, password);
    }

    @Override
    public void stopCleanUp(String osdUUID, String password) throws IOException {
        adminClient.stopCleanUp(osdUUID, password);
    }

    @Override
    public boolean isRunningCleanUp(String osdUUID, String password) throws IOException {
        return adminClient.isRunningCleanUp(osdUUID, password);
    }

    @Override
    public String getCleanUpState(String osdUUID, String password) throws IOException {
        return adminClient.getCleanUpState(osdUUID, password);
    }

    @Override
    public List<String> getCleanUpResult(String osdUUID, String password) throws IOException {
        return adminClient.getCleanUpResult(osdUUID, password);
    }

    @Override
    public ServiceSet getServiceByType(ServiceType serviceType) throws IOException {
        return adminClient.getServiceByType(serviceType);
    }

    @Override
    public Service getServiceByUUID(String uuid) throws IOException {
        return adminClient.getServiceByUUID(uuid);
    }

    @Override
    public void setOSDServiceStatus(String osdUUID, ServiceStatus serviceStatus) throws IOException {
        adminClient.setOSDServiceStatus(osdUUID, serviceStatus);
    }

    @Override
    public ServiceStatus getOSDServiceStatus(String osdUUID) throws IOException {
        return adminClient.getOSDServiceStatus(osdUUID);
    }

    @Override
    public Set<String> getRemovedOsds() throws IOException {
        return adminClient.getRemovedOsds();
    }

    @Override
    public void createVolume(Auth auth, UserCredentials userCredentials, String volumeName, int mode,
            String ownerUsername, String ownerGroupname, AccessControlPolicyType accessPolicyType,
            StripingPolicyType defaultStripingPolicyType, int defaultStripeSize, int defaultStripeWidth,
            List<KeyValuePair> volumeAttributes) throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException {
        adminClient.createVolume(auth, userCredentials, volumeName, mode, ownerUsername, ownerGroupname,
                accessPolicyType, defaultStripingPolicyType, defaultStripeSize, defaultStripeWidth, volumeAttributes);
    }

    @Override
    public void deleteVolume(Auth auth, UserCredentials userCredentials, String volumeName) throws IOException,
            PosixErrorException, AddressToUUIDNotFoundException {
        adminClient.deleteVolume(auth, userCredentials, volumeName);
    }

    @Override
    public Volumes listVolumes() throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        return adminClient.listVolumes();
    }

    @Override
    public String[] listVolumeNames() throws IOException {
        return adminClient.listVolumeNames();
    }

    @Override
    public Map<String, Service> listServers() throws IOException, PosixErrorException {
        return adminClient.listServers();
    }

    @Override
    public Map<String, Service> listOSDsAndAttributes() throws IOException, PosixErrorException {
        return adminClient.listOSDsAndAttributes();
    }
}
