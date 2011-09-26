/*
 * Copyright (c) 2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xtreemfs.common.libxtreemfs.exceptions.VolumeNotFoundException;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.AccessControlPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Volumes;

/**
 * Available client implementations which are allowed by CreateClient().
 * 
 * <br>
 * Sep 2, 2011
 */
enum ClientImplementationType {
    kDefaultClient
}

/**
 * Provides methods to open, close, create, delete and list volumes and to instantiate a new client object, to
 * start and shutdown a Client object.
 * 
 * <br>
 * Sep 2, 2011
 */
public abstract class Client implements UUIDResolver {

    /**
     * Returns an instance of the default Client implementation.
     * 
     * @param dir_service_address
     *            Address of the DIR service (Format: ip-addr:port, e.g. localhost:32638)
     * @param user_credentials
     *            Name and Groups of the user.
     * @param ssl_options
     *            NULL if no SSL is used.
     * @param options
     *            Has to contain loglevel string and logfile path.
     */
    public static Client createClient(String dirServiceAddress, UserCredentials userCredentials,
            SSLOptions sslOptions, Options options) {
        return createClient(dirServiceAddress, userCredentials, sslOptions, options,
                ClientImplementationType.kDefaultClient);
    }

    /** Returns an instance of the chosen Client implementation. */
    public static Client createClient(String dirServiceAddress, UserCredentials userCredentials,
            SSLOptions sslOptions, Options options, ClientImplementationType type) {

        switch (type) {
        case kDefaultClient:
            return new ClientImplementation(dirServiceAddress, userCredentials, sslOptions, options);
        default:
            return null;
        }
    }

    /**
     * Need to be called before client can be used. Initializes required stuff.
     */
    public abstract void start() throws Exception;

    public abstract void shutdown();

    /**
     * Open a volume and use the returned class to access it.
     * 
     * @remark Ownership is NOT transferred to the caller. Instead Volume->Close() has to be called to destroy
     *         the object.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws UnknownAddressSchemeException
     * @throws VolumeNotFoundException
     */
    public abstract Volume openVolume(String volumeName, SSLOptions sslOptions, Options options)
            throws Exception;

    /**
     * Creates a volume on the MRC at mrc_address using certain default values ( POSIX access policy type,
     * striping size = 128k and width = 1 (i.e. no striping), mode = 777 and owner username and groupname
     * retrieved from the user_credentials.
     * 
     * @param mrcAddress
     *            String of the form "hostname:port".
     * @param auth
     *            Authentication data, e.g. of type AUTH_PASSWORD.
     * @param userCredentials
     *            Username and groups of the user who executes CreateVolume(). Not checked so far?
     * @param volumeName
     *            Name of the new volume.
     * 
     * @throws IOException
     * @throws PosixErrorException
     */
    public void createVolume(String mrcAddress, Auth auth, UserCredentials userCredentials, String volumeName)
            throws IOException {

        List<KeyValuePair> volumeAttributes = new ArrayList<KeyValuePair>();
        createVolume(mrcAddress, auth, userCredentials, volumeName, 511, "", "",
                AccessControlPolicyType.ACCESS_CONTROL_POLICY_POSIX,
                StripingPolicyType.STRIPING_POLICY_RAID0, 128, 1, volumeAttributes);
    };

    // TODO(pseiferth): Also provide a method which accepts a list of MRC addresses
    // or an UUID Iterator object which contains all addresses.
    /**
     * Creates a volume on the MRC at mrc_address using certain default values ( POSIX access policy type,
     * striping size = 128k and width = 1 (i.e. no striping), mode = 777 and owner username and groupname
     * retrieved from the user_credentials.
     * 
     * @param mrcAddress
     *            String of the form "hostname:port".
     * @param auth
     *            Authentication data, e.g. of type AUTH_PASSWORD.
     * @param userCredentials
     *            Username and groups of the user who executes CreateVolume(). Not checked so far?
     * @param volumeName
     *            Name of the new volume.
     * 
     * @throws IOException
     * @throws PosixErrorException
     */
    public void createVolume(List<String> mrcAddresses, Auth auth, UserCredentials userCredentials,
            String volumeName) throws IOException {
        List<KeyValuePair> volumeAttributes = new ArrayList<KeyValuePair>();
        createVolume(mrcAddresses, auth, userCredentials, volumeName, 511, "", "",
                AccessControlPolicyType.ACCESS_CONTROL_POLICY_POSIX,
                StripingPolicyType.STRIPING_POLICY_RAID0, 128, 1, volumeAttributes);
    }

    /**
     * Creates a volume on the MRC at mrc_address.
     * 
     * @param mrcAddress
     *            String of the form "hostname:port".
     * @param auth
     *            Authentication data, e.g. of type AUTH_PASSWORD.
     * @param userCredentials
     *            Username and groups of the user who executes CreateVolume().
     * @param volumeName
     *            Name of the new volume.
     * @param mode
     *            Mode of the volume's root directory (in octal representation (e.g. 511), not decimal (777)).
     * @param ownerUsername
     *            Name of the owner user.
     * @param ownerGroupname
     *            Name of the owner group.
     * @param accessPolicy_type
     *            Access policy type (Null, Posix, Volume, ...).
     * @param defaultStripingPolicyType
     *            Only RAID0 so far.
     * @param defaultStripeSize
     *            Size of an object on the OSD (in kBytes).
     * @param defaultStripeWidth
     *            Number of OSDs objects of a file are striped across.
     * @param volumeAttributes
     *            Reference to a list of key-value pairs of volume attributes which will bet set at creation
     *            time of the volume.
     * 
     * @throws IOException
     * @throws PosixErrorException
     */
    public abstract void createVolume(String mrcAddress, Auth auth, UserCredentials userCredentials,
            String volumeName, int mode, String ownerUsername, String ownerGroupname,
            AccessControlPolicyType accessPolicyType, StripingPolicyType defaultStripingPolicyType,
            int defaultStripeSize, int defaultStripeWidth, List<KeyValuePair> volumeAttributes)
            throws IOException;

    // TODO(mberlin): Also provide a method which accepts a list of MRC addresses
    // or an UUID Iterator object which contains all addresses.
    /**
     * Creates a volume on the MRC at mrc_address.
     * 
     * @param mrcAddress
     *            String of the form "hostname:port".
     * @param auth
     *            Authentication data, e.g. of type AUTH_PASSWORD.
     * @param userCredentials
     *            Username and groups of the user who executes CreateVolume().
     * @param volumeName
     *            Name of the new volume.
     * @param mode
     *            Mode of the volume's root directory (in octal representation (e.g. 511), not decimal (777)).
     * @param ownerUsername
     *            Name of the owner user.
     * @param ownerGroupname
     *            Name of the owner group.
     * @param accessPolicy_type
     *            Access policy type (Null, Posix, Volume, ...).
     * @param defaultStripingPolicyType
     *            Only RAID0 so far.
     * @param defaultStripeSize
     *            Size of an object on the OSD (in kBytes).
     * @param defaultStripeWidth
     *            Number of OSDs objects of a file are striped across.
     * @param volumeAttributes
     *            Reference to a list of key-value pairs of volume attributes which will bet set at creation
     *            time of the volume.
     * 
     * @throws IOException
     * @throws PosixErrorException
     */
    public abstract void createVolume(List<String> mrcAddresses, Auth auth, UserCredentials userCredentials,
            String volumeName, int mode, String ownerUsername, String ownerGroupname,
            AccessControlPolicyType accessPolicyType, StripingPolicyType defaultStripingPolicyType,
            int defaultStripeSize, int defaultStripeWidth, List<KeyValuePair> volumeAttributes)
            throws IOException;

    /**
     * Deletes the volume "volume_name" at the MRC "mrc_address".
     * 
     * @param mrcAddress
     *            String of the form "hostname:port".
     * @param auth
     *            Authentication data, e.g. of type AUTH_PASSWORD.
     * @param userCredentials
     *            Username and groups of the user who executes CreateVolume().
     * @param volumeName
     *            Name of the volume to be deleted.
     * 
     * @throws IOException
     * @throws PosixErrorException
     */
    public abstract void deleteVolume(String mrcAddress, Auth auth, UserCredentials userCredentials,
            String volumeName) throws IOException;

    // TODO(mberlin): Also provide a method which accepts a list of MRC addresses
    // or an UUID Iterator object which contains all addresses.
    /**
     * Deletes the volume "volume_name" at the MRC "mrc_address".
     * 
     * @param mrcAddress
     *            String of the form "hostname:port".
     * @param auth
     *            Authentication data, e.g. of type AUTH_PASSWORD.
     * @param userCredentials
     *            Username and groups of the user who executes CreateVolume().
     * @param volumeName
     *            Name of the volume to be deleted.
     * 
     * @throws IOException
     * @throws PosixErrorException
     */
    public abstract void deleteVolume(List<String> mrcAddresses, Auth auth, UserCredentials userCredentials,
            String volumeName) throws IOException;

    /**
     * Returns the available volumes on a MRC.
     * 
     * @param mrcAddress
     *            String of the form "hostname:port".
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws IOException
     * @throws PosixErrorException
     * 
     */
    public abstract Volumes listVolumes(String mrcAddress) throws IOException;

    /**
     * Returns the available volumes on a MRC.
     * 
     * @param uuidIteratorWithMRCAddresses
     *            UUIDIterator object which contains MRC addresses of the form "hostname:port".
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws IOException
     * @throws PosixErrorException
     * 
     * @remark Ownership of the return value is transferred to the caller.
     */
    // Volumes ListVolumes(UUIDIterator uuidIteratorWithMRCAddresses) {
    // //TODO: IMplement!
    // }

    /**
     * Returns a pointer to a UUIDResolver object, which provides functions to resolve UUIDs to IP-Addresses
     * and Ports.
     * 
     * @remark Ownership is NOT transferred to the caller.
     */
    public abstract UUIDResolver getUUIDResolver();

}
