/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.clients;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.clients.internal.OpenFileList;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.client.PBRPCException;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDWriteResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.VivaldiCoordinates;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XCap;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XLocSet;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.DirectoryEntries;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.DirectoryEntry;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Setattrs;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Stat;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.StatVFS;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.XAttr;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.getattrResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.getxattrResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.listxattrResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.openResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.unlinkResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_get_suitable_osdsRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_get_suitable_osdsResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_get_xlocsetRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_replica_addRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_replica_addResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_replica_removeRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_replica_removeResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_set_replica_update_policyRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_update_file_sizeRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;

import com.google.protobuf.ByteString;

/**
 * 
 * @author bjko
 */
public class Volume {

    private final MRCServiceClient          mrcClient;

    final UUIDResolver                      uuidResolver;

    private final String                    volumeName;

    private final UserCredentials           userCreds;

    protected final OSDServiceClient        osdClient;

    private final OpenFileList              ofl;

    private final int                       maxRetries;

    private static final VivaldiCoordinates emptyCoordinates;

    static {
        emptyCoordinates = VivaldiCoordinates.newBuilder().setLocalError(0).setXCoordinate(0).setYCoordinate(0).build();
    }

    /*
     * private final LRUCache<String,CachedXAttr> xattrCache;
     * 
     * private final int mdCacheTimeout_ms;
     */

    Volume(OSDServiceClient osdClient, MRCServiceClient client, String volumeName, UUIDResolver uuidResolver,
            UserCredentials userCreds) {
        this(osdClient, client, volumeName, uuidResolver, userCreds, 0, 5);
    }

    Volume(OSDServiceClient osdClient, MRCServiceClient client, String volumeName, UUIDResolver uuidResolver,
            UserCredentials userCreds, int mdCacheTimeout_ms, int maxRetries) {
        this.mrcClient = client;
        this.volumeName = volumeName.endsWith("/") ? volumeName : volumeName + "/";
        this.uuidResolver = uuidResolver;
        this.userCreds = userCreds;
        this.osdClient = osdClient;
        this.maxRetries = maxRetries;
        this.ofl = new OpenFileList(client);
        /*
         * this.xattrCache = new LRUCache<String, CachedXAttr>(2048);
         * this.mdCacheTimeout_ms = mdCacheTimeout_ms;
         */
        ofl.start();
    }

    /**
     * same semantics as File.list()
     * 
     * @param path
     * @param cred
     * @return
     * @throws IOException
     */
    public String[] list(String path) throws IOException {
        return list(path, userCreds);
    }

    public String[] list(String path, UserCredentials userCreds) throws IOException {

        RPCResponse<DirectoryEntries> response = null;

        final String fixedVol = fixPath(volumeName);
        final String fixedPath = fixPath(path);
        try {
            response = mrcClient.readdir(null, RPCAuthentication.authNone, userCreds, fixedVol, fixedPath, 0, 0, true,
                    0);
            DirectoryEntries entries = response.get();
            String[] list = new String[entries.getEntriesCount()];
            for (int i = 0; i < list.length; i++) {
                list[i] = entries.getEntries(i).getName();
            }
            return list;
        } catch (PBRPCException ex) {
            if (ex.getPOSIXErrno() == POSIXErrno.POSIX_ERROR_ENOENT)
                return null;
            throw wrapException(ex);
        } catch (InterruptedException ex) {
            throw wrapException(ex);
        } finally {
            if (response != null)
                response.freeBuffers();
        }
    }

    public DirectoryEntry[] listEntries(String path, UserCredentials userCreds) throws IOException {
        RPCResponse<DirectoryEntries> response = null;
        path = path.replace("//", "/");
        final String fixedVol = fixPath(volumeName);
        final String fixedPath = fixPath(path);
        try {
            response = mrcClient.readdir(null, RPCAuthentication.authNone, userCreds, fixedVol, fixedPath, 0, 0, false,
                    0);
            DirectoryEntries entries = response.get();
            DirectoryEntry[] list = new DirectoryEntry[entries.getEntriesCount()];
            for (int i = 0; i < list.length; i++) {
                list[i] = entries.getEntries(i);
                Stat s = list[i].getStbuf();
                OSDWriteResponse r = ofl.getLocalFS(volumeName + s.getIno());
                if (r != null && r.hasTruncateEpoch()) {
                    // update with local file size, if cahced
                    if ((r.getTruncateEpoch() > s.getTruncateEpoch()) || (r.getTruncateEpoch() == s.getTruncateEpoch())
                            && (r.getSizeInBytes() > s.getSize())) {
                        s = s.toBuilder().setSize(r.getSizeInBytes()).setTruncateEpoch(r.getTruncateEpoch()).build();
                        list[i] = list[i].toBuilder().setStbuf(s).build();
                    }
                }
            }
            return list;
        } catch (PBRPCException ex) {
            if (ex.getPOSIXErrno() == POSIXErrno.POSIX_ERROR_ENOENT)
                return null;
            throw wrapException(ex);
        } catch (InterruptedException ex) {
            throw wrapException(ex);
        } finally {
            if (response != null)
                response.freeBuffers();
        }
    }

    public DirectoryEntry[] listEntries(String path) throws IOException {
        return listEntries(path, userCreds);
    }

    public File getFile(String path, UserCredentials userCreds) {
        return new File(this, userCreds, path);
    }

    public File getFile(String path) {
        return new File(this, userCreds, path);
    }

    public String getName() {
        return volumeName;
    }

    String fixPath(String path) {
        path = path.replace("//", "/");
        if (path.endsWith("/"))
            path = path.substring(0, path.length() - 1);
        if (path.startsWith("/"))
            path = path.substring(1);
        return path;
    }

    public long getFreeSpace(UserCredentials userCreds) throws IOException {
        RPCResponse<StatVFS> response = null;
        try {
            response = mrcClient.statvfs(null, RPCAuthentication.authNone, userCreds, volumeName.replace("/", ""), 0);
            StatVFS fsinfo = response.get();
            return fsinfo.getBavail() * fsinfo.getBsize();
        } catch (PBRPCException ex) {
            throw wrapException(ex);
        } catch (InterruptedException ex) {
            throw wrapException(ex);
        } finally {
            if (response != null)
                response.freeBuffers();
        }
    }

    public long getFreeSpace() throws IOException {
        return getFreeSpace(userCreds);
    }

    public StatVFS statfs(UserCredentials userCreds) throws IOException {
        RPCResponse<StatVFS> response = null;
        try {
            response = mrcClient.statvfs(null, RPCAuthentication.authNone, userCreds, volumeName.replace("/", ""), 0);
            StatVFS fsinfo = response.get();
            return fsinfo;
        } catch (PBRPCException ex) {
            throw wrapException(ex);
        } catch (InterruptedException ex) {
            throw wrapException(ex);
        } finally {
            if (response != null)
                response.freeBuffers();
        }
    }

    public StatVFS statfs() throws IOException {
        return statfs(userCreds);
    }

    public boolean isReplicateOnClose(UserCredentials userCreds) throws IOException {
        String numRepl = getxattr(fixPath(volumeName), "xtreemfs.repl_factor", userCreds);
        if (numRepl == null)
            return false;
        return numRepl.equals("1");
    }

    public boolean isReplicateOnClose() throws IOException {
        return isReplicateOnClose(userCreds);
    }

    public boolean isSnapshot() {
        return volumeName.indexOf('@') != -1;
    }

    public int getDefaultReplicationFactor(UserCredentials userCreds) throws IOException {
        String numRepl = getxattr(fixPath(volumeName), "xtreemfs.repl_factor", userCreds);
        try {
            return Integer.valueOf(numRepl);
        } catch (Exception ex) {
            throw new IOException("cannot fetch replication factor", ex);
        }
    }

    public int getDefaultReplicationFactor() throws IOException {
        return getDefaultReplicationFactor(userCreds);
    }

    public long getUsedSpace(UserCredentials userCreds) throws IOException {
        RPCResponse<StatVFS> response = null;
        try {
            response = mrcClient.statvfs(null, RPCAuthentication.authNone, userCreds, volumeName.replace("/", ""), 0);
            StatVFS fsinfo = response.get();
            return (fsinfo.getBlocks() - fsinfo.getBavail()) * fsinfo.getBsize();
        } catch (PBRPCException ex) {
            throw wrapException(ex);
        } catch (InterruptedException ex) {
            throw wrapException(ex);
        } finally {
            if (response != null)
                response.freeBuffers();
        }
    }

    public long getUsedSpace() throws IOException {
        return getUsedSpace(userCreds);
    }

    public long getDefaultObjectSize(UserCredentials userCreds) throws IOException {
        RPCResponse<StatVFS> response = null;
        try {
            response = mrcClient.statvfs(null, RPCAuthentication.authNone, userCreds, volumeName.replace("/", ""), 0);
            StatVFS fsinfo = response.get();
            return fsinfo.getBsize();
        } catch (PBRPCException ex) {
            throw wrapException(ex);
        } catch (InterruptedException ex) {
            throw wrapException(ex);
        } finally {
            if (response != null)
                response.freeBuffers();
        }
    }

    public long getDefaultObjectSize() throws IOException {
        return getDefaultObjectSize(userCreds);
    }

    public void enableSnapshots(boolean enable, UserCredentials userCreds) throws IOException {
        RPCResponse r = null;
        try {
            String value = enable + "";
            r = mrcClient.setxattr(null, RPCAuthentication.authNone, userCreds, volumeName.replace("/", ""), "",
                    "xtreemfs.snapshots_enabled", value, ByteString.copyFrom(value.getBytes()), 0);
            r.get();
        } catch (PBRPCException ex) {
            throw wrapException(ex);
        } catch (InterruptedException ex) {
            throw wrapException(ex);
        } finally {
            if (r != null)
                r.freeBuffers();
        }
    }

    public void enableSnapshots(boolean enable) throws IOException {
        enableSnapshots(enable, userCreds);
    }

    public void snapshot(String name, boolean recursive, UserCredentials userCreds) throws IOException {
        RPCResponse r = null;
        try {
            String cmd = "c" + (recursive ? "r" : "") + " " + name;
            r = mrcClient.setxattr(null, RPCAuthentication.authNone, userCreds, volumeName.replace("/", ""), "",
                    "xtreemfs.snapshots", cmd, ByteString.copyFrom(cmd.getBytes()), 0);
            r.get();
        } catch (PBRPCException ex) {
            throw wrapException(ex);
        } catch (InterruptedException ex) {
            throw wrapException(ex);
        } finally {
            if (r != null)
                r.freeBuffers();
        }
    }

    public void snapshot(String name, boolean recursive) throws IOException {
        snapshot(name, recursive, userCreds);
    }

    Stat stat(String path, UserCredentials userCreds) throws IOException {
        RPCResponse<getattrResponse> response = null;
        try {
            response = mrcClient.getattr(null, RPCAuthentication.authNone, userCreds, fixPath(volumeName),
                    fixPath(path), 0);
            Stat s = response.get().getStbuf();
            OSDWriteResponse r = ofl.getLocalFS(volumeName + s.getIno());
            if (r != null && r.hasTruncateEpoch()) {
                // update with local file size, if cahced
                if ((r.getTruncateEpoch() > s.getTruncateEpoch()) || (r.getTruncateEpoch() == s.getTruncateEpoch())
                        && (r.getSizeInBytes() > s.getSize())) {
                    s = s.toBuilder().setSize(r.getSizeInBytes()).setTruncateEpoch(r.getTruncateEpoch()).build();
                }
            }
            return s;
        } catch (PBRPCException ex) {
            throw wrapException(ex);
        } catch (InterruptedException ex) {
            throw wrapException(ex);
        } finally {
            if (response != null)
                response.freeBuffers();
        }
    }

    String getxattr(String path, String name, UserCredentials userCreds) throws IOException {
        RPCResponse<getxattrResponse> response = null;
        try {
            response = mrcClient.getxattr(null, RPCAuthentication.authNone, userCreds, fixPath(volumeName),
                    fixPath(path), name);
            return response.get().getValue();
        } catch (PBRPCException ex) {
            if (ex.getPOSIXErrno() == POSIXErrno.POSIX_ERROR_ENODATA)
                return null;
            throw wrapException(ex);
        } catch (InterruptedException ex) {
            throw wrapException(ex);
        } finally {
            if (response != null)
                response.freeBuffers();
        }
    }

    String[] listxattr(String path, UserCredentials userCreds) throws IOException {
        RPCResponse<listxattrResponse> response = null;
        try {
            response = mrcClient.listxattr(null, RPCAuthentication.authNone, userCreds, fixPath(volumeName),
                    fixPath(path), true);
            listxattrResponse result = response.get();
            List<XAttr> attrs = result.getXattrsList();
            String[] names = new String[attrs.size()];
            for (int i = 0; i < names.length; i++)
                names[i] = attrs.get(i).getName();
            return names;
        } catch (PBRPCException ex) {
            if (ex.getPOSIXErrno() == POSIXErrno.POSIX_ERROR_ENODATA)
                return null;
            throw wrapException(ex);
        } catch (InterruptedException ex) {
            throw wrapException(ex);
        } finally {
            if (response != null)
                response.freeBuffers();
        }
    }

    void setxattr(String path, String name, String value, UserCredentials userCreds) throws IOException {
        RPCResponse response = null;
        try {
            response = mrcClient.setxattr(null, RPCAuthentication.authNone, userCreds, fixPath(volumeName),
                    fixPath(path), name, value, ByteString.copyFrom(value.getBytes()), 0);
            response.get();
        } catch (PBRPCException ex) {
            throw wrapException(ex);
        } catch (InterruptedException ex) {
            throw wrapException(ex);
        } finally {
            if (response != null)
                response.freeBuffers();
        }
    }

    void mkdir(String path, int permissions, UserCredentials userCreds) throws IOException {
        RPCResponse response = null;
        try {
            response = mrcClient.mkdir(null, RPCAuthentication.authNone, userCreds, fixPath(volumeName), fixPath(path),
                    permissions);
            response.get();
        } catch (PBRPCException ex) {
            throw wrapException(ex);
        } catch (InterruptedException ex) {
            throw wrapException(ex);
        } finally {
            if (response != null)
                response.freeBuffers();
        }
    }

    void touch(String path, UserCredentials userCreds) throws IOException {
        RPCResponse response = null;
        try {
            response = mrcClient.open(null, RPCAuthentication.authNone, userCreds, fixPath(volumeName), fixPath(path),
                    GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber(), 0700, 0, emptyCoordinates);
            response.get();
        } catch (PBRPCException ex) {
            throw wrapException(ex);
        } catch (InterruptedException ex) {
            throw wrapException(ex);
        } finally {
            if (response != null)
                response.freeBuffers();
        }
    }

    void rename(String src, String dest, UserCredentials userCreds) throws IOException {
        RPCResponse response = null;
        try {
            response = mrcClient.rename(null, RPCAuthentication.authNone, userCreds, fixPath(volumeName), fixPath(src),
                    fixPath(dest));
            response.get();
        } catch (PBRPCException ex) {
            throw wrapException(ex);
        } catch (InterruptedException ex) {
            throw wrapException(ex);
        } finally {
            if (response != null)
                response.freeBuffers();
        }
    }

    void unlink(String path, UserCredentials userCreds) throws IOException {
        RPCResponse<unlinkResponse> response = null;
        RPCResponse ulnkResp = null;
        try {
            response = mrcClient
                    .unlink(null, RPCAuthentication.authNone, userCreds, fixPath(volumeName), fixPath(path));
            unlinkResponse resp = response.get();
            final FileCredentials fcs = resp.hasCreds() ? resp.getCreds() : null;
            if (fcs != null) {
                // delete on OSDs
                for (GlobalTypes.Replica r : fcs.getXlocs().getReplicasList()) {
                    final String headOSDuuid = r.getOsdUuids(0);
                    final ServiceUUID osdAddr = new ServiceUUID(headOSDuuid, uuidResolver);
                    osdAddr.resolve();
                    ulnkResp = osdClient.unlink(osdAddr.getAddress(), RPCAuthentication.authNone,
                            RPCAuthentication.userService, fcs, fcs.getXcap().getFileId());
                    ulnkResp.get();
                    ulnkResp.freeBuffers();
                    ulnkResp = null;
                }
            }
        } catch (PBRPCException ex) {
            throw wrapException(ex);
        } catch (InterruptedException ex) {
            throw wrapException(ex);
        } finally {
            if (response != null)
                response.freeBuffers();
            if (ulnkResp != null)
                ulnkResp.freeBuffers();
        }
    }

    void storeFileSizeUpdate(String fileId, OSDWriteResponse resp, UserCredentials userCreds) {
        ofl.fsUpdate(fileId, resp);
    }

    void pushFileSizeUpdate(String fileId, UserCredentials userCreds) throws IOException {
        OSDWriteResponse owr = ofl.sendFsUpdate(fileId);
        if (owr != null) {
            XCap cap = ofl.getCapability(fileId);
            RPCResponse response = null;
            try {
                if (!owr.hasSizeInBytes())
                    return;

                long newSize = owr.getSizeInBytes();
                int newEpoch = owr.getTruncateEpoch();

                OSDWriteResponse.Builder osdResp = OSDWriteResponse.newBuilder().setSizeInBytes(newSize)
                        .setTruncateEpoch(newEpoch);
                xtreemfs_update_file_sizeRequest fsBuf = xtreemfs_update_file_sizeRequest.newBuilder().setXcap(cap)
                        .setOsdWriteResponse(osdResp).build();

                response = mrcClient.xtreemfs_update_file_size(null, RPCAuthentication.authNone, userCreds, fsBuf);
                response.get();

            } catch (PBRPCException ex) {
                throw wrapException(ex);
            } catch (InterruptedException ex) {
                throw wrapException(ex);
            } finally {
                if (response != null)
                    response.freeBuffers();
            }
        }
    }

    void closeFile(RandomAccessFile file, String fileId, boolean readOnly, UserCredentials userCreds)
            throws IOException {

        pushFileSizeUpdate(fileId, userCreds);
        try {
            XCap cap = ofl.getCapability(fileId);
            // notify MRC that file has been closed
            RPCResponse response = null;
            try {
                response = mrcClient.xtreemfs_update_file_size(null, RPCAuthentication.authNone, userCreds, cap,
                        OSDWriteResponse.newBuilder().build(), true, emptyCoordinates);
                response.get();
            } catch (Exception ex) {
                Logging.logError(Logging.LEVEL_ERROR, this, ex);
                throw new IOException("file could not be closed due to exception");
            } finally {
                if (response != null)
                    response.freeBuffers();
            }
        } finally {
            ofl.closeFile(fileId, file);
        }

    }

    RandomAccessFile openFile(File parent, int flags, int mode, UserCredentials userCreds) throws IOException {
        RPCResponse<openResponse> response = null;
        final String fullPath = fixPath(volumeName + parent.getPath());
        final String fixedVol = fixPath(volumeName);
        final String fixedPath = fixPath(parent.getPath());
        try {
            response = mrcClient.open(null, RPCAuthentication.authNone, userCreds, fixedVol, fixedPath, flags, mode, 0,
                    emptyCoordinates);
            FileCredentials cred = response.get().getCreds();

            boolean syncMd = (flags & GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_SYNC.getNumber()) > 0;
            boolean rdOnly = cred.getXlocs().getReplicaUpdatePolicy()
                    .equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY);
            RandomAccessFile file = new RandomAccessFile(parent, this, osdClient, cred, rdOnly, syncMd, userCreds);
            ofl.openFile(cred.getXcap(), file);
            return file;
        } catch (PBRPCException ex) {
            if (ex.getPOSIXErrno() == POSIXErrno.POSIX_ERROR_ENOENT)
                throw new FileNotFoundException("file '" + fullPath + "' does not exist");
            throw wrapException(ex);
        } catch (InterruptedException ex) {
            throw wrapException(ex);
        } finally {
            if (response != null)
                response.freeBuffers();
        }
    }

    XCap truncateFile(String fileId, UserCredentials userCreds) throws IOException {
        RPCResponse<XCap> response = null;
        try {
            response = mrcClient.ftruncate(null, RPCAuthentication.authNone, userCreds, ofl.getCapability(fileId));
            return response.get();

        } catch (PBRPCException ex) {
            if (ex.getPOSIXErrno() == POSIXErrno.POSIX_ERROR_ENOENT)
                throw new FileNotFoundException("file '" + fileId + "' does not exist");
            throw wrapException(ex);
        } catch (InterruptedException ex) {
            throw wrapException(ex);
        } finally {
            if (response != null)
                response.freeBuffers();
        }
    }

    List<String> getSuitableOSDs(File file, int numOSDs, UserCredentials userCreds) throws IOException {
        String fileId = getxattr(file.getPath(), "xtreemfs.file_id", userCreds);
        RPCResponse<xtreemfs_get_suitable_osdsResponse> response = null;
        try {
            xtreemfs_get_suitable_osdsRequest request = xtreemfs_get_suitable_osdsRequest.newBuilder()
                    .setFileId(fileId).setNumOsds(numOSDs).build();
            response = mrcClient.xtreemfs_get_suitable_osds(null, RPCAuthentication.authNone, userCreds, request);
            return response.get().getOsdUuidsList();
        } catch (PBRPCException ex) {
            throw wrapException(ex);
        } catch (InterruptedException ex) {
            throw wrapException(ex);
        } finally {
            if (response != null)
                response.freeBuffers();
        }
    }

    void chmod(String path, int mode, UserCredentials userCreds) throws IOException {

        Stat stbuf = Stat.newBuilder().setAtimeNs(0).setAttributes(0).setBlksize(0).setCtimeNs(0).setDev(0).setEtag(0)
                .setGroupId("").setIno(0).setMode(mode).setMtimeNs(0).setNlink(0).setSize(0).setTruncateEpoch(0)
                .setUserId("").build();
        int toSet = Setattrs.SETATTR_MODE.getNumber();

        RPCResponse response = null;
        try {
            response = mrcClient.setattr(null, RPCAuthentication.authNone, userCreds, fixPath(volumeName),
                    fixPath(path), stbuf, toSet);
            response.get();
        } catch (PBRPCException ex) {
            throw wrapException(ex);
        } catch (InterruptedException ex) {
            throw wrapException(ex);
        } finally {
            if (response != null)
                response.freeBuffers();
        }
    }
    
    void chown(String path, String user, UserCredentials userCreds) throws IOException {

        Stat stbuf = Stat.newBuilder().setAtimeNs(0).setAttributes(0).setBlksize(0).setCtimeNs(0).setDev(0).setEtag(0)
                .setGroupId("").setIno(0).setMode(0).setMtimeNs(0).setNlink(0).setSize(0).setTruncateEpoch(0)
                .setUserId(user).build();
        int toSet = Setattrs.SETATTR_UID.getNumber();

        RPCResponse response = null;
        try {
            response = mrcClient.setattr(null, RPCAuthentication.authNone, userCreds, fixPath(volumeName),
                    fixPath(path), stbuf, toSet);
            response.get();
        } catch (PBRPCException ex) {
            throw wrapException(ex);
        } catch (InterruptedException ex) {
            throw wrapException(ex);
        } finally {
            if (response != null)
                response.freeBuffers();
        }
    }
    
    void chgrp(String path, String group, UserCredentials userCreds) throws IOException {

        Stat stbuf = Stat.newBuilder().setAtimeNs(0).setAttributes(0).setBlksize(0).setCtimeNs(0).setDev(0).setEtag(0)
                .setGroupId(group).setIno(0).setMode(0).setMtimeNs(0).setNlink(0).setSize(0).setTruncateEpoch(0)
                .setUserId("").build();
        int toSet = Setattrs.SETATTR_GID.getNumber();

        RPCResponse response = null;
        try {
            response = mrcClient.setattr(null, RPCAuthentication.authNone, userCreds, fixPath(volumeName),
                    fixPath(path), stbuf, toSet);
            response.get();
        } catch (PBRPCException ex) {
            throw wrapException(ex);
        } catch (InterruptedException ex) {
            throw wrapException(ex);
        } finally {
            if (response != null)
                response.freeBuffers();
        }
    }
    
    void setACL(String path, Map<String, Object> aclEntries, UserCredentials userCreds) throws IOException {
        
        // remove all existing entries first
        Map<String, Object> existingACL = getACL(path, userCreds);
        for (Entry<String, Object> entry : existingACL.entrySet()) {
            String entity = entry.getKey();
            if (!entity.equals("u:") && !entity.equals("g:") && !entity.equals("o:") && !entity.equals("m:"))
                setxattr(path, "xtreemfs.acl", "x " + entity, userCreds);
        }
        
        // add all entries from the given list
        for (Entry<String, Object> entry : aclEntries.entrySet())
            setxattr(path, "xtreemfs.acl", "m " + entry.getKey() + ":" + entry.getValue(), userCreds);
    }
    
    Map<String, Object> getACL(String path, UserCredentials userCreds) throws IOException {
        try {
            String aclAsJSON = getxattr(path, "xtreemfs.acl", userCreds);
            return (Map<String, Object>) JSONParser.parseJSON(new JSONString(aclAsJSON));
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    static IOException wrapException(PBRPCException ex) {
        if (ex.getPOSIXErrno() == POSIXErrno.POSIX_ERROR_ENOENT)
            return new FileNotFoundException(ex.getErrorMessage());
        return new IOException(ex.getPOSIXErrno() + ": " + ex.getErrorMessage(), ex);
    }

    static IOException wrapException(InterruptedException ex) {
        return new IOException("operation was interruped: " + ex, ex);
    }

    public void finalize() {
        ofl.shutdown();
    }

    void addReplica(File file, int width, List<String> osdSet, int flags, UserCredentials userCreds) throws IOException {

        RPCResponse<openResponse> response1 = null;
        RPCResponse<xtreemfs_replica_addResponse> response2 = null;
        RPCResponse response3 = null;
        final String fullPath = fixPath(volumeName + file.getPath());
        final String fixedVol = fixPath(volumeName);
        final String fixedPath = fixPath(file.getPath());
        try {

            org.xtreemfs.common.clients.Replica r = file.getReplica(0);
            StripingPolicy sp = StripingPolicy.newBuilder().setStripeSize(r.getStripeSize()).setWidth(width)
                    .setType(r.getStripingPolicy()).build();
            org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica newReplica = org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica
                    .newBuilder().addAllOsdUuids(osdSet).setReplicationFlags(flags).setStripingPolicy(sp).build();

            response1 = mrcClient.open(null, RPCAuthentication.authNone, userCreds, fixedVol, fixedPath, 0,
                    GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber(), 0, emptyCoordinates);
            FileCredentials oldCreds = response1.get().getCreds();
            response1.freeBuffers();
            response1 = null;

            boolean readOnlyRepl = (oldCreds.getXlocs().getReplicaUpdatePolicy()
                    .equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY));

            xtreemfs_replica_addRequest request = xtreemfs_replica_addRequest.newBuilder().setNewReplica(newReplica)
                    .setFileId(oldCreds.getXcap().getFileId()).build();
            response2 = mrcClient.xtreemfs_replica_add(null, RPCAuthentication.authNone, userCreds, request);
            xtreemfs_replica_addResponse addResponse = response2.get();
            waitForXLocSetInstallation(addResponse.getFileId(), addResponse.getExpectedXlocsetVersion());

            if (readOnlyRepl) {
                if ((flags & GlobalTypes.REPL_FLAG.REPL_FLAG_FULL_REPLICA.getNumber()) > 0) {

                    response1 = mrcClient.open(null, RPCAuthentication.authNone, userCreds, fixedVol, fixedPath, 0,
                            GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber(), 0, emptyCoordinates);
                    FileCredentials newCreds = response1.get().getCreds();

                    for (int objNo = 0; objNo < width; objNo++) {
                        ServiceUUID osd = new ServiceUUID(osdSet.get(objNo), uuidResolver);
                        response3 = osdClient
                                .read(osd.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService,
                                        newCreds, newCreds.getXcap().getFileId(), objNo, 0, 0, 1);
                        response3.get();
                        response3.freeBuffers();
                        response3 = null;
                    }
                }
            } else {

                response1 = mrcClient.open(null, RPCAuthentication.authNone, userCreds, fixedVol, fixedPath, 0,
                        GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber(), 0, emptyCoordinates);
                FileCredentials newCreds = response1.get().getCreds();

                ServiceUUID osd = new ServiceUUID(osdSet.get(0), uuidResolver);
                response3 = osdClient.xtreemfs_rwr_notify(osd.getAddress(), RPCAuthentication.authNone,
                        RPCAuthentication.userService, newCreds);
                response3.get();
                response3.freeBuffers();
                response3 = null;

            }

        } catch (PBRPCException ex) {
            if (ex.getPOSIXErrno() == POSIXErrno.POSIX_ERROR_ENOENT)
                throw new FileNotFoundException("file '" + fullPath + "' does not exist");
            throw wrapException(ex);
        } catch (InterruptedException ex) {
            throw wrapException(ex);
        } finally {
            if (response1 != null)
                response1.freeBuffers();
            if (response2 != null)
                response2.freeBuffers();
            if (response3 != null)
                response3.freeBuffers();
        }
    }

    void removeReplica(File file, String headOSDuuid, UserCredentials userCreds) throws IOException {

        RPCResponse<openResponse> response1 = null;
        RPCResponse<xtreemfs_replica_removeResponse> response2 = null;
        RPCResponse response3 = null;
        final String fullPath = fixPath(volumeName + file.getPath());
        final String fixedVol = fixPath(volumeName);
        final String fixedPath = fixPath(file.getPath());
        try {
            response1 = mrcClient.open(null, RPCAuthentication.authNone, userCreds, fixedVol, fixedPath, 0,
                    GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber(), 0, emptyCoordinates);
            FileCredentials oldCreds = response1.get().getCreds();

            xtreemfs_replica_removeRequest request = xtreemfs_replica_removeRequest.newBuilder()
                    .setOsdUuid(headOSDuuid).setFileId(oldCreds.getXcap().getFileId()).build();

            response2 = mrcClient.xtreemfs_replica_remove(null, RPCAuthentication.authNone, userCreds, request);
            xtreemfs_replica_removeResponse removeResponse = response2.get();
            FileCredentials delCap = FileCredentials.newBuilder().setXlocs(removeResponse.getUnlinkXloc())
                    .setXcap(removeResponse.getUnlinkXcap()).build();
            
            waitForXLocSetInstallation(removeResponse.getFileId(), removeResponse.getExpectedXlocsetVersion());

            ServiceUUID osd = new ServiceUUID(headOSDuuid, uuidResolver);

            boolean readOnlyRepl = (oldCreds.getXlocs().getReplicaUpdatePolicy()
                    .equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY));

            FileCredentials newCreds = FileCredentials.newBuilder().setXcap(delCap.getXcap())
                    .setXlocs(oldCreds.getXlocs()).build();
            response3 = osdClient.unlink(osd.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService,
                    newCreds, oldCreds.getXcap().getFileId());
            response3.get();

        } catch (PBRPCException ex) {
            if (ex.getPOSIXErrno() == POSIXErrno.POSIX_ERROR_ENOENT)
                throw new FileNotFoundException("file '" + fullPath + "' does not exist");
            throw wrapException(ex);
        } catch (InterruptedException ex) {
            throw wrapException(ex);
        } finally {
            if (response1 != null)
                response1.freeBuffers();
            if (response2 != null)
                response2.freeBuffers();
            if (response3 != null)
                response3.freeBuffers();
        }
    }

    private XLocSet waitForXLocSetInstallation(String fileId, int expectedVersion) throws IOException {
        // The delay to wait between to pings in seconds.
        int delay_s = 10;

        // The initial call is made without delay.
        XLocSet xLocSet = getXLocSet(fileId);

        // Periodically ping the MRC to request the current XLocSet.
        while (xLocSet.getVersion() < expectedVersion) {
            try {
                Thread.sleep(delay_s * 1000);
            } catch (InterruptedException e) {
                String msg = "Caught interrupt while waiting for the next poll, abort waiting for xLocSet installation.";
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, e, msg);
                }
                throw new IOException(msg);
            }

            xLocSet = getXLocSet(fileId);
        }

        if (xLocSet.getVersion() > expectedVersion) {
            // TODO (jdillmann): Unexpected! Decide what to do
        }

        return xLocSet;
    }

    private XLocSet getXLocSet(String fileId) throws IOException {

        RPCResponse<XLocSet> response = null;
        XLocSet xloc;

        xtreemfs_get_xlocsetRequest.Builder reqBuilder = xtreemfs_get_xlocsetRequest.newBuilder();
        reqBuilder.setFileId(fileId);

        try {
            response = mrcClient.xtreemfs_get_xlocset(null, RPCAuthentication.authNone, userCreds, reqBuilder.build());
            xloc = response.get();
        } catch (PBRPCException ex) {
            throw wrapException(ex);
        } catch (InterruptedException ex) {
            throw wrapException(ex);
        } finally {
            if (response != null)
                response.freeBuffers();
        }

        assert (xloc != null);
        return xloc;
    }

    void setReplicaUpdatePolicy(String path, String policy, UserCredentials userCreds) throws IOException {
        RPCResponse response = null;
        try {
            xtreemfs_set_replica_update_policyRequest msg = xtreemfs_set_replica_update_policyRequest.newBuilder()
                    .setVolumeName(fixPath(volumeName)).setPath(fixPath(path)).setUpdatePolicy(policy).build();
            response = mrcClient.xtreemfs_set_replica_update_policy(null, RPCAuthentication.authNone, userCreds, msg);
            response.get();
        } catch (PBRPCException ex) {
            throw wrapException(ex);
        } catch (InterruptedException ex) {
            throw wrapException(ex);
        } finally {
            if (response != null)
                response.freeBuffers();
        }
    }

    void shutdown() {
        ofl.shutdown();
    }

    /**
     * @return the maxRetries
     */
    public int getMaxRetries() {
        return maxRetries;
    }
    
}
