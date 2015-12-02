//automatically generated from MRC.proto at Wed Dec 02 14:27:50 CET 2015
//(c) 2015. See LICENSE file for details.

package org.xtreemfs.pbrpc.generatedinterfaces;

import java.io.IOException;
import java.util.List;
import java.net.InetSocketAddress;
import com.google.protobuf.Message;
import com.google.protobuf.ByteString;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.pbrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;

public class MRCServiceClient {

    private RPCNIOSocketClient client;
    private InetSocketAddress  defaultServer;

    public MRCServiceClient(RPCNIOSocketClient client, InetSocketAddress defaultServer) {
        this.client = client;
        this.defaultServer = defaultServer;
    }

    public RPCResponse fsetattr(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, MRC.fsetattrRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse response = new RPCResponse(null);
         client.sendRequest(server, authHeader, userCreds, 20001, 2, input, null, response, false);
         return response;
    }

    public RPCResponse fsetattr(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, MRC.Stat stbuf, int to_set, GlobalTypes.XCap cap) throws IOException {
         final MRC.fsetattrRequest msg = MRC.fsetattrRequest.newBuilder().setStbuf(stbuf).setToSet(to_set).setCap(cap).build();
         return fsetattr(server, authHeader, userCreds,msg);
    }

    public RPCResponse<GlobalTypes.XCap> ftruncate(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, GlobalTypes.XCap input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<GlobalTypes.XCap> response = new RPCResponse<GlobalTypes.XCap>(GlobalTypes.XCap.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 20001, 3, input, null, response, false);
         return response;
    }

    public RPCResponse<GlobalTypes.XCap> ftruncate(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, int access_mode, String client_identity, long expire_time_s, int expire_timeout_s, String file_id, boolean replicate_on_close, String server_signature, int truncate_epoch, GlobalTypes.SnapConfig snap_config, long snap_timestamp, long voucher_size, long expire_time_ms, GlobalTypes.TraceConfig trace_config) throws IOException {
         final GlobalTypes.XCap msg = GlobalTypes.XCap.newBuilder().setAccessMode(access_mode).setClientIdentity(client_identity).setExpireTimeS(expire_time_s).setExpireTimeoutS(expire_timeout_s).setFileId(file_id).setReplicateOnClose(replicate_on_close).setServerSignature(server_signature).setTruncateEpoch(truncate_epoch).setSnapConfig(snap_config).setSnapTimestamp(snap_timestamp).setVoucherSize(voucher_size).setExpireTimeMs(expire_time_ms).setTraceConfig(trace_config).build();
         return ftruncate(server, authHeader, userCreds,msg);
    }

    public RPCResponse<MRC.getattrResponse> getattr(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, MRC.getattrRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<MRC.getattrResponse> response = new RPCResponse<MRC.getattrResponse>(MRC.getattrResponse.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 20001, 4, input, null, response, false);
         return response;
    }

    public RPCResponse<MRC.getattrResponse> getattr(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String volume_name, String path, long known_etag) throws IOException {
         final MRC.getattrRequest msg = MRC.getattrRequest.newBuilder().setVolumeName(volume_name).setPath(path).setKnownEtag(known_etag).build();
         return getattr(server, authHeader, userCreds,msg);
    }

    public RPCResponse<MRC.getxattrResponse> getxattr(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, MRC.getxattrRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<MRC.getxattrResponse> response = new RPCResponse<MRC.getxattrResponse>(MRC.getxattrResponse.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 20001, 5, input, null, response, false);
         return response;
    }

    public RPCResponse<MRC.getxattrResponse> getxattr(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String volume_name, String path, String name) throws IOException {
         final MRC.getxattrRequest msg = MRC.getxattrRequest.newBuilder().setVolumeName(volume_name).setPath(path).setName(name).build();
         return getxattr(server, authHeader, userCreds,msg);
    }

    public RPCResponse<MRC.timestampResponse> link(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, MRC.linkRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<MRC.timestampResponse> response = new RPCResponse<MRC.timestampResponse>(MRC.timestampResponse.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 20001, 6, input, null, response, false);
         return response;
    }

    public RPCResponse<MRC.timestampResponse> link(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String volume_name, String target_path, String link_path) throws IOException {
         final MRC.linkRequest msg = MRC.linkRequest.newBuilder().setVolumeName(volume_name).setTargetPath(target_path).setLinkPath(link_path).build();
         return link(server, authHeader, userCreds,msg);
    }

    public RPCResponse<MRC.listxattrResponse> listxattr(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, MRC.listxattrRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<MRC.listxattrResponse> response = new RPCResponse<MRC.listxattrResponse>(MRC.listxattrResponse.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 20001, 7, input, null, response, false);
         return response;
    }

    public RPCResponse<MRC.listxattrResponse> listxattr(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String volume_name, String path, boolean names_only) throws IOException {
         final MRC.listxattrRequest msg = MRC.listxattrRequest.newBuilder().setVolumeName(volume_name).setPath(path).setNamesOnly(names_only).build();
         return listxattr(server, authHeader, userCreds,msg);
    }

    public RPCResponse<MRC.timestampResponse> mkdir(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, MRC.mkdirRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<MRC.timestampResponse> response = new RPCResponse<MRC.timestampResponse>(MRC.timestampResponse.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 20001, 8, input, null, response, false);
         return response;
    }

    public RPCResponse<MRC.timestampResponse> mkdir(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String volume_name, String path, int mode) throws IOException {
         final MRC.mkdirRequest msg = MRC.mkdirRequest.newBuilder().setVolumeName(volume_name).setPath(path).setMode(mode).build();
         return mkdir(server, authHeader, userCreds,msg);
    }

    public RPCResponse<MRC.openResponse> open(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, MRC.openRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<MRC.openResponse> response = new RPCResponse<MRC.openResponse>(MRC.openResponse.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 20001, 9, input, null, response, false);
         return response;
    }

    public RPCResponse<MRC.openResponse> open(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String volume_name, String path, int flags, int mode, int attributes, GlobalTypes.VivaldiCoordinates coordinates) throws IOException {
         final MRC.openRequest msg = MRC.openRequest.newBuilder().setVolumeName(volume_name).setPath(path).setFlags(flags).setMode(mode).setAttributes(attributes).setCoordinates(coordinates).build();
         return open(server, authHeader, userCreds,msg);
    }

    public RPCResponse<MRC.DirectoryEntries> readdir(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, MRC.readdirRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<MRC.DirectoryEntries> response = new RPCResponse<MRC.DirectoryEntries>(MRC.DirectoryEntries.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 20001, 10, input, null, response, false);
         return response;
    }

    public RPCResponse<MRC.DirectoryEntries> readdir(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String volume_name, String path, long known_etag, int limit_directory_entries_count, boolean names_only, long seen_directory_entries_count) throws IOException {
         final MRC.readdirRequest msg = MRC.readdirRequest.newBuilder().setVolumeName(volume_name).setPath(path).setKnownEtag(known_etag).setLimitDirectoryEntriesCount(limit_directory_entries_count).setNamesOnly(names_only).setSeenDirectoryEntriesCount(seen_directory_entries_count).build();
         return readdir(server, authHeader, userCreds,msg);
    }

    public RPCResponse<MRC.readlinkResponse> readlink(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, MRC.readlinkRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<MRC.readlinkResponse> response = new RPCResponse<MRC.readlinkResponse>(MRC.readlinkResponse.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 20001, 11, input, null, response, false);
         return response;
    }

    public RPCResponse<MRC.readlinkResponse> readlink(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String volume_name, String path) throws IOException {
         final MRC.readlinkRequest msg = MRC.readlinkRequest.newBuilder().setVolumeName(volume_name).setPath(path).build();
         return readlink(server, authHeader, userCreds,msg);
    }

    public RPCResponse<MRC.timestampResponse> removexattr(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, MRC.removexattrRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<MRC.timestampResponse> response = new RPCResponse<MRC.timestampResponse>(MRC.timestampResponse.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 20001, 12, input, null, response, false);
         return response;
    }

    public RPCResponse<MRC.timestampResponse> removexattr(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String volume_name, String path, String name) throws IOException {
         final MRC.removexattrRequest msg = MRC.removexattrRequest.newBuilder().setVolumeName(volume_name).setPath(path).setName(name).build();
         return removexattr(server, authHeader, userCreds,msg);
    }

    public RPCResponse<MRC.renameResponse> rename(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, MRC.renameRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<MRC.renameResponse> response = new RPCResponse<MRC.renameResponse>(MRC.renameResponse.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 20001, 13, input, null, response, false);
         return response;
    }

    public RPCResponse<MRC.renameResponse> rename(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String volume_name, String source_path, String target_path) throws IOException {
         final MRC.renameRequest msg = MRC.renameRequest.newBuilder().setVolumeName(volume_name).setSourcePath(source_path).setTargetPath(target_path).build();
         return rename(server, authHeader, userCreds,msg);
    }

    public RPCResponse<MRC.timestampResponse> rmdir(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, MRC.rmdirRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<MRC.timestampResponse> response = new RPCResponse<MRC.timestampResponse>(MRC.timestampResponse.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 20001, 14, input, null, response, false);
         return response;
    }

    public RPCResponse<MRC.timestampResponse> rmdir(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String volume_name, String path) throws IOException {
         final MRC.rmdirRequest msg = MRC.rmdirRequest.newBuilder().setVolumeName(volume_name).setPath(path).build();
         return rmdir(server, authHeader, userCreds,msg);
    }

    public RPCResponse<MRC.timestampResponse> setattr(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, MRC.setattrRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<MRC.timestampResponse> response = new RPCResponse<MRC.timestampResponse>(MRC.timestampResponse.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 20001, 15, input, null, response, false);
         return response;
    }

    public RPCResponse<MRC.timestampResponse> setattr(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String volume_name, String path, MRC.Stat stbuf, int to_set) throws IOException {
         final MRC.setattrRequest msg = MRC.setattrRequest.newBuilder().setVolumeName(volume_name).setPath(path).setStbuf(stbuf).setToSet(to_set).build();
         return setattr(server, authHeader, userCreds,msg);
    }

    public RPCResponse<MRC.timestampResponse> setxattr(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, MRC.setxattrRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<MRC.timestampResponse> response = new RPCResponse<MRC.timestampResponse>(MRC.timestampResponse.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 20001, 16, input, null, response, false);
         return response;
    }

    public RPCResponse<MRC.timestampResponse> setxattr(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String volume_name, String path, String name, String value, ByteString value_bytes_string, int flags) throws IOException {
         final MRC.setxattrRequest msg = MRC.setxattrRequest.newBuilder().setVolumeName(volume_name).setPath(path).setName(name).setValue(value).setValueBytesString(value_bytes_string).setFlags(flags).build();
         return setxattr(server, authHeader, userCreds,msg);
    }

    public RPCResponse<MRC.StatVFS> statvfs(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, MRC.statvfsRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<MRC.StatVFS> response = new RPCResponse<MRC.StatVFS>(MRC.StatVFS.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 20001, 17, input, null, response, false);
         return response;
    }

    public RPCResponse<MRC.StatVFS> statvfs(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String volume_name, long known_etag) throws IOException {
         final MRC.statvfsRequest msg = MRC.statvfsRequest.newBuilder().setVolumeName(volume_name).setKnownEtag(known_etag).build();
         return statvfs(server, authHeader, userCreds,msg);
    }

    public RPCResponse<MRC.timestampResponse> symlink(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, MRC.symlinkRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<MRC.timestampResponse> response = new RPCResponse<MRC.timestampResponse>(MRC.timestampResponse.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 20001, 18, input, null, response, false);
         return response;
    }

    public RPCResponse<MRC.timestampResponse> symlink(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String volume_name, String target_path, String link_path) throws IOException {
         final MRC.symlinkRequest msg = MRC.symlinkRequest.newBuilder().setVolumeName(volume_name).setTargetPath(target_path).setLinkPath(link_path).build();
         return symlink(server, authHeader, userCreds,msg);
    }

    public RPCResponse<MRC.unlinkResponse> unlink(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, MRC.unlinkRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<MRC.unlinkResponse> response = new RPCResponse<MRC.unlinkResponse>(MRC.unlinkResponse.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 20001, 19, input, null, response, false);
         return response;
    }

    public RPCResponse<MRC.unlinkResponse> unlink(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String volume_name, String path) throws IOException {
         final MRC.unlinkRequest msg = MRC.unlinkRequest.newBuilder().setVolumeName(volume_name).setPath(path).build();
         return unlink(server, authHeader, userCreds,msg);
    }

    public RPCResponse access(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, MRC.accessRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse response = new RPCResponse(null);
         client.sendRequest(server, authHeader, userCreds, 20001, 20, input, null, response, false);
         return response;
    }

    public RPCResponse access(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String volume_name, String path, int flags) throws IOException {
         final MRC.accessRequest msg = MRC.accessRequest.newBuilder().setVolumeName(volume_name).setPath(path).setFlags(flags).build();
         return access(server, authHeader, userCreds,msg);
    }

    public RPCResponse xtreemfs_checkpoint(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, Common.emptyRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse response = new RPCResponse(null);
         client.sendRequest(server, authHeader, userCreds, 20001, 30, input, null, response, false);
         return response;
    }

    public RPCResponse xtreemfs_checkpoint(InetSocketAddress server, Auth authHeader, UserCredentials userCreds) throws IOException {
         
         return xtreemfs_checkpoint(server, authHeader, userCreds,null);
    }

    public RPCResponse<MRC.xtreemfs_check_file_existsResponse> xtreemfs_check_file_exists(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, MRC.xtreemfs_check_file_existsRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<MRC.xtreemfs_check_file_existsResponse> response = new RPCResponse<MRC.xtreemfs_check_file_existsResponse>(MRC.xtreemfs_check_file_existsResponse.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 20001, 31, input, null, response, false);
         return response;
    }

    public RPCResponse<MRC.xtreemfs_check_file_existsResponse> xtreemfs_check_file_exists(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String volume_id, List<String> file_ids, String osd_uuid) throws IOException {
         final MRC.xtreemfs_check_file_existsRequest msg = MRC.xtreemfs_check_file_existsRequest.newBuilder().setVolumeId(volume_id).addAllFileIds(file_ids).setOsdUuid(osd_uuid).build();
         return xtreemfs_check_file_exists(server, authHeader, userCreds,msg);
    }

    public RPCResponse xtreemfs_clear_vouchers(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, MRC.xtreemfs_clear_vouchersRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse response = new RPCResponse(null);
         client.sendRequest(server, authHeader, userCreds, 20001, 52, input, null, response, false);
         return response;
    }

    public RPCResponse xtreemfs_clear_vouchers(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, GlobalTypes.FileCredentials creds, List<GlobalTypes.OSDFinalizeVouchersResponse> osd_finalize_vouchers_response, List<Long> expire_time_ms) throws IOException {
         final MRC.xtreemfs_clear_vouchersRequest msg = MRC.xtreemfs_clear_vouchersRequest.newBuilder().setCreds(creds).addAllOsdFinalizeVouchersResponse(osd_finalize_vouchers_response).addAllExpireTimeMs(expire_time_ms).build();
         return xtreemfs_clear_vouchers(server, authHeader, userCreds,msg);
    }

    public RPCResponse xtreemfs_dump_database(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, MRC.xtreemfs_dump_restore_databaseRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse response = new RPCResponse(null);
         client.sendRequest(server, authHeader, userCreds, 20001, 32, input, null, response, false);
         return response;
    }

    public RPCResponse xtreemfs_dump_database(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String dump_file) throws IOException {
         final MRC.xtreemfs_dump_restore_databaseRequest msg = MRC.xtreemfs_dump_restore_databaseRequest.newBuilder().setDumpFile(dump_file).build();
         return xtreemfs_dump_database(server, authHeader, userCreds,msg);
    }

    public RPCResponse<MRC.xtreemfs_get_suitable_osdsResponse> xtreemfs_get_suitable_osds(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, MRC.xtreemfs_get_suitable_osdsRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<MRC.xtreemfs_get_suitable_osdsResponse> response = new RPCResponse<MRC.xtreemfs_get_suitable_osdsResponse>(MRC.xtreemfs_get_suitable_osdsResponse.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 20001, 33, input, null, response, false);
         return response;
    }

    public RPCResponse<MRC.xtreemfs_get_suitable_osdsResponse> xtreemfs_get_suitable_osds(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String file_id, String path, String volume_name, int num_osds) throws IOException {
         final MRC.xtreemfs_get_suitable_osdsRequest msg = MRC.xtreemfs_get_suitable_osdsRequest.newBuilder().setFileId(file_id).setPath(path).setVolumeName(volume_name).setNumOsds(num_osds).build();
         return xtreemfs_get_suitable_osds(server, authHeader, userCreds,msg);
    }

    public RPCResponse<MRC.stringMessage> xtreemfs_internal_debug(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, MRC.stringMessage input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<MRC.stringMessage> response = new RPCResponse<MRC.stringMessage>(MRC.stringMessage.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 20001, 34, input, null, response, false);
         return response;
    }

    public RPCResponse<MRC.stringMessage> xtreemfs_internal_debug(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String a_string) throws IOException {
         final MRC.stringMessage msg = MRC.stringMessage.newBuilder().setAString(a_string).build();
         return xtreemfs_internal_debug(server, authHeader, userCreds,msg);
    }

    public RPCResponse<MRC.xtreemfs_listdirResponse> xtreemfs_listdir(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, MRC.xtreemfs_listdirRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<MRC.xtreemfs_listdirResponse> response = new RPCResponse<MRC.xtreemfs_listdirResponse>(MRC.xtreemfs_listdirResponse.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 20001, 35, input, null, response, false);
         return response;
    }

    public RPCResponse<MRC.xtreemfs_listdirResponse> xtreemfs_listdir(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String path) throws IOException {
         final MRC.xtreemfs_listdirRequest msg = MRC.xtreemfs_listdirRequest.newBuilder().setPath(path).build();
         return xtreemfs_listdir(server, authHeader, userCreds,msg);
    }

    public RPCResponse<MRC.Volumes> xtreemfs_lsvol(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, Common.emptyRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<MRC.Volumes> response = new RPCResponse<MRC.Volumes>(MRC.Volumes.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 20001, 36, input, null, response, false);
         return response;
    }

    public RPCResponse<MRC.Volumes> xtreemfs_lsvol(InetSocketAddress server, Auth authHeader, UserCredentials userCreds) throws IOException {
         
         return xtreemfs_lsvol(server, authHeader, userCreds,null);
    }

    public RPCResponse xtreemfs_mkvol(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, MRC.Volume input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse response = new RPCResponse(null);
         client.sendRequest(server, authHeader, userCreds, 20001, 47, input, null, response, false);
         return response;
    }

    public RPCResponse xtreemfs_mkvol(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, GlobalTypes.AccessControlPolicyType access_control_policy, GlobalTypes.StripingPolicy default_striping_policy, String id, int mode, String name, String owner_group_id, String owner_user_id, List<GlobalTypes.KeyValuePair> attrs, long quota) throws IOException {
         final MRC.Volume msg = MRC.Volume.newBuilder().setAccessControlPolicy(access_control_policy).setDefaultStripingPolicy(default_striping_policy).setId(id).setMode(mode).setName(name).setOwnerGroupId(owner_group_id).setOwnerUserId(owner_user_id).addAllAttrs(attrs).setQuota(quota).build();
         return xtreemfs_mkvol(server, authHeader, userCreds,msg);
    }

    public RPCResponse<GlobalTypes.XCap> xtreemfs_renew_capability(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, GlobalTypes.XCap input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<GlobalTypes.XCap> response = new RPCResponse<GlobalTypes.XCap>(GlobalTypes.XCap.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 20001, 37, input, null, response, false);
         return response;
    }

    public RPCResponse<GlobalTypes.XCap> xtreemfs_renew_capability(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, int access_mode, String client_identity, long expire_time_s, int expire_timeout_s, String file_id, boolean replicate_on_close, String server_signature, int truncate_epoch, GlobalTypes.SnapConfig snap_config, long snap_timestamp, long voucher_size, long expire_time_ms, GlobalTypes.TraceConfig trace_config) throws IOException {
         final GlobalTypes.XCap msg = GlobalTypes.XCap.newBuilder().setAccessMode(access_mode).setClientIdentity(client_identity).setExpireTimeS(expire_time_s).setExpireTimeoutS(expire_timeout_s).setFileId(file_id).setReplicateOnClose(replicate_on_close).setServerSignature(server_signature).setTruncateEpoch(truncate_epoch).setSnapConfig(snap_config).setSnapTimestamp(snap_timestamp).setVoucherSize(voucher_size).setExpireTimeMs(expire_time_ms).setTraceConfig(trace_config).build();
         return xtreemfs_renew_capability(server, authHeader, userCreds,msg);
    }

    public RPCResponse<GlobalTypes.XCap> xtreemfs_renew_capability_and_voucher(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, MRC.xtreemfs_renew_capabilityRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<GlobalTypes.XCap> response = new RPCResponse<GlobalTypes.XCap>(GlobalTypes.XCap.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 20001, 53, input, null, response, false);
         return response;
    }

    public RPCResponse<GlobalTypes.XCap> xtreemfs_renew_capability_and_voucher(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, GlobalTypes.XCap xcap, boolean increaseVoucher) throws IOException {
         final MRC.xtreemfs_renew_capabilityRequest msg = MRC.xtreemfs_renew_capabilityRequest.newBuilder().setXcap(xcap).setIncreaseVoucher(increaseVoucher).build();
         return xtreemfs_renew_capability_and_voucher(server, authHeader, userCreds,msg);
    }

    public RPCResponse xtreemfs_replication_to_master(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, Common.emptyRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse response = new RPCResponse(null);
         client.sendRequest(server, authHeader, userCreds, 20001, 38, input, null, response, false);
         return response;
    }

    public RPCResponse xtreemfs_replication_to_master(InetSocketAddress server, Auth authHeader, UserCredentials userCreds) throws IOException {
         
         return xtreemfs_replication_to_master(server, authHeader, userCreds,null);
    }

    public RPCResponse<MRC.xtreemfs_replica_addResponse> xtreemfs_replica_add(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, MRC.xtreemfs_replica_addRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<MRC.xtreemfs_replica_addResponse> response = new RPCResponse<MRC.xtreemfs_replica_addResponse>(MRC.xtreemfs_replica_addResponse.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 20001, 39, input, null, response, false);
         return response;
    }

    public RPCResponse<MRC.xtreemfs_replica_addResponse> xtreemfs_replica_add(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String file_id, String path, String volume_name, GlobalTypes.Replica new_replica) throws IOException {
         final MRC.xtreemfs_replica_addRequest msg = MRC.xtreemfs_replica_addRequest.newBuilder().setFileId(file_id).setPath(path).setVolumeName(volume_name).setNewReplica(new_replica).build();
         return xtreemfs_replica_add(server, authHeader, userCreds,msg);
    }

    public RPCResponse<GlobalTypes.Replicas> xtreemfs_replica_list(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, MRC.xtreemfs_replica_listRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<GlobalTypes.Replicas> response = new RPCResponse<GlobalTypes.Replicas>(GlobalTypes.Replicas.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 20001, 40, input, null, response, false);
         return response;
    }

    public RPCResponse<GlobalTypes.Replicas> xtreemfs_replica_list(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String file_id, String path, String volume_name) throws IOException {
         final MRC.xtreemfs_replica_listRequest msg = MRC.xtreemfs_replica_listRequest.newBuilder().setFileId(file_id).setPath(path).setVolumeName(volume_name).build();
         return xtreemfs_replica_list(server, authHeader, userCreds,msg);
    }

    public RPCResponse<MRC.xtreemfs_replica_removeResponse> xtreemfs_replica_remove(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, MRC.xtreemfs_replica_removeRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<MRC.xtreemfs_replica_removeResponse> response = new RPCResponse<MRC.xtreemfs_replica_removeResponse>(MRC.xtreemfs_replica_removeResponse.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 20001, 41, input, null, response, false);
         return response;
    }

    public RPCResponse<MRC.xtreemfs_replica_removeResponse> xtreemfs_replica_remove(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String file_id, String path, String volume_name, String osd_uuid) throws IOException {
         final MRC.xtreemfs_replica_removeRequest msg = MRC.xtreemfs_replica_removeRequest.newBuilder().setFileId(file_id).setPath(path).setVolumeName(volume_name).setOsdUuid(osd_uuid).build();
         return xtreemfs_replica_remove(server, authHeader, userCreds,msg);
    }

    public RPCResponse xtreemfs_restore_database(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, MRC.xtreemfs_dump_restore_databaseRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse response = new RPCResponse(null);
         client.sendRequest(server, authHeader, userCreds, 20001, 42, input, null, response, false);
         return response;
    }

    public RPCResponse xtreemfs_restore_database(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String dump_file) throws IOException {
         final MRC.xtreemfs_dump_restore_databaseRequest msg = MRC.xtreemfs_dump_restore_databaseRequest.newBuilder().setDumpFile(dump_file).build();
         return xtreemfs_restore_database(server, authHeader, userCreds,msg);
    }

    public RPCResponse xtreemfs_restore_file(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, MRC.xtreemfs_restore_fileRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse response = new RPCResponse(null);
         client.sendRequest(server, authHeader, userCreds, 20001, 43, input, null, response, false);
         return response;
    }

    public RPCResponse xtreemfs_restore_file(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String file_path, String file_id, long file_size, String osd_uuid, int stripe_size) throws IOException {
         final MRC.xtreemfs_restore_fileRequest msg = MRC.xtreemfs_restore_fileRequest.newBuilder().setFilePath(file_path).setFileId(file_id).setFileSize(file_size).setOsdUuid(osd_uuid).setStripeSize(stripe_size).build();
         return xtreemfs_restore_file(server, authHeader, userCreds,msg);
    }

    public RPCResponse xtreemfs_rmvol(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, MRC.xtreemfs_rmvolRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse response = new RPCResponse(null);
         client.sendRequest(server, authHeader, userCreds, 20001, 44, input, null, response, false);
         return response;
    }

    public RPCResponse xtreemfs_rmvol(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String volume_name) throws IOException {
         final MRC.xtreemfs_rmvolRequest msg = MRC.xtreemfs_rmvolRequest.newBuilder().setVolumeName(volume_name).build();
         return xtreemfs_rmvol(server, authHeader, userCreds,msg);
    }

    public RPCResponse xtreemfs_shutdown(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, Common.emptyRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse response = new RPCResponse(null);
         client.sendRequest(server, authHeader, userCreds, 20001, 45, input, null, response, false);
         return response;
    }

    public RPCResponse xtreemfs_shutdown(InetSocketAddress server, Auth authHeader, UserCredentials userCreds) throws IOException {
         
         return xtreemfs_shutdown(server, authHeader, userCreds,null);
    }

    public RPCResponse<MRC.timestampResponse> xtreemfs_update_file_size(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, MRC.xtreemfs_update_file_sizeRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<MRC.timestampResponse> response = new RPCResponse<MRC.timestampResponse>(MRC.timestampResponse.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 20001, 46, input, null, response, false);
         return response;
    }

    public RPCResponse<MRC.timestampResponse> xtreemfs_update_file_size(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, GlobalTypes.XCap xcap, GlobalTypes.OSDWriteResponse osd_write_response, boolean close_file, GlobalTypes.VivaldiCoordinates coordinates) throws IOException {
         final MRC.xtreemfs_update_file_sizeRequest msg = MRC.xtreemfs_update_file_sizeRequest.newBuilder().setXcap(xcap).setOsdWriteResponse(osd_write_response).setCloseFile(close_file).setCoordinates(coordinates).build();
         return xtreemfs_update_file_size(server, authHeader, userCreds,msg);
    }

    public RPCResponse xtreemfs_set_replica_update_policy(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, MRC.xtreemfs_set_replica_update_policyRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse response = new RPCResponse(null);
         client.sendRequest(server, authHeader, userCreds, 20001, 48, input, null, response, false);
         return response;
    }

    public RPCResponse xtreemfs_set_replica_update_policy(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String file_id, String path, String volume_name, String update_policy) throws IOException {
         final MRC.xtreemfs_set_replica_update_policyRequest msg = MRC.xtreemfs_set_replica_update_policyRequest.newBuilder().setFileId(file_id).setPath(path).setVolumeName(volume_name).setUpdatePolicy(update_policy).build();
         return xtreemfs_set_replica_update_policy(server, authHeader, userCreds,msg);
    }

    public RPCResponse<MRC.xtreemfs_set_read_only_xattrResponse> xtreemfs_set_read_only_xattr(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, MRC.xtreemfs_set_read_only_xattrRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<MRC.xtreemfs_set_read_only_xattrResponse> response = new RPCResponse<MRC.xtreemfs_set_read_only_xattrResponse>(MRC.xtreemfs_set_read_only_xattrResponse.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 20001, 49, input, null, response, false);
         return response;
    }

    public RPCResponse<MRC.xtreemfs_set_read_only_xattrResponse> xtreemfs_set_read_only_xattr(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String file_id, boolean value) throws IOException {
         final MRC.xtreemfs_set_read_only_xattrRequest msg = MRC.xtreemfs_set_read_only_xattrRequest.newBuilder().setFileId(file_id).setValue(value).build();
         return xtreemfs_set_read_only_xattr(server, authHeader, userCreds,msg);
    }

    public RPCResponse<GlobalTypes.FileCredentials> xtreemfs_get_file_credentials(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, MRC.xtreemfs_get_file_credentialsRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<GlobalTypes.FileCredentials> response = new RPCResponse<GlobalTypes.FileCredentials>(GlobalTypes.FileCredentials.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 20001, 50, input, null, response, false);
         return response;
    }

    public RPCResponse<GlobalTypes.FileCredentials> xtreemfs_get_file_credentials(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String file_id) throws IOException {
         final MRC.xtreemfs_get_file_credentialsRequest msg = MRC.xtreemfs_get_file_credentialsRequest.newBuilder().setFileId(file_id).build();
         return xtreemfs_get_file_credentials(server, authHeader, userCreds,msg);
    }

    public RPCResponse<GlobalTypes.XLocSet> xtreemfs_get_xlocset(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, MRC.xtreemfs_get_xlocsetRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<GlobalTypes.XLocSet> response = new RPCResponse<GlobalTypes.XLocSet>(GlobalTypes.XLocSet.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 20001, 51, input, null, response, false);
         return response;
    }

    public RPCResponse<GlobalTypes.XLocSet> xtreemfs_get_xlocset(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String file_id, String path, String volume_name, GlobalTypes.XCap xcap, GlobalTypes.VivaldiCoordinates coordinates) throws IOException {
         final MRC.xtreemfs_get_xlocsetRequest msg = MRC.xtreemfs_get_xlocsetRequest.newBuilder().setFileId(file_id).setPath(path).setVolumeName(volume_name).setXcap(xcap).setCoordinates(coordinates).build();
         return xtreemfs_get_xlocset(server, authHeader, userCreds,msg);
    }

    public RPCResponse<MRC.xtreemfs_reselect_osdsResponse> xtreemfs_reselect_osds(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, MRC.xtreemfs_reselect_osdsRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<MRC.xtreemfs_reselect_osdsResponse> response = new RPCResponse<MRC.xtreemfs_reselect_osdsResponse>(MRC.xtreemfs_reselect_osdsResponse.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 20001, 54, input, null, response, false);
         return response;
    }

    public RPCResponse<MRC.xtreemfs_reselect_osdsResponse> xtreemfs_reselect_osds(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String path, String volume_name, GlobalTypes.VivaldiCoordinates coordinates) throws IOException {
         final MRC.xtreemfs_reselect_osdsRequest msg = MRC.xtreemfs_reselect_osdsRequest.newBuilder().setPath(path).setVolumeName(volume_name).setCoordinates(coordinates).build();
         return xtreemfs_reselect_osds(server, authHeader, userCreds,msg);
    }

    public boolean clientIsAlive() {
        return client.isAlive();
    }
}