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
import org.xtreemfs.common.libxtreemfs.ClientFactory.ClientType;
import org.xtreemfs.common.libxtreemfs.Options;
import org.xtreemfs.common.libxtreemfs.exceptions.AddressToUUIDNotFoundException;
import org.xtreemfs.common.libxtreemfs.exceptions.PosixErrorException;
import org.xtreemfs.common.libxtreemfs.exceptions.VolumeNotFoundException;
import org.xtreemfs.common.libxtreemfs.jni.generated.ClientProxy;
import org.xtreemfs.common.libxtreemfs.jni.generated.OptionsProxy;
import org.xtreemfs.common.libxtreemfs.jni.generated.SSLOptionsProxy;
import org.xtreemfs.common.libxtreemfs.jni.generated.VolumeProxy;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceStatus;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceType;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Volumes;

public class NativeAdminClient extends NativeClient implements AdminClient {

    private final AdminClient adminClient;

    public NativeAdminClient(ClientProxy client, AdminClient adminClient) {
        super(client);
        this.adminClient = adminClient;
    }

    public static NativeAdminClient createClient(String[] dirServiceAddressesArray, UserCredentials userCredentials,
            SSLOptions sslOptions, Options options) {

        ClientProxy clientProxy = NativeHelper.createClientProxy(dirServiceAddressesArray, userCredentials, sslOptions,
                options);
        AdminClient adminClient = ClientFactory.createAdminClient(ClientType.JAVA, dirServiceAddressesArray,
                userCredentials, sslOptions, options);
        NativeAdminClient client = new NativeAdminClient(clientProxy, adminClient);
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
    public NativeAdminVolume openVolume(String volumeName, SSLOptions sslOptions, Options options)
            throws AddressToUUIDNotFoundException, VolumeNotFoundException, IOException {
        OptionsProxy optionsProxy = NativeHelper.migrateOptions(options);
        SSLOptionsProxy sslOptionsProxy = NativeHelper.migrateSSLOptions(sslOptions);

        VolumeProxy volume = proxy.openVolumeProxy(volumeName, sslOptionsProxy, optionsProxy);
        AdminVolume adminVolume = adminClient.openVolume(volumeName, sslOptions, options);
        NativeAdminVolume nativeVolume = new NativeAdminVolume(this, volume, adminVolume, volumeName);
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
    public Volumes listVolumes() throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        return adminClient.listVolumes();
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
