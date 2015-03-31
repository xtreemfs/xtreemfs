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
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.AuthType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.AccessControlPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Volumes;

public class ClientNative extends ClientProxy implements Client {

    private final static Auth authBogus;
    static {
        authBogus = Auth.newBuilder().setAuthType(AuthType.AUTH_NONE).build();
    }

    protected ClientNative(long cPtr, boolean cMemoryOwn) {
        super(cPtr, cMemoryOwn);
    }

    public ClientNative(ClientProxy c) {
        this(ClientProxy.getCPtr(c), false);
    }

    @Override
    public void start(boolean startThreadsAsDaemons) throws Exception {
        if (startThreadsAsDaemons) {
            throw new RuntimeException("starting threads as daemons is not supported.");
        }
        start();
    }

    @Override
    public VolumeNative openVolume(String volumeName, org.xtreemfs.foundation.SSLOptions sslOptions,
            org.xtreemfs.common.libxtreemfs.Options options)
            throws AddressToUUIDNotFoundException, VolumeNotFoundException, IOException {
        // TODO: JNIOptions, SSLOptions
        OptionsProxy optionsNative = new OptionsProxy();
        SSLOptionsProxy sslOptionsNative = null;
        VolumeProxy volume = openVolumeProxy(volumeName, sslOptionsNative, optionsNative);
        VolumeNative volumeNative = new VolumeNative(volume, volumeName);

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
