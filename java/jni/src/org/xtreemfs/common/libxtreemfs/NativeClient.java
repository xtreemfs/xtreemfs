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

public class NativeClient extends ClientProxy implements Client {

    private final static Auth authBogus;
    static {
        authBogus = Auth.newBuilder().setAuthType(AuthType.AUTH_NONE).build();
    }

    protected NativeClient(long cPtr, boolean cMemoryOwn) {
        super(cPtr, cMemoryOwn);
    }

    protected NativeClient(ClientProxy c) {
        this(ClientProxy.getCPtr(c), false);
    }

    public static NativeClient createClient(String[] dirServiceAddressesArray, UserCredentials userCredentials,
            SSLOptions sslOptions, Options options) {
        ClientProxy clientProxy = NativeHelper.createClientProxy(dirServiceAddressesArray, userCredentials, sslOptions,
                options);
        return new NativeClient(clientProxy);
    }

    @Override
    public void start(boolean startThreadsAsDaemons) throws Exception {
        if (startThreadsAsDaemons) {
            throw new RuntimeException("starting threads as daemons is not supported.");
        }
        start();
    }

    @Override
    public NativeVolume openVolume(String volumeName, org.xtreemfs.foundation.SSLOptions sslOptions,
            org.xtreemfs.common.libxtreemfs.Options options)
            throws AddressToUUIDNotFoundException, VolumeNotFoundException, IOException {
        OptionsProxy optionsProxy = NativeHelper.migrateOptions(options);
        SSLOptionsProxy sslOptionsProxy = null;
        if (sslOptions != null) {
            // TODO (jdillmann): Merge from sslOptions
            throw new RuntimeException("SSLOptions are not supported yet.");
        }
        VolumeProxy volume = openVolumeProxy(volumeName, sslOptionsProxy, optionsProxy);
        NativeVolume volumeNative = new NativeVolume(volume, volumeName);

        return volumeNative;
    }
    
    @Override
    public void createVolume(String mrcAddress, Auth auth, UserCredentials userCredentials, String volumeName)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        createVolume(new ServiceAddresses(mrcAddress), auth, userCredentials, volumeName);
    }

    @Override
    public void createVolume(List<String> mrcAddresses, Auth auth, UserCredentials userCredentials, String volumeName)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        createVolume(new ServiceAddresses(VectorString.from(mrcAddresses)), auth, userCredentials, volumeName);
    }

    @Override
    public void createVolume(Auth auth, UserCredentials userCredentials, String volumeName, int mode,
            String ownerUsername, String ownerGroupname, AccessControlPolicyType accessPolicyType,
            StripingPolicyType defaultStripingPolicyType, int defaultStripeSize, int defaultStripeWidth,
            List<KeyValuePair> volumeAttributes) throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException {
        // TODO (jdillmann): Can't access DIR directly to call xtreemfs_service_get_by_name

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

        createVolume(new ServiceAddresses(mrcAddress), auth, userCredentials, volumeName, mode, ownerUsername,
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

        createVolume(new ServiceAddresses(VectorString.from(mrcAddresses)), auth, userCredentials, volumeName, mode,
                ownerUsername, ownerGroupname, accessPolicyType, quota, defaultStripingPolicyType, defaultStripeSize,
                defaultStripeWidth, vs);

        vs.delete();
    }

    @Override
    public void deleteVolume(String mrcAddress, Auth auth, UserCredentials userCredentials, String volumeName)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        deleteVolume(new ServiceAddresses(mrcAddress), auth, userCredentials, volumeName);
    }

    @Override
    public void deleteVolume(List<String> mrcAddresses, Auth auth, UserCredentials userCredentials, String volumeName)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        deleteVolume(new ServiceAddresses(VectorString.from(mrcAddresses)), auth, userCredentials, volumeName);
    }

    @Override
    public void deleteVolume(Auth auth, UserCredentials userCredentials, String volumeName) throws IOException,
            PosixErrorException, AddressToUUIDNotFoundException {
        // TODO (jdillmann): Can't access DIR directly to call xtreemfs_service_get_by_name
    }

    @Override
    public Volumes listVolumes(String mrcAddress) throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException {
        return listVolumes(new ServiceAddresses(mrcAddress), authBogus);
    }

    @Override
    public Volumes listVolumes() throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        // TODO (jdillmann): Can't access DIR directly to call xtreemfs_service_get_by_name
        return null;
    }

    @Override
    public String[] listVolumeNames() throws IOException {
        // TODO (jdillmann): Can't access DIR directly to call xtreemfs_service_get_by_name
        return null;
    }

    @Override
    public Volumes listVolumes(List<String> mrcAddresses) throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException {
        return listVolumes(new ServiceAddresses(VectorString.from(mrcAddresses)), authBogus);
    }

    @Override
    public Map<String, Service> listServers() throws IOException, PosixErrorException {
        // TODO (jdillmann): Can't access DIR directly to call xtreemfs_service_get_by_name
        return null;
    }

    @Override
    public Map<String, Service> listOSDsAndAttributes() throws IOException, PosixErrorException {
        // TODO (jdillmann): Can't access DIR directly to call xtreemfs_service_get_by_name
        return null;
    }
}
