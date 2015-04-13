package org.xtreemfs.common.libxtreemfs;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.xtreemfs.common.libxtreemfs.exceptions.AddressToUUIDNotFoundException;
import org.xtreemfs.common.libxtreemfs.exceptions.PosixErrorException;
import org.xtreemfs.common.libxtreemfs.exceptions.VolumeNotFoundException;
import org.xtreemfs.common.libxtreemfs.swig.ClientProxy;
import org.xtreemfs.common.libxtreemfs.swig.OptionsProxy;
import org.xtreemfs.common.libxtreemfs.swig.SSLOptionsProxy;
import org.xtreemfs.common.libxtreemfs.swig.ServiceAddresses;
import org.xtreemfs.common.libxtreemfs.swig.VectorString;
import org.xtreemfs.common.libxtreemfs.swig.VolumeProxy;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.AuthType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.AccessControlPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Volumes;

public class NativeClient2 implements Client {
    
    // Load the native library.
    static { System.loadLibrary("xtreemfs_jni"); }

    protected final ClientProxy proxy;

    protected final static Auth authBogus;
    static {
        authBogus = Auth.newBuilder().setAuthType(AuthType.AUTH_NONE).build();
    }

    public NativeClient2(ClientProxy client) {
        proxy = client;
    }

    public static NativeClient2 createClient(String[] dirServiceAddressesArray, UserCredentials userCredentials,
            SSLOptions sslOptions, Options options) {

        ClientProxy clientProxy = NativeHelper.createClientProxy(dirServiceAddressesArray, userCredentials, sslOptions,
                options);
        NativeClient2 client = new NativeClient2(clientProxy);
        return client;
    }

    @Override
    public void start() throws Exception {
        proxy.start();
    }

    @Override
    public void start(boolean startThreadsAsDaemons) throws Exception {
        if (startThreadsAsDaemons) {
            throw new RuntimeException("starting threads as daemons is not supported.");
        }
        start();
    }

    @Override
    public void shutdown() {
        proxy.shutdown();
    }

    @Override
    public NativeVolume2 openVolume(String volumeName, SSLOptions sslOptions, Options options)
            throws AddressToUUIDNotFoundException, VolumeNotFoundException, IOException {
        OptionsProxy optionsProxy = NativeHelper.migrateOptions(options);
        SSLOptionsProxy sslOptionsProxy = null;
        if (sslOptions != null) {
            // TODO (jdillmann): Merge from sslOptions
            throw new RuntimeException("SSLOptions are not supported yet.");
        }
        VolumeProxy volume = proxy.openVolumeProxy(volumeName, sslOptionsProxy, optionsProxy);
        NativeVolume2 nativeVolume = new NativeVolume2(volume, volumeName);

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
        proxy.createVolume(new ServiceAddresses(VectorString.from(mrcAddresses)), auth, userCredentials, volumeName);
    }

    @Override
    public void createVolume(Auth auth, UserCredentials userCredentials, String volumeName, int mode,
            String ownerUsername, String ownerGroupname, AccessControlPolicyType accessPolicyType,
            StripingPolicyType defaultStripingPolicyType, int defaultStripeSize, int defaultStripeWidth,
            List<KeyValuePair> volumeAttributes) throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException {
        // TODO (jdillmann): Can't access DIR directly to call xtreemfs_service_get_by_name
        throw new RuntimeException("Not implemented yet.");
    }

    @Override
    public void createVolume(String mrcAddress, Auth auth, UserCredentials userCredentials, String volumeName,
            int mode, String ownerUsername, String ownerGroupname, AccessControlPolicyType accessPolicyType,
            StripingPolicyType defaultStripingPolicyType, int defaultStripeSize, int defaultStripeWidth,
            List<KeyValuePair> volumeAttributes) throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException {
        final int quota = 0;

        VectorString vs = new VectorString(volumeAttributes.size());
        for (KeyValuePair kv : volumeAttributes) {
            String s = new String(kv.toByteArray());
            vs.add(s);
        }

        proxy.createVolume(new ServiceAddresses(mrcAddress), auth, userCredentials, volumeName, mode, ownerUsername,
                ownerGroupname, accessPolicyType, quota, defaultStripingPolicyType, defaultStripeSize,
                defaultStripeWidth, vs);

        vs.delete();
    }

    @Override
    public void createVolume(List<String> mrcAddresses, Auth auth, UserCredentials userCredentials, String volumeName,
            int mode, String ownerUsername, String ownerGroupname, AccessControlPolicyType accessPolicyType,
            StripingPolicyType defaultStripingPolicyType, int defaultStripeSize, int defaultStripeWidth,
            List<KeyValuePair> volumeAttributes) throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException {
        final int quota = 0;

        VectorString vs = new VectorString(volumeAttributes.size());
        for (KeyValuePair kv : volumeAttributes) {
            String s = new String(kv.toByteArray());
            vs.add(s);
        }

        proxy.createVolume(new ServiceAddresses(VectorString.from(mrcAddresses)), auth, userCredentials, volumeName,
                mode, ownerUsername, ownerGroupname, accessPolicyType, quota, defaultStripingPolicyType,
                defaultStripeSize, defaultStripeWidth, vs);

        vs.delete();
    }

    @Override
    public void deleteVolume(String mrcAddress, Auth auth, UserCredentials userCredentials, String volumeName)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        proxy.deleteVolume(new ServiceAddresses(mrcAddress), auth, userCredentials, volumeName);
    }

    @Override
    public void deleteVolume(List<String> mrcAddresses, Auth auth, UserCredentials userCredentials, String volumeName)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        proxy.deleteVolume(new ServiceAddresses(VectorString.from(mrcAddresses)), auth, userCredentials, volumeName);
    }

    @Override
    public void deleteVolume(Auth auth, UserCredentials userCredentials, String volumeName) throws IOException,
            PosixErrorException, AddressToUUIDNotFoundException {
        // TODO (jdillmann): Can't access DIR directly to call xtreemfs_service_get_by_name
        throw new RuntimeException("Not implemented yet.");
    }

    @Override
    public Volumes listVolumes(String mrcAddress) throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException {
        return proxy.listVolumes(new ServiceAddresses(mrcAddress), authBogus);
    }

    @Override
    public Volumes listVolumes() throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        // TODO (jdillmann): Can't access DIR directly to call xtreemfs_service_get_by_name
        throw new RuntimeException("Not implemented yet.");
    }

    @Override
    public String[] listVolumeNames() throws IOException {
        // TODO (jdillmann): Can't access DIR directly to call xtreemfs_service_get_by_name
        throw new RuntimeException("Not implemented yet.");
    }

    @Override
    public Volumes listVolumes(List<String> mrcAddresses) throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException {
        return proxy.listVolumes(new ServiceAddresses(VectorString.from(mrcAddresses)), authBogus);
    }

    @Override
    public Map<String, Service> listServers() throws IOException, PosixErrorException {
        // TODO (jdillmann): Can't access DIR directly to call xtreemfs_service_get_by_name
        throw new RuntimeException("Not implemented yet.");
    }

    @Override
    public Map<String, Service> listOSDsAndAttributes() throws IOException, PosixErrorException {
        // TODO (jdillmann): Can't access DIR directly to call xtreemfs_service_get_by_name
        throw new RuntimeException("Not implemented yet.");
    }
}
