/*
 * Copyright (c) 2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.xtreemfs.common.libxtreemfs.exceptions.AddressToUUIDNotFoundException;
import org.xtreemfs.common.libxtreemfs.exceptions.PosixErrorException;
import org.xtreemfs.common.libxtreemfs.exceptions.VolumeNotFoundException;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.AccessControlPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Volumes;

/**
 * Provides methods to open, close, create, delete and list volumes and to instantiate a new client object, to
 * start and shutdown a Client object.
 */
public interface Client {
    /**
     * Need to be called before client can be used. Initializes required stuff.
     */
    public void start() throws Exception;
    
    /**
     * Same as start(), but add option to start Threads as daemons. Daemon threads are only used by the XtreemFSHadoopClient.
     * 
     * @param startThreadsAsDaemons if true, all threads are as daemons.
     */
    public void start(boolean startThreadsAsDaemons) throws Exception;

    public void shutdown();

    /**
     * Open a volume and use the returned class to access it.
     * 
     * @remark Ownership is NOT transferred to the caller. Instead Volume->Close() has to be called to destroy
     *         the object.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws VolumeNotFoundException
     * @throws {@link IOException}
     */
    public Volume openVolume(String volumeName, SSLOptions sslOptions, Options options)
            throws AddressToUUIDNotFoundException, VolumeNotFoundException, IOException;

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
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException;

    /**
     * Creates a volume on the MRC at mrc_address using certain default values ( POSIX access policy type,
     * striping size = 128k and width = 1 (i.e. no striping), mode = 777 and owner username and groupname
     * retrieved from the user_credentials.
     * 
     * @param mrcAddresses
     *            List of Strings of the form "hostname:port".
     * @param auth
     *            Authentication data, e.g. of type AUTH_PASSWORD.
     * @param userCredentials
     *            Username and groups of the user who executes CreateVolume(). Not checked so far?
     * @param volumeName
     *            Name of the new volume.
     * 
     * @throws IOException
     * @throws PosixErrorException
     * @throws AddressToUUIDNotFoundException
     */
    public void createVolume(List<String> mrcAddresses, Auth auth, UserCredentials userCredentials,
            String volumeName) throws IOException, PosixErrorException, AddressToUUIDNotFoundException;

    /**
     * Creates a volume on the first found MRC.
     * 
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
     * @param accessPolicyType
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
     * @throws AddressToUUIDNotFoundException
     * @throws PosixErrorException
     * @throws IOException
     */
    public void createVolume(Auth auth, UserCredentials userCredentials, String volumeName, int mode,
            String ownerUsername, String ownerGroupname, AccessControlPolicyType accessPolicyType,
            StripingPolicyType defaultStripingPolicyType, int defaultStripeSize, int defaultStripeWidth,
            List<KeyValuePair> volumeAttributes) throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException;

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
     * @param accessPolicyType
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
     * @throws AddressToUUIDNotFoundException
     */
    public void createVolume(String mrcAddress, Auth auth, UserCredentials userCredentials,
            String volumeName, int mode, String ownerUsername, String ownerGroupname,
            AccessControlPolicyType accessPolicyType, StripingPolicyType defaultStripingPolicyType,
            int defaultStripeSize, int defaultStripeWidth, List<KeyValuePair> volumeAttributes)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException;

    /**
     * Creates a volume on the MRC at mrc_address.
     * 
     * @param mrcAddresses
     *            List of Strings of the form "hostname:port".
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
     * @param accessPolicyType
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
     * @throws AddressToUUIDNotFoundException
     */
    public void createVolume(List<String> mrcAddresses, Auth auth, UserCredentials userCredentials,
            String volumeName, int mode, String ownerUsername, String ownerGroupname,
            AccessControlPolicyType accessPolicyType, StripingPolicyType defaultStripingPolicyType,
            int defaultStripeSize, int defaultStripeWidth, List<KeyValuePair> volumeAttributes)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException;

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
    public void deleteVolume(String mrcAddress, Auth auth, UserCredentials userCredentials, String volumeName)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException;

    /**
     * Deletes the volume "volume_name" at the MRC "mrc_address".
     * 
     * @param mrcAddresses
     *            List of Strings of the form "hostname:port".
     * @param auth
     *            Authentication data, e.g. of type AUTH_PASSWORD.
     * @param userCredentials
     *            Username and groups of the user who executes CreateVolume().
     * @param volumeName
     *            Name of the volume to be deleted.
     * 
     * @throws IOException
     * @throws PosixErrorException
     * @throws AddressToUUIDNotFoundException
     */
    public void deleteVolume(List<String> mrcAddresses, Auth auth, UserCredentials userCredentials,
            String volumeName) throws IOException, PosixErrorException, AddressToUUIDNotFoundException;

    /**
     * Retrieves the first MRC from the DIR and deletes the volume "volume_name" at this the MRC.
     * 
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
    public void deleteVolume(Auth auth, UserCredentials userCredentials, String volumeName)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException;

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
    public Volumes listVolumes(String mrcAddress) throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException;

    /**
     * Returns the available volumes on a MRC.
     * 
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws IOException
     * @throws PosixErrorException
     * 
     * @remark Ownership of the return value is transferred to the caller.
     */
    @Deprecated
    public Volumes listVolumes() throws IOException, PosixErrorException, AddressToUUIDNotFoundException;

    /**
     * Returns the available volumes as list of names
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws IOException
     * @throws PosixErrorException
     * 
     */
    public String[] listVolumeNames() throws IOException;

    /**
     * Returns the available volumes on a MRC.
     * 
     * @param mrcAddresses
     *            <String> List which contains MRC addresses of the form "hostname:port".
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws IOException
     * @throws PosixErrorException
     * 
     * @remark Ownership of the return value is transferred to the caller.
     */
    public Volumes listVolumes(List<String> mrcAddresses) throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException;

    /**
     * List all servers and return their host names
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws IOException
     * @throws PosixErrorException
     */
    @Deprecated
    public Map<String, Service> listServers() throws IOException, PosixErrorException;

    /**
     * Returns the registered UUID of the OSDs and their attributes on the DIR.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws IOException
     * @throws PosixErrorException
     */
    @Deprecated
    public Map<String, Service> listOSDsAndAttributes() throws IOException, PosixErrorException;
}
