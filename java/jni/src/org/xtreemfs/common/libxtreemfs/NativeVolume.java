package org.xtreemfs.common.libxtreemfs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.xtreemfs.common.libxtreemfs.exceptions.AddressToUUIDNotFoundException;
import org.xtreemfs.common.libxtreemfs.exceptions.PosixErrorException;
import org.xtreemfs.common.libxtreemfs.swig.FileHandleProxy;
import org.xtreemfs.common.libxtreemfs.swig.VectorString;
import org.xtreemfs.common.libxtreemfs.swig.VolumeProxy;
import org.xtreemfs.common.xloc.ReplicationPolicyImplementation;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.mrc.metadata.ReplicationPolicy;
import org.xtreemfs.mrc.utils.MRCHelper;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.DirectoryEntries;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Stat;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.XATTR_FLAGS;

public class NativeVolume extends VolumeProxy implements Volume {

    private static final String XTREEMFS_DEFAULT_RP      = "xtreemfs.default_rp";
    private static final String OSD_SELECTION_POLICY     = "xtreemfs.osel_policy";
    private static final String REPLICA_SELECTION_POLICY = "xtreemfs.rsel_policy";

    /**
     * Name of this volume.
     */
    private final String        volumeName;

    public NativeVolume(VolumeProxy volume, String volumeName) {
        super(getCPtr(volume), false);
        this.volumeName = volumeName;
    }

    public NativeVolume(VolumeProxy volume) {
        this(volume, null);
    }

    @Override
    public void internalShutdown() {
        // not needed for the C++ volume
    }

    @Override
    public void start() throws Exception {
        // not needed for the C++ volume
    }

    @Override
    public void start(boolean startThreadsAsDaemons) throws Exception {
        if (startThreadsAsDaemons) {
            throw new RuntimeException("starting threads as daemons is not supported.");
        }
    }

    @Override
    public String readLink(UserCredentials userCredentials, String path) throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException {
        String[] result = { null };
        readLink(userCredentials, path, result);
        return result[0];
    }

    @Override
    public NativeFileHandle openFile(UserCredentials userCredentials, String path, int flags) throws IOException,
            PosixErrorException, AddressToUUIDNotFoundException {
        FileHandleProxy fileHandleProxy = openFileProxy(userCredentials, path, flags);
        return (new NativeFileHandle(fileHandleProxy));
    }

    @Override
    public NativeFileHandle openFile(UserCredentials userCredentials, String path, int flags, int mode)
            throws IOException,
            PosixErrorException, AddressToUUIDNotFoundException {
        FileHandleProxy fileHandleProxy = openFileProxy(userCredentials, path, flags, mode);
        return (new NativeFileHandle(fileHandleProxy));
    }

    @Override
    public void createDirectory(UserCredentials userCredentials, String path, int mode, boolean recursive)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        if (recursive) {
            if (path.equals("/")) {
                return;
            }
            if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            final String parent = path.substring(0, path.lastIndexOf("/"));
            if (isDirectory(userCredentials, parent) || parent.isEmpty()) {
                createDirectory(userCredentials, path, mode, false);
            } else {
                createDirectory(userCredentials, parent, mode, true);
                createDirectory(userCredentials, path, mode, false);
            }
        } else {
            makeDirectory(userCredentials, path, mode);
        }
    }

    private boolean isDirectory(UserCredentials userCredentials, String path) throws PosixErrorException, IOException,
            AddressToUUIDNotFoundException {
        // try {
            Stat stat = getAttr(userCredentials, path);
            return (stat.getMode() & SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_S_IFDIR.getNumber()) > 0;
        /*
         * TODO (jdillmann): } catch (PosixErrorException pee) { if
         * (pee.getPosixError().equals(POSIXErrno.POSIX_ERROR_ENOENT)) { return false; } else { throw pee; } }
         */
    }

    @Override
    public void createDirectory(UserCredentials userCredentials, String path, int mode) throws IOException,
            PosixErrorException, AddressToUUIDNotFoundException {
        createDirectory(userCredentials, path, mode, false);
    }

    @Override
    public void removeDirectory(UserCredentials userCredentials, String path) throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException {
        deleteDirectory(userCredentials, path);
    }

    @Override
    public DirectoryEntries readDir(UserCredentials userCredentials, String path, int offset, int count,
            boolean namesOnly) throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        return readDir(userCredentials, path, (long) offset, (long) count, namesOnly);
    }

    @Override
    public void setXAttr(UserCredentials userCredentials, Auth auth, String path, String name, String value,
            XATTR_FLAGS flags) throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        // TODO(jdillmann): Auth param missing
    }

    @Override
    public String getXAttr(UserCredentials userCredentials, String path, String name) throws IOException,
            PosixErrorException, AddressToUUIDNotFoundException {
        String[] value = { null };

        boolean result = getXAttr(userCredentials, path, name, value);
        if (!result) {
            return null;
        }

        return value[0];
    }

    @Override
    public int getXAttrSize(UserCredentials userCredentials, String path, String name) throws IOException,
            PosixErrorException, AddressToUUIDNotFoundException {
        int[] size = { -1 };

        boolean result = getXAttrSize(userCredentials, path, name, size);
        if (!result) {
            return -1;
        }
        return size[0];
    }

    @Override
    public List<String> getSuitableOSDs(UserCredentials userCredentials, String path, int numberOfOsds)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        // TODO(jdillmann): This is really really inefficient: list will be copied 3 times!!! think of a better solution
        VectorString vs = new VectorString();
        getSuitableOSDs(userCredentials, path, numberOfOsds, vs);

        List<String> list = new ArrayList<String>();
        for (int i = 0, c = (int) vs.size(); i < c; ++i) {
            list.add(vs.get(i));
        }

        vs.delete();
        return list;
    }

    @Override
    public void setDefaultReplicationPolicy(UserCredentials userCredentials, String directory,
            String replicationPolicy, int replicationFactor, int replicationFlags) throws IOException,
            PosixErrorException, AddressToUUIDNotFoundException {
        String JSON = "{ " + "\"replication-factor\": " + String.valueOf(replicationFactor) + ","
                + "\"update-policy\": " + "\"" + replicationPolicy + "\"," + "\"replication-flags\": "
                + String.valueOf(replicationFlags) + " }";
        setXAttr(userCredentials, directory, XTREEMFS_DEFAULT_RP, JSON, XATTR_FLAGS.XATTR_FLAGS_CREATE);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ReplicationPolicy getDefaultReplicationPolicy(UserCredentials userCredentials, String directory)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        Object replicationPolicyObject;
        Map<String, Object> replicationPolicyMap;

        try {
            String rpAsJSON = getXAttr(userCredentials, directory, XTREEMFS_DEFAULT_RP);
            replicationPolicyObject = JSONParser.parseJSON(new JSONString(rpAsJSON));
        } catch (JSONException e) {
            throw new IOException(e);
        }

        try {
            replicationPolicyMap = (Map<String, Object>) replicationPolicyObject;
        } catch (ClassCastException e) {
            throw new IOException("JSON response does not contain a Map.", e);
        }

        if (!(replicationPolicyMap.containsKey("replication-factor")
                && replicationPolicyMap.containsKey("update-policy") && replicationPolicyMap
                    .containsKey("replication-flags"))) {
            throw new IOException("Incomplete JSON response from MRC.");
        }

        final String updatePolicy;
        final int replicationFactor;
        final int replicationFlags;
        try {
            // The JSONParser returns every number as a Long object.
            replicationFlags = ((Long) replicationPolicyMap.get("replication-flags")).intValue();
            replicationFactor = ((Long) replicationPolicyMap.get("replication-factor")).intValue();
            updatePolicy = (String) replicationPolicyMap.get("update-policy");
        } catch (ClassCastException e) {
            throw new IOException(e);
        }

        return new ReplicationPolicyImplementation(updatePolicy, replicationFactor, replicationFlags);
    }

    @Override
    public List<StripeLocation> getStripeLocations(UserCredentials userCredentials, String path, long startSize,
            long length) throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void removeACL(UserCredentials userCreds, String path, String user) throws IOException {
        Set<String> elements = new HashSet<String>();
        elements.add(user);
        removeACL(userCreds, path, elements);
    }

    @Override
    public void removeACL(UserCredentials userCreds, String path, Set<String> aclEntries) throws IOException {
        // add all entries from the given list
        for (String entity : aclEntries) {
            if (!entity.equals("u:") && !entity.equals("g:") && !entity.equals("o:") && !entity.equals("m:")) {
                setXAttr(userCreds, path, "xtreemfs.acl", "x " + entity, XATTR_FLAGS.XATTR_FLAGS_REPLACE);
            }
        }
    }

    @Override
    public void setACL(UserCredentials userCreds, String path, String user, String accessrights) throws IOException {
        HashMap<String, Object> elements = new HashMap<String, Object>();
        elements.put(user, accessrights);
        setACL(userCreds, path, elements);
    }

    @Override
    public void setACL(UserCredentials userCreds, String path, Map<String, Object> aclEntries) throws IOException {
        // add all entries from the given list
        for (Entry<String, Object> entry : aclEntries.entrySet())
            setXAttr(userCreds, path, "xtreemfs.acl", "m " + entry.getKey() + ":" + entry.getValue(),
                    XATTR_FLAGS.XATTR_FLAGS_REPLACE);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> listACL(UserCredentials userCreds, String path) throws IOException {
        try {
            String aclAsJSON = getXAttr(userCreds, path, "xtreemfs.acl");
            return (Map<String, Object>) JSONParser.parseJSON(new JSONString(aclAsJSON));
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    @Override
    public String getOSDSelectionPolicy(UserCredentials userCreds) throws IOException {
        return getXAttr(userCreds, "/", OSD_SELECTION_POLICY);
    }

    @Override
    public void setOSDSelectionPolicy(UserCredentials userCreds, String policies) throws IOException {
        setXAttr(userCreds, "/", OSD_SELECTION_POLICY, policies, XATTR_FLAGS.XATTR_FLAGS_REPLACE);
    }

    @Override
    public String getReplicaSelectionPolicy(UserCredentials userCreds) throws IOException {
        return getXAttr(userCreds, "/", REPLICA_SELECTION_POLICY);
    }

    @Override
    public void setReplicaSelectionPolicy(UserCredentials userCreds, String policies) throws IOException {
        setXAttr(userCreds, "/", REPLICA_SELECTION_POLICY, policies, XATTR_FLAGS.XATTR_FLAGS_REPLACE);
    }

    @Override
    public void setPolicyAttribute(UserCredentials userCreds, String attribute, String value) throws IOException {
        setXAttr(userCreds, "/", MRCHelper.XTREEMFS_POLICY_ATTR_PREFIX + attribute, value,
                XATTR_FLAGS.XATTR_FLAGS_REPLACE);
    }

    @Override
    public String getVolumeName() {
        return volumeName;
    }

}
