//automatically generated from OSD.proto at Thu Jun 02 16:52:25 CEST 2016
//(c) 2016. See LICENSE file for details.

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

public class OSDServiceClient {

    private RPCNIOSocketClient client;
    private InetSocketAddress  defaultServer;

    public OSDServiceClient(RPCNIOSocketClient client, InetSocketAddress defaultServer) {
        this.client = client;
        this.defaultServer = defaultServer;
    }

    public RPCResponse<OSD.ObjectData> read(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, OSD.readRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<OSD.ObjectData> response = new RPCResponse<OSD.ObjectData>(OSD.ObjectData.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 30001, 10, input, null, response, false);
         return response;
    }

    public RPCResponse<OSD.ObjectData> read(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, GlobalTypes.FileCredentials file_credentials, String file_id, long object_number, long object_version, int offset, int length) throws IOException {
         final OSD.readRequest msg = OSD.readRequest.newBuilder().setFileCredentials(file_credentials).setFileId(file_id).setObjectNumber(object_number).setObjectVersion(object_version).setOffset(offset).setLength(length).build();
         return read(server, authHeader, userCreds,msg);
    }

    public RPCResponse<GlobalTypes.OSDWriteResponse> truncate(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, OSD.truncateRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<GlobalTypes.OSDWriteResponse> response = new RPCResponse<GlobalTypes.OSDWriteResponse>(GlobalTypes.OSDWriteResponse.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 30001, 11, input, null, response, false);
         return response;
    }

    public RPCResponse<GlobalTypes.OSDWriteResponse> truncate(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, GlobalTypes.FileCredentials file_credentials, String file_id, long new_file_size) throws IOException {
         final OSD.truncateRequest msg = OSD.truncateRequest.newBuilder().setFileCredentials(file_credentials).setFileId(file_id).setNewFileSize(new_file_size).build();
         return truncate(server, authHeader, userCreds,msg);
    }

    public RPCResponse unlink(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, OSD.unlink_osd_Request input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse response = new RPCResponse(null);
         client.sendRequest(server, authHeader, userCreds, 30001, 12, input, null, response, false);
         return response;
    }

    public RPCResponse unlink(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, GlobalTypes.FileCredentials file_credentials, String file_id) throws IOException {
         final OSD.unlink_osd_Request msg = OSD.unlink_osd_Request.newBuilder().setFileCredentials(file_credentials).setFileId(file_id).build();
         return unlink(server, authHeader, userCreds,msg);
    }

    public RPCResponse<GlobalTypes.OSDWriteResponse> write(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, OSD.writeRequest input, ReusableBuffer data) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<GlobalTypes.OSDWriteResponse> response = new RPCResponse<GlobalTypes.OSDWriteResponse>(GlobalTypes.OSDWriteResponse.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 30001, 13, input, data, response, false);
         return response;
    }

    public RPCResponse<GlobalTypes.OSDWriteResponse> write(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, GlobalTypes.FileCredentials file_credentials, String file_id, long object_number, long object_version, int offset, long lease_timeout, OSD.ObjectData object_data, ReusableBuffer data) throws IOException {
         final OSD.writeRequest msg = OSD.writeRequest.newBuilder().setFileCredentials(file_credentials).setFileId(file_id).setObjectNumber(object_number).setObjectVersion(object_version).setOffset(offset).setLeaseTimeout(lease_timeout).setObjectData(object_data).build();
         return write(server, authHeader, userCreds,msg, data);
    }

    public RPCResponse xtreemfs_broadcast_gmax(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, OSD.xtreemfs_broadcast_gmaxRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse response = new RPCResponse(null);
         client.sendRequest(server, authHeader, userCreds, 30001, 20, input, null, response, false);
         return response;
    }

    public RPCResponse xtreemfs_broadcast_gmax(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String file_id, long truncate_epoch, long last_object, long file_size) throws IOException {
         final OSD.xtreemfs_broadcast_gmaxRequest msg = OSD.xtreemfs_broadcast_gmaxRequest.newBuilder().setFileId(file_id).setTruncateEpoch(truncate_epoch).setLastObject(last_object).setFileSize(file_size).build();
         return xtreemfs_broadcast_gmax(server, authHeader, userCreds,msg);
    }

    public RPCResponse<OSD.ObjectData> xtreemfs_check_object(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, OSD.xtreemfs_check_objectRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<OSD.ObjectData> response = new RPCResponse<OSD.ObjectData>(OSD.ObjectData.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 30001, 21, input, null, response, false);
         return response;
    }

    public RPCResponse<OSD.ObjectData> xtreemfs_check_object(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, GlobalTypes.FileCredentials file_credentials, String file_id, long object_number, long object_version) throws IOException {
         final OSD.xtreemfs_check_objectRequest msg = OSD.xtreemfs_check_objectRequest.newBuilder().setFileCredentials(file_credentials).setFileId(file_id).setObjectNumber(object_number).setObjectVersion(object_version).build();
         return xtreemfs_check_object(server, authHeader, userCreds,msg);
    }

    public RPCResponse<OSD.xtreemfs_cleanup_get_resultsResponse> xtreemfs_cleanup_get_results(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, Common.emptyRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<OSD.xtreemfs_cleanup_get_resultsResponse> response = new RPCResponse<OSD.xtreemfs_cleanup_get_resultsResponse>(OSD.xtreemfs_cleanup_get_resultsResponse.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 30001, 30, input, null, response, false);
         return response;
    }

    public RPCResponse<OSD.xtreemfs_cleanup_get_resultsResponse> xtreemfs_cleanup_get_results(InetSocketAddress server, Auth authHeader, UserCredentials userCreds) throws IOException {
         
         return xtreemfs_cleanup_get_results(server, authHeader, userCreds,null);
    }

    public RPCResponse<OSD.xtreemfs_cleanup_is_runningResponse> xtreemfs_cleanup_is_running(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, Common.emptyRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<OSD.xtreemfs_cleanup_is_runningResponse> response = new RPCResponse<OSD.xtreemfs_cleanup_is_runningResponse>(OSD.xtreemfs_cleanup_is_runningResponse.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 30001, 31, input, null, response, false);
         return response;
    }

    public RPCResponse<OSD.xtreemfs_cleanup_is_runningResponse> xtreemfs_cleanup_is_running(InetSocketAddress server, Auth authHeader, UserCredentials userCreds) throws IOException {
         
         return xtreemfs_cleanup_is_running(server, authHeader, userCreds,null);
    }

    public RPCResponse xtreemfs_cleanup_start(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, OSD.xtreemfs_cleanup_startRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse response = new RPCResponse(null);
         client.sendRequest(server, authHeader, userCreds, 30001, 32, input, null, response, false);
         return response;
    }

    public RPCResponse xtreemfs_cleanup_start(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, boolean remove_zombies, boolean remove_unavail_volume, boolean lost_and_found, boolean delete_metadata, int metadata_timeout) throws IOException {
         final OSD.xtreemfs_cleanup_startRequest msg = OSD.xtreemfs_cleanup_startRequest.newBuilder().setRemoveZombies(remove_zombies).setRemoveUnavailVolume(remove_unavail_volume).setLostAndFound(lost_and_found).setDeleteMetadata(delete_metadata).setMetadataTimeout(metadata_timeout).build();
         return xtreemfs_cleanup_start(server, authHeader, userCreds,msg);
    }

    public RPCResponse<OSD.xtreemfs_cleanup_statusResponse> xtreemfs_cleanup_status(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, Common.emptyRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<OSD.xtreemfs_cleanup_statusResponse> response = new RPCResponse<OSD.xtreemfs_cleanup_statusResponse>(OSD.xtreemfs_cleanup_statusResponse.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 30001, 33, input, null, response, false);
         return response;
    }

    public RPCResponse<OSD.xtreemfs_cleanup_statusResponse> xtreemfs_cleanup_status(InetSocketAddress server, Auth authHeader, UserCredentials userCreds) throws IOException {
         
         return xtreemfs_cleanup_status(server, authHeader, userCreds,null);
    }

    public RPCResponse xtreemfs_cleanup_stop(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, Common.emptyRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse response = new RPCResponse(null);
         client.sendRequest(server, authHeader, userCreds, 30001, 34, input, null, response, false);
         return response;
    }

    public RPCResponse xtreemfs_cleanup_stop(InetSocketAddress server, Auth authHeader, UserCredentials userCreds) throws IOException {
         
         return xtreemfs_cleanup_stop(server, authHeader, userCreds,null);
    }

    public RPCResponse xtreemfs_cleanup_versions_start(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, Common.emptyRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse response = new RPCResponse(null);
         client.sendRequest(server, authHeader, userCreds, 30001, 35, input, null, response, false);
         return response;
    }

    public RPCResponse xtreemfs_cleanup_versions_start(InetSocketAddress server, Auth authHeader, UserCredentials userCreds) throws IOException {
         
         return xtreemfs_cleanup_versions_start(server, authHeader, userCreds,null);
    }

    public RPCResponse<GlobalTypes.OSDFinalizeVouchersResponse> xtreemfs_finalize_vouchers(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, OSD.xtreemfs_finalize_vouchersRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<GlobalTypes.OSDFinalizeVouchersResponse> response = new RPCResponse<GlobalTypes.OSDFinalizeVouchersResponse>(GlobalTypes.OSDFinalizeVouchersResponse.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 30001, 22, input, null, response, false);
         return response;
    }

    public RPCResponse<GlobalTypes.OSDFinalizeVouchersResponse> xtreemfs_finalize_vouchers(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, GlobalTypes.FileCredentials file_credentials, List<Long> expire_time_ms) throws IOException {
         final OSD.xtreemfs_finalize_vouchersRequest msg = OSD.xtreemfs_finalize_vouchersRequest.newBuilder().setFileCredentials(file_credentials).addAllExpireTimeMs(expire_time_ms).build();
         return xtreemfs_finalize_vouchers(server, authHeader, userCreds,msg);
    }

    public RPCResponse xtreemfs_repair_object(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, OSD.xtreemfs_repair_objectRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse response = new RPCResponse(null);
         client.sendRequest(server, authHeader, userCreds, 30001, 36, input, null, response, false);
         return response;
    }

    public RPCResponse xtreemfs_repair_object(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, GlobalTypes.FileCredentials file_credentials, String file_id, long object_number, long object_version) throws IOException {
         final OSD.xtreemfs_repair_objectRequest msg = OSD.xtreemfs_repair_objectRequest.newBuilder().setFileCredentials(file_credentials).setFileId(file_id).setObjectNumber(object_number).setObjectVersion(object_version).build();
         return xtreemfs_repair_object(server, authHeader, userCreds,msg);
    }

    public RPCResponse<OSD.ObjectData> xtreemfs_rwr_fetch(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, OSD.xtreemfs_rwr_fetchRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<OSD.ObjectData> response = new RPCResponse<OSD.ObjectData>(OSD.ObjectData.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 30001, 73, input, null, response, false);
         return response;
    }

    public RPCResponse<OSD.ObjectData> xtreemfs_rwr_fetch(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, GlobalTypes.FileCredentials file_credentials, String file_id, long object_number, long object_version) throws IOException {
         final OSD.xtreemfs_rwr_fetchRequest msg = OSD.xtreemfs_rwr_fetchRequest.newBuilder().setFileCredentials(file_credentials).setFileId(file_id).setObjectNumber(object_number).setObjectVersion(object_version).build();
         return xtreemfs_rwr_fetch(server, authHeader, userCreds,msg);
    }

    public RPCResponse xtreemfs_rwr_flease_msg(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, OSD.xtreemfs_rwr_flease_msgRequest input, ReusableBuffer data) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse response = new RPCResponse(null);
         client.sendRequest(server, authHeader, userCreds, 30001, 71, input, data, response, false);
         return response;
    }

    public RPCResponse xtreemfs_rwr_flease_msg(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String sender_hostname, int sender_port, ReusableBuffer data) throws IOException {
         final OSD.xtreemfs_rwr_flease_msgRequest msg = OSD.xtreemfs_rwr_flease_msgRequest.newBuilder().setSenderHostname(sender_hostname).setSenderPort(sender_port).build();
         return xtreemfs_rwr_flease_msg(server, authHeader, userCreds,msg, data);
    }

    public RPCResponse xtreemfs_rwr_notify(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, GlobalTypes.FileCredentials input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse response = new RPCResponse(null);
         client.sendRequest(server, authHeader, userCreds, 30001, 75, input, null, response, false);
         return response;
    }

    public RPCResponse xtreemfs_rwr_notify(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, GlobalTypes.XCap xcap, GlobalTypes.XLocSet xlocs) throws IOException {
         final GlobalTypes.FileCredentials msg = GlobalTypes.FileCredentials.newBuilder().setXcap(xcap).setXlocs(xlocs).build();
         return xtreemfs_rwr_notify(server, authHeader, userCreds,msg);
    }

    public RPCResponse<OSD.ObjectData> xtreemfs_rwr_set_primary_epoch(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, OSD.xtreemfs_rwr_set_primary_epochRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<OSD.ObjectData> response = new RPCResponse<OSD.ObjectData>(OSD.ObjectData.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 30001, 78, input, null, response, false);
         return response;
    }

    public RPCResponse<OSD.ObjectData> xtreemfs_rwr_set_primary_epoch(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, GlobalTypes.FileCredentials file_credentials, String file_id, int primary_epoch) throws IOException {
         final OSD.xtreemfs_rwr_set_primary_epochRequest msg = OSD.xtreemfs_rwr_set_primary_epochRequest.newBuilder().setFileCredentials(file_credentials).setFileId(file_id).setPrimaryEpoch(primary_epoch).build();
         return xtreemfs_rwr_set_primary_epoch(server, authHeader, userCreds,msg);
    }

    public RPCResponse<OSD.ReplicaStatus> xtreemfs_rwr_status(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, OSD.xtreemfs_rwr_statusRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<OSD.ReplicaStatus> response = new RPCResponse<OSD.ReplicaStatus>(OSD.ReplicaStatus.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 30001, 76, input, null, response, false);
         return response;
    }

    public RPCResponse<OSD.ReplicaStatus> xtreemfs_rwr_status(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, GlobalTypes.FileCredentials file_credentials, String file_id, long max_local_obj_version) throws IOException {
         final OSD.xtreemfs_rwr_statusRequest msg = OSD.xtreemfs_rwr_statusRequest.newBuilder().setFileCredentials(file_credentials).setFileId(file_id).setMaxLocalObjVersion(max_local_obj_version).build();
         return xtreemfs_rwr_status(server, authHeader, userCreds,msg);
    }

    public RPCResponse xtreemfs_rwr_truncate(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, OSD.xtreemfs_rwr_truncateRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse response = new RPCResponse(null);
         client.sendRequest(server, authHeader, userCreds, 30001, 74, input, null, response, false);
         return response;
    }

    public RPCResponse xtreemfs_rwr_truncate(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, GlobalTypes.FileCredentials file_credentials, String file_id, long new_file_size, long object_version) throws IOException {
         final OSD.xtreemfs_rwr_truncateRequest msg = OSD.xtreemfs_rwr_truncateRequest.newBuilder().setFileCredentials(file_credentials).setFileId(file_id).setNewFileSize(new_file_size).setObjectVersion(object_version).build();
         return xtreemfs_rwr_truncate(server, authHeader, userCreds,msg);
    }

    public RPCResponse xtreemfs_rwr_update(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, OSD.xtreemfs_rwr_updateRequest input, ReusableBuffer data) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse response = new RPCResponse(null);
         client.sendRequest(server, authHeader, userCreds, 30001, 72, input, data, response, false);
         return response;
    }

    public RPCResponse xtreemfs_rwr_update(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, GlobalTypes.FileCredentials file_credentials, String file_id, long new_file_size, long object_number, long object_version, int offset, OSD.ObjectData obj, ReusableBuffer data) throws IOException {
         final OSD.xtreemfs_rwr_updateRequest msg = OSD.xtreemfs_rwr_updateRequest.newBuilder().setFileCredentials(file_credentials).setFileId(file_id).setNewFileSize(new_file_size).setObjectNumber(object_number).setObjectVersion(object_version).setOffset(offset).setObj(obj).build();
         return xtreemfs_rwr_update(server, authHeader, userCreds,msg, data);
    }

    public RPCResponse xtreemfs_rwr_auth_state(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, OSD.xtreemfs_rwr_auth_stateRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse response = new RPCResponse(null);
         client.sendRequest(server, authHeader, userCreds, 30001, 79, input, null, response, false);
         return response;
    }

    public RPCResponse xtreemfs_rwr_auth_state(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, GlobalTypes.FileCredentials file_credentials, String file_id, OSD.AuthoritativeReplicaState state) throws IOException {
         final OSD.xtreemfs_rwr_auth_stateRequest msg = OSD.xtreemfs_rwr_auth_stateRequest.newBuilder().setFileCredentials(file_credentials).setFileId(file_id).setState(state).build();
         return xtreemfs_rwr_auth_state(server, authHeader, userCreds,msg);
    }

    public RPCResponse xtreemfs_rwr_reset_complete(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, OSD.xtreemfs_rwr_reset_completeRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse response = new RPCResponse(null);
         client.sendRequest(server, authHeader, userCreds, 30001, 80, input, null, response, false);
         return response;
    }

    public RPCResponse xtreemfs_rwr_reset_complete(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, GlobalTypes.FileCredentials file_credentials, String file_id, int primary_epoch) throws IOException {
         final OSD.xtreemfs_rwr_reset_completeRequest msg = OSD.xtreemfs_rwr_reset_completeRequest.newBuilder().setFileCredentials(file_credentials).setFileId(file_id).setPrimaryEpoch(primary_epoch).build();
         return xtreemfs_rwr_reset_complete(server, authHeader, userCreds,msg);
    }

    public RPCResponse<OSD.InternalGmax> xtreemfs_internal_get_gmax(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, OSD.xtreemfs_internal_get_gmaxRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<OSD.InternalGmax> response = new RPCResponse<OSD.InternalGmax>(OSD.InternalGmax.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 30001, 40, input, null, response, false);
         return response;
    }

    public RPCResponse<OSD.InternalGmax> xtreemfs_internal_get_gmax(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, GlobalTypes.FileCredentials file_credentials, String file_id) throws IOException {
         final OSD.xtreemfs_internal_get_gmaxRequest msg = OSD.xtreemfs_internal_get_gmaxRequest.newBuilder().setFileCredentials(file_credentials).setFileId(file_id).build();
         return xtreemfs_internal_get_gmax(server, authHeader, userCreds,msg);
    }

    public RPCResponse<GlobalTypes.OSDWriteResponse> xtreemfs_internal_truncate(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, OSD.truncateRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<GlobalTypes.OSDWriteResponse> response = new RPCResponse<GlobalTypes.OSDWriteResponse>(GlobalTypes.OSDWriteResponse.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 30001, 41, input, null, response, false);
         return response;
    }

    public RPCResponse<GlobalTypes.OSDWriteResponse> xtreemfs_internal_truncate(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, GlobalTypes.FileCredentials file_credentials, String file_id, long new_file_size) throws IOException {
         final OSD.truncateRequest msg = OSD.truncateRequest.newBuilder().setFileCredentials(file_credentials).setFileId(file_id).setNewFileSize(new_file_size).build();
         return xtreemfs_internal_truncate(server, authHeader, userCreds,msg);
    }

    public RPCResponse<OSD.xtreemfs_internal_get_file_sizeResponse> xtreemfs_internal_get_file_size(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, OSD.xtreemfs_internal_get_file_sizeRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<OSD.xtreemfs_internal_get_file_sizeResponse> response = new RPCResponse<OSD.xtreemfs_internal_get_file_sizeResponse>(OSD.xtreemfs_internal_get_file_sizeResponse.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 30001, 42, input, null, response, false);
         return response;
    }

    public RPCResponse<OSD.xtreemfs_internal_get_file_sizeResponse> xtreemfs_internal_get_file_size(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, GlobalTypes.FileCredentials file_credentials, String file_id) throws IOException {
         final OSD.xtreemfs_internal_get_file_sizeRequest msg = OSD.xtreemfs_internal_get_file_sizeRequest.newBuilder().setFileCredentials(file_credentials).setFileId(file_id).build();
         return xtreemfs_internal_get_file_size(server, authHeader, userCreds,msg);
    }

    public RPCResponse<OSD.InternalReadLocalResponse> xtreemfs_internal_read_local(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, OSD.xtreemfs_internal_read_localRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<OSD.InternalReadLocalResponse> response = new RPCResponse<OSD.InternalReadLocalResponse>(OSD.InternalReadLocalResponse.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 30001, 43, input, null, response, false);
         return response;
    }

    public RPCResponse<OSD.InternalReadLocalResponse> xtreemfs_internal_read_local(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, GlobalTypes.FileCredentials file_credentials, String file_id, long object_number, long object_version, int offset, int length, boolean attach_object_list, List<OSD.ObjectList> required_objects) throws IOException {
         final OSD.xtreemfs_internal_read_localRequest msg = OSD.xtreemfs_internal_read_localRequest.newBuilder().setFileCredentials(file_credentials).setFileId(file_id).setObjectNumber(object_number).setObjectVersion(object_version).setOffset(offset).setLength(length).setAttachObjectList(attach_object_list).addAllRequiredObjects(required_objects).build();
         return xtreemfs_internal_read_local(server, authHeader, userCreds,msg);
    }

    public RPCResponse<OSD.ObjectList> xtreemfs_internal_get_object_set(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, OSD.xtreemfs_internal_get_object_setRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<OSD.ObjectList> response = new RPCResponse<OSD.ObjectList>(OSD.ObjectList.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 30001, 44, input, null, response, false);
         return response;
    }

    public RPCResponse<OSD.ObjectList> xtreemfs_internal_get_object_set(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, GlobalTypes.FileCredentials file_credentials, String file_id) throws IOException {
         final OSD.xtreemfs_internal_get_object_setRequest msg = OSD.xtreemfs_internal_get_object_setRequest.newBuilder().setFileCredentials(file_credentials).setFileId(file_id).build();
         return xtreemfs_internal_get_object_set(server, authHeader, userCreds,msg);
    }

    public RPCResponse<OSD.xtreemfs_internal_get_fileid_listResponse> xtreemfs_internal_get_fileid_list(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, Common.emptyRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<OSD.xtreemfs_internal_get_fileid_listResponse> response = new RPCResponse<OSD.xtreemfs_internal_get_fileid_listResponse>(OSD.xtreemfs_internal_get_fileid_listResponse.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 30001, 45, input, null, response, false);
         return response;
    }

    public RPCResponse<OSD.xtreemfs_internal_get_fileid_listResponse> xtreemfs_internal_get_fileid_list(InetSocketAddress server, Auth authHeader, UserCredentials userCreds) throws IOException {
         
         return xtreemfs_internal_get_fileid_list(server, authHeader, userCreds,null);
    }

    public RPCResponse<OSD.Lock> xtreemfs_lock_acquire(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, OSD.lockRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<OSD.Lock> response = new RPCResponse<OSD.Lock>(OSD.Lock.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 30001, 50, input, null, response, false);
         return response;
    }

    public RPCResponse<OSD.Lock> xtreemfs_lock_acquire(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, GlobalTypes.FileCredentials file_credentials, OSD.Lock lock_request) throws IOException {
         final OSD.lockRequest msg = OSD.lockRequest.newBuilder().setFileCredentials(file_credentials).setLockRequest(lock_request).build();
         return xtreemfs_lock_acquire(server, authHeader, userCreds,msg);
    }

    public RPCResponse<OSD.Lock> xtreemfs_lock_check(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, OSD.lockRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<OSD.Lock> response = new RPCResponse<OSD.Lock>(OSD.Lock.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 30001, 51, input, null, response, false);
         return response;
    }

    public RPCResponse<OSD.Lock> xtreemfs_lock_check(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, GlobalTypes.FileCredentials file_credentials, OSD.Lock lock_request) throws IOException {
         final OSD.lockRequest msg = OSD.lockRequest.newBuilder().setFileCredentials(file_credentials).setLockRequest(lock_request).build();
         return xtreemfs_lock_check(server, authHeader, userCreds,msg);
    }

    public RPCResponse xtreemfs_lock_release(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, OSD.lockRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse response = new RPCResponse(null);
         client.sendRequest(server, authHeader, userCreds, 30001, 52, input, null, response, false);
         return response;
    }

    public RPCResponse xtreemfs_lock_release(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, GlobalTypes.FileCredentials file_credentials, OSD.Lock lock_request) throws IOException {
         final OSD.lockRequest msg = OSD.lockRequest.newBuilder().setFileCredentials(file_credentials).setLockRequest(lock_request).build();
         return xtreemfs_lock_release(server, authHeader, userCreds,msg);
    }

    public RPCResponse<OSD.xtreemfs_pingMesssage> xtreemfs_ping(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, OSD.xtreemfs_pingMesssage input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<OSD.xtreemfs_pingMesssage> response = new RPCResponse<OSD.xtreemfs_pingMesssage>(OSD.xtreemfs_pingMesssage.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 30001, 60, input, null, response, false);
         return response;
    }

    public RPCResponse<OSD.xtreemfs_pingMesssage> xtreemfs_ping(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, GlobalTypes.VivaldiCoordinates coordinates, boolean request_response) throws IOException {
         final OSD.xtreemfs_pingMesssage msg = OSD.xtreemfs_pingMesssage.newBuilder().setCoordinates(coordinates).setRequestResponse(request_response).build();
         return xtreemfs_ping(server, authHeader, userCreds,msg);
    }

    public RPCResponse xtreemfs_shutdown(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, Common.emptyRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse response = new RPCResponse(null);
         client.sendRequest(server, authHeader, userCreds, 30001, 70, input, null, response, false);
         return response;
    }

    public RPCResponse xtreemfs_shutdown(InetSocketAddress server, Auth authHeader, UserCredentials userCreds) throws IOException {
         
         return xtreemfs_shutdown(server, authHeader, userCreds,null);
    }

    public RPCResponse<OSD.xtreemfs_xloc_set_invalidateResponse> xtreemfs_xloc_set_invalidate(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, OSD.xtreemfs_xloc_set_invalidateRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<OSD.xtreemfs_xloc_set_invalidateResponse> response = new RPCResponse<OSD.xtreemfs_xloc_set_invalidateResponse>(OSD.xtreemfs_xloc_set_invalidateResponse.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 30001, 81, input, null, response, false);
         return response;
    }

    public RPCResponse<OSD.xtreemfs_xloc_set_invalidateResponse> xtreemfs_xloc_set_invalidate(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, GlobalTypes.FileCredentials file_credentials, String file_id) throws IOException {
         final OSD.xtreemfs_xloc_set_invalidateRequest msg = OSD.xtreemfs_xloc_set_invalidateRequest.newBuilder().setFileCredentials(file_credentials).setFileId(file_id).build();
         return xtreemfs_xloc_set_invalidate(server, authHeader, userCreds,msg);
    }

    public RPCResponse<OSD.xtreemfs_rwr_reset_statusResponse> xtreemfs_rwr_auth_state_invalidated(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, OSD.xtreemfs_rwr_auth_stateRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<OSD.xtreemfs_rwr_reset_statusResponse> response = new RPCResponse<OSD.xtreemfs_rwr_reset_statusResponse>(OSD.xtreemfs_rwr_reset_statusResponse.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 30001, 82, input, null, response, false);
         return response;
    }

    public RPCResponse<OSD.xtreemfs_rwr_reset_statusResponse> xtreemfs_rwr_auth_state_invalidated(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, GlobalTypes.FileCredentials file_credentials, String file_id, OSD.AuthoritativeReplicaState state) throws IOException {
         final OSD.xtreemfs_rwr_auth_stateRequest msg = OSD.xtreemfs_rwr_auth_stateRequest.newBuilder().setFileCredentials(file_credentials).setFileId(file_id).setState(state).build();
         return xtreemfs_rwr_auth_state_invalidated(server, authHeader, userCreds,msg);
    }

    public RPCResponse<OSD.xtreemfs_rwr_reset_statusResponse> xtreemfs_rwr_reset_status(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, OSD.xtreemfs_rwr_reset_statusRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<OSD.xtreemfs_rwr_reset_statusResponse> response = new RPCResponse<OSD.xtreemfs_rwr_reset_statusResponse>(OSD.xtreemfs_rwr_reset_statusResponse.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 30001, 83, input, null, response, false);
         return response;
    }

    public RPCResponse<OSD.xtreemfs_rwr_reset_statusResponse> xtreemfs_rwr_reset_status(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, GlobalTypes.FileCredentials file_credentials, String file_id, OSD.AuthoritativeReplicaState state) throws IOException {
         final OSD.xtreemfs_rwr_reset_statusRequest msg = OSD.xtreemfs_rwr_reset_statusRequest.newBuilder().setFileCredentials(file_credentials).setFileId(file_id).setState(state).build();
         return xtreemfs_rwr_reset_status(server, authHeader, userCreds,msg);
    }

    public RPCResponse<OSD.xtreemfs_ec_get_interval_vectorsResponse> xtreemfs_ec_get_interval_vectors(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, OSD.xtreemfs_ec_get_interval_vectorsRequest input) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<OSD.xtreemfs_ec_get_interval_vectorsResponse> response = new RPCResponse<OSD.xtreemfs_ec_get_interval_vectorsResponse>(OSD.xtreemfs_ec_get_interval_vectorsResponse.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 30001, 84, input, null, response, false);
         return response;
    }

    public RPCResponse<OSD.xtreemfs_ec_get_interval_vectorsResponse> xtreemfs_ec_get_interval_vectors(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, GlobalTypes.FileCredentials file_credentials, String file_id) throws IOException {
         final OSD.xtreemfs_ec_get_interval_vectorsRequest msg = OSD.xtreemfs_ec_get_interval_vectorsRequest.newBuilder().setFileCredentials(file_credentials).setFileId(file_id).build();
         return xtreemfs_ec_get_interval_vectors(server, authHeader, userCreds,msg);
    }

    public boolean clientIsAlive() {
        return client.isAlive();
    }
}