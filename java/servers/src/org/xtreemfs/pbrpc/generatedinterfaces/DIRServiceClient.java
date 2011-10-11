//automatically generated from DIR.proto at Tue Oct 11 12:07:53 CEST 2011
//(c) 2011. See LICENSE file for details.

package org.xtreemfs.pbrpc.generatedinterfaces;

import java.io.IOException;
import java.util.List;
import java.net.InetSocketAddress;
import com.google.protobuf.Message;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.pbrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;

public class DIRServiceClient {

    private RPCNIOSocketClient client;
    private InetSocketAddress  defaultServer;

    public DIRServiceClient(RPCNIOSocketClient client, InetSocketAddress defaultServer) {
        this.client = client;
        this.defaultServer = defaultServer;
    }

    public RPCResponse<DIR.AddressMappingSet> xtreemfs_address_mappings_get(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, DIR.addressMappingGetRequest input) throws IOException {
         return xtreemfs_address_mappings_get(server, authHeader, userCreds, input, client.getRequestTimeout(), false);
    }

    public RPCResponse<DIR.AddressMappingSet> xtreemfs_address_mappings_get(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String uuid) throws IOException {
         final DIR.addressMappingGetRequest msg = DIR.addressMappingGetRequest.newBuilder().setUuid(uuid).build();
         return xtreemfs_address_mappings_get(server, authHeader, userCreds,msg, client.getRequestTimeout(), false);
    }

    public RPCResponse<DIR.AddressMappingSet> xtreemfs_address_mappings_get(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, DIR.addressMappingGetRequest input, long TTL, boolean serverHighPriority) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<DIR.AddressMappingSet> response = new RPCResponse<DIR.AddressMappingSet>(DIR.AddressMappingSet.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 10001, 1, input, null, response, false, (client.getRequestTimeout() < TTL) ? client.getRequestTimeout() : TTL, serverHighPriority);
         return response;
    }

    public RPCResponse<DIR.AddressMappingSet> xtreemfs_address_mappings_get(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String uuid, long TTL, boolean serverHighPriority) throws IOException {
         final DIR.addressMappingGetRequest msg = DIR.addressMappingGetRequest.newBuilder().setUuid(uuid).build();
         return xtreemfs_address_mappings_get(server, authHeader, userCreds, msg, (client.getRequestTimeout() < TTL) ? client.getRequestTimeout() : TTL, serverHighPriority);
    }

    public RPCResponse xtreemfs_address_mappings_remove(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, DIR.addressMappingGetRequest input) throws IOException {
         return xtreemfs_address_mappings_remove(server, authHeader, userCreds, input, client.getRequestTimeout(), false);
    }

    public RPCResponse xtreemfs_address_mappings_remove(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String uuid) throws IOException {
         final DIR.addressMappingGetRequest msg = DIR.addressMappingGetRequest.newBuilder().setUuid(uuid).build();
         return xtreemfs_address_mappings_remove(server, authHeader, userCreds,msg, client.getRequestTimeout(), false);
    }

    public RPCResponse xtreemfs_address_mappings_remove(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, DIR.addressMappingGetRequest input, long TTL, boolean serverHighPriority) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse response = new RPCResponse(null);
         client.sendRequest(server, authHeader, userCreds, 10001, 2, input, null, response, false, (client.getRequestTimeout() < TTL) ? client.getRequestTimeout() : TTL, serverHighPriority);
         return response;
    }

    public RPCResponse xtreemfs_address_mappings_remove(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String uuid, long TTL, boolean serverHighPriority) throws IOException {
         final DIR.addressMappingGetRequest msg = DIR.addressMappingGetRequest.newBuilder().setUuid(uuid).build();
         return xtreemfs_address_mappings_remove(server, authHeader, userCreds, msg, (client.getRequestTimeout() < TTL) ? client.getRequestTimeout() : TTL, serverHighPriority);
    }

    public RPCResponse<DIR.addressMappingSetResponse> xtreemfs_address_mappings_set(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, DIR.AddressMappingSet input) throws IOException {
         return xtreemfs_address_mappings_set(server, authHeader, userCreds, input, client.getRequestTimeout(), false);
    }

    public RPCResponse<DIR.addressMappingSetResponse> xtreemfs_address_mappings_set(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, List<DIR.AddressMapping> mappings) throws IOException {
         final DIR.AddressMappingSet msg = DIR.AddressMappingSet.newBuilder().addAllMappings(mappings).build();
         return xtreemfs_address_mappings_set(server, authHeader, userCreds,msg, client.getRequestTimeout(), false);
    }

    public RPCResponse<DIR.addressMappingSetResponse> xtreemfs_address_mappings_set(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, DIR.AddressMappingSet input, long TTL, boolean serverHighPriority) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<DIR.addressMappingSetResponse> response = new RPCResponse<DIR.addressMappingSetResponse>(DIR.addressMappingSetResponse.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 10001, 3, input, null, response, false, (client.getRequestTimeout() < TTL) ? client.getRequestTimeout() : TTL, serverHighPriority);
         return response;
    }

    public RPCResponse<DIR.addressMappingSetResponse> xtreemfs_address_mappings_set(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, List<DIR.AddressMapping> mappings, long TTL, boolean serverHighPriority) throws IOException {
         final DIR.AddressMappingSet msg = DIR.AddressMappingSet.newBuilder().addAllMappings(mappings).build();
         return xtreemfs_address_mappings_set(server, authHeader, userCreds, msg, (client.getRequestTimeout() < TTL) ? client.getRequestTimeout() : TTL, serverHighPriority);
    }

    public RPCResponse<DIR.DirService> xtreemfs_discover_dir(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, Common.emptyRequest input) throws IOException {
         return xtreemfs_discover_dir(server, authHeader, userCreds, input, client.getRequestTimeout(), false);
    }

    public RPCResponse<DIR.DirService> xtreemfs_discover_dir(InetSocketAddress server, Auth authHeader, UserCredentials userCreds) throws IOException {
         
         return xtreemfs_discover_dir(server, authHeader, userCreds,null, client.getRequestTimeout(), false);
    }

    public RPCResponse<DIR.DirService> xtreemfs_discover_dir(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, Common.emptyRequest input, long TTL, boolean serverHighPriority) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<DIR.DirService> response = new RPCResponse<DIR.DirService>(DIR.DirService.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 10001, 4, input, null, response, false, (client.getRequestTimeout() < TTL) ? client.getRequestTimeout() : TTL, serverHighPriority);
         return response;
    }

    public RPCResponse<DIR.DirService> xtreemfs_discover_dir(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, long TTL, boolean serverHighPriority) throws IOException {
         
         return xtreemfs_discover_dir(server, authHeader, userCreds, null, (client.getRequestTimeout() < TTL) ? client.getRequestTimeout() : TTL, serverHighPriority);
    }

    public RPCResponse<DIR.globalTimeSGetResponse> xtreemfs_global_time_s_get(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, Common.emptyRequest input) throws IOException {
         return xtreemfs_global_time_s_get(server, authHeader, userCreds, input, client.getRequestTimeout(), false);
    }

    public RPCResponse<DIR.globalTimeSGetResponse> xtreemfs_global_time_s_get(InetSocketAddress server, Auth authHeader, UserCredentials userCreds) throws IOException {
         
         return xtreemfs_global_time_s_get(server, authHeader, userCreds,null, client.getRequestTimeout(), false);
    }

    public RPCResponse<DIR.globalTimeSGetResponse> xtreemfs_global_time_s_get(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, Common.emptyRequest input, long TTL, boolean serverHighPriority) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<DIR.globalTimeSGetResponse> response = new RPCResponse<DIR.globalTimeSGetResponse>(DIR.globalTimeSGetResponse.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 10001, 5, input, null, response, false, (client.getRequestTimeout() < TTL) ? client.getRequestTimeout() : TTL, serverHighPriority);
         return response;
    }

    public RPCResponse<DIR.globalTimeSGetResponse> xtreemfs_global_time_s_get(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, long TTL, boolean serverHighPriority) throws IOException {
         
         return xtreemfs_global_time_s_get(server, authHeader, userCreds, null, (client.getRequestTimeout() < TTL) ? client.getRequestTimeout() : TTL, serverHighPriority);
    }

    public RPCResponse xtreemfs_service_deregister(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, DIR.serviceDeregisterRequest input) throws IOException {
         return xtreemfs_service_deregister(server, authHeader, userCreds, input, client.getRequestTimeout(), false);
    }

    public RPCResponse xtreemfs_service_deregister(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String uuid) throws IOException {
         final DIR.serviceDeregisterRequest msg = DIR.serviceDeregisterRequest.newBuilder().setUuid(uuid).build();
         return xtreemfs_service_deregister(server, authHeader, userCreds,msg, client.getRequestTimeout(), false);
    }

    public RPCResponse xtreemfs_service_deregister(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, DIR.serviceDeregisterRequest input, long TTL, boolean serverHighPriority) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse response = new RPCResponse(null);
         client.sendRequest(server, authHeader, userCreds, 10001, 6, input, null, response, false, (client.getRequestTimeout() < TTL) ? client.getRequestTimeout() : TTL, serverHighPriority);
         return response;
    }

    public RPCResponse xtreemfs_service_deregister(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String uuid, long TTL, boolean serverHighPriority) throws IOException {
         final DIR.serviceDeregisterRequest msg = DIR.serviceDeregisterRequest.newBuilder().setUuid(uuid).build();
         return xtreemfs_service_deregister(server, authHeader, userCreds, msg, (client.getRequestTimeout() < TTL) ? client.getRequestTimeout() : TTL, serverHighPriority);
    }

    public RPCResponse<DIR.ServiceSet> xtreemfs_service_get_by_name(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, DIR.serviceGetByNameRequest input) throws IOException {
         return xtreemfs_service_get_by_name(server, authHeader, userCreds, input, client.getRequestTimeout(), false);
    }

    public RPCResponse<DIR.ServiceSet> xtreemfs_service_get_by_name(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String name) throws IOException {
         final DIR.serviceGetByNameRequest msg = DIR.serviceGetByNameRequest.newBuilder().setName(name).build();
         return xtreemfs_service_get_by_name(server, authHeader, userCreds,msg, client.getRequestTimeout(), false);
    }

    public RPCResponse<DIR.ServiceSet> xtreemfs_service_get_by_name(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, DIR.serviceGetByNameRequest input, long TTL, boolean serverHighPriority) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<DIR.ServiceSet> response = new RPCResponse<DIR.ServiceSet>(DIR.ServiceSet.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 10001, 7, input, null, response, false, (client.getRequestTimeout() < TTL) ? client.getRequestTimeout() : TTL, serverHighPriority);
         return response;
    }

    public RPCResponse<DIR.ServiceSet> xtreemfs_service_get_by_name(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String name, long TTL, boolean serverHighPriority) throws IOException {
         final DIR.serviceGetByNameRequest msg = DIR.serviceGetByNameRequest.newBuilder().setName(name).build();
         return xtreemfs_service_get_by_name(server, authHeader, userCreds, msg, (client.getRequestTimeout() < TTL) ? client.getRequestTimeout() : TTL, serverHighPriority);
    }

    public RPCResponse<DIR.ServiceSet> xtreemfs_service_get_by_type(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, DIR.serviceGetByTypeRequest input) throws IOException {
         return xtreemfs_service_get_by_type(server, authHeader, userCreds, input, client.getRequestTimeout(), false);
    }

    public RPCResponse<DIR.ServiceSet> xtreemfs_service_get_by_type(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, DIR.ServiceType type) throws IOException {
         final DIR.serviceGetByTypeRequest msg = DIR.serviceGetByTypeRequest.newBuilder().setType(type).build();
         return xtreemfs_service_get_by_type(server, authHeader, userCreds,msg, client.getRequestTimeout(), false);
    }

    public RPCResponse<DIR.ServiceSet> xtreemfs_service_get_by_type(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, DIR.serviceGetByTypeRequest input, long TTL, boolean serverHighPriority) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<DIR.ServiceSet> response = new RPCResponse<DIR.ServiceSet>(DIR.ServiceSet.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 10001, 8, input, null, response, false, (client.getRequestTimeout() < TTL) ? client.getRequestTimeout() : TTL, serverHighPriority);
         return response;
    }

    public RPCResponse<DIR.ServiceSet> xtreemfs_service_get_by_type(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, DIR.ServiceType type, long TTL, boolean serverHighPriority) throws IOException {
         final DIR.serviceGetByTypeRequest msg = DIR.serviceGetByTypeRequest.newBuilder().setType(type).build();
         return xtreemfs_service_get_by_type(server, authHeader, userCreds, msg, (client.getRequestTimeout() < TTL) ? client.getRequestTimeout() : TTL, serverHighPriority);
    }

    public RPCResponse<DIR.ServiceSet> xtreemfs_service_get_by_uuid(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, DIR.serviceGetByUUIDRequest input) throws IOException {
         return xtreemfs_service_get_by_uuid(server, authHeader, userCreds, input, client.getRequestTimeout(), false);
    }

    public RPCResponse<DIR.ServiceSet> xtreemfs_service_get_by_uuid(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String name) throws IOException {
         final DIR.serviceGetByUUIDRequest msg = DIR.serviceGetByUUIDRequest.newBuilder().setName(name).build();
         return xtreemfs_service_get_by_uuid(server, authHeader, userCreds,msg, client.getRequestTimeout(), false);
    }

    public RPCResponse<DIR.ServiceSet> xtreemfs_service_get_by_uuid(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, DIR.serviceGetByUUIDRequest input, long TTL, boolean serverHighPriority) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<DIR.ServiceSet> response = new RPCResponse<DIR.ServiceSet>(DIR.ServiceSet.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 10001, 9, input, null, response, false, (client.getRequestTimeout() < TTL) ? client.getRequestTimeout() : TTL, serverHighPriority);
         return response;
    }

    public RPCResponse<DIR.ServiceSet> xtreemfs_service_get_by_uuid(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String name, long TTL, boolean serverHighPriority) throws IOException {
         final DIR.serviceGetByUUIDRequest msg = DIR.serviceGetByUUIDRequest.newBuilder().setName(name).build();
         return xtreemfs_service_get_by_uuid(server, authHeader, userCreds, msg, (client.getRequestTimeout() < TTL) ? client.getRequestTimeout() : TTL, serverHighPriority);
    }

    public RPCResponse xtreemfs_service_offline(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, DIR.serviceGetByUUIDRequest input) throws IOException {
         return xtreemfs_service_offline(server, authHeader, userCreds, input, client.getRequestTimeout(), false);
    }

    public RPCResponse xtreemfs_service_offline(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String name) throws IOException {
         final DIR.serviceGetByUUIDRequest msg = DIR.serviceGetByUUIDRequest.newBuilder().setName(name).build();
         return xtreemfs_service_offline(server, authHeader, userCreds,msg, client.getRequestTimeout(), false);
    }

    public RPCResponse xtreemfs_service_offline(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, DIR.serviceGetByUUIDRequest input, long TTL, boolean serverHighPriority) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse response = new RPCResponse(null);
         client.sendRequest(server, authHeader, userCreds, 10001, 10, input, null, response, false, (client.getRequestTimeout() < TTL) ? client.getRequestTimeout() : TTL, serverHighPriority);
         return response;
    }

    public RPCResponse xtreemfs_service_offline(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String name, long TTL, boolean serverHighPriority) throws IOException {
         final DIR.serviceGetByUUIDRequest msg = DIR.serviceGetByUUIDRequest.newBuilder().setName(name).build();
         return xtreemfs_service_offline(server, authHeader, userCreds, msg, (client.getRequestTimeout() < TTL) ? client.getRequestTimeout() : TTL, serverHighPriority);
    }

    public RPCResponse<DIR.serviceRegisterResponse> xtreemfs_service_register(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, DIR.serviceRegisterRequest input) throws IOException {
         return xtreemfs_service_register(server, authHeader, userCreds, input, client.getRequestTimeout(), false);
    }

    public RPCResponse<DIR.serviceRegisterResponse> xtreemfs_service_register(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, DIR.Service service) throws IOException {
         final DIR.serviceRegisterRequest msg = DIR.serviceRegisterRequest.newBuilder().setService(service).build();
         return xtreemfs_service_register(server, authHeader, userCreds,msg, client.getRequestTimeout(), false);
    }

    public RPCResponse<DIR.serviceRegisterResponse> xtreemfs_service_register(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, DIR.serviceRegisterRequest input, long TTL, boolean serverHighPriority) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<DIR.serviceRegisterResponse> response = new RPCResponse<DIR.serviceRegisterResponse>(DIR.serviceRegisterResponse.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 10001, 11, input, null, response, false, (client.getRequestTimeout() < TTL) ? client.getRequestTimeout() : TTL, serverHighPriority);
         return response;
    }

    public RPCResponse<DIR.serviceRegisterResponse> xtreemfs_service_register(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, DIR.Service service, long TTL, boolean serverHighPriority) throws IOException {
         final DIR.serviceRegisterRequest msg = DIR.serviceRegisterRequest.newBuilder().setService(service).build();
         return xtreemfs_service_register(server, authHeader, userCreds, msg, (client.getRequestTimeout() < TTL) ? client.getRequestTimeout() : TTL, serverHighPriority);
    }

    public RPCResponse xtreemfs_checkpoint(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, Common.emptyRequest input) throws IOException {
         return xtreemfs_checkpoint(server, authHeader, userCreds, input, client.getRequestTimeout(), false);
    }

    public RPCResponse xtreemfs_checkpoint(InetSocketAddress server, Auth authHeader, UserCredentials userCreds) throws IOException {
         
         return xtreemfs_checkpoint(server, authHeader, userCreds,null, client.getRequestTimeout(), false);
    }

    public RPCResponse xtreemfs_checkpoint(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, Common.emptyRequest input, long TTL, boolean serverHighPriority) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse response = new RPCResponse(null);
         client.sendRequest(server, authHeader, userCreds, 10001, 20, input, null, response, false, (client.getRequestTimeout() < TTL) ? client.getRequestTimeout() : TTL, serverHighPriority);
         return response;
    }

    public RPCResponse xtreemfs_checkpoint(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, long TTL, boolean serverHighPriority) throws IOException {
         
         return xtreemfs_checkpoint(server, authHeader, userCreds, null, (client.getRequestTimeout() < TTL) ? client.getRequestTimeout() : TTL, serverHighPriority);
    }

    public RPCResponse xtreemfs_shutdown(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, Common.emptyRequest input) throws IOException {
         return xtreemfs_shutdown(server, authHeader, userCreds, input, client.getRequestTimeout(), false);
    }

    public RPCResponse xtreemfs_shutdown(InetSocketAddress server, Auth authHeader, UserCredentials userCreds) throws IOException {
         
         return xtreemfs_shutdown(server, authHeader, userCreds,null, client.getRequestTimeout(), false);
    }

    public RPCResponse xtreemfs_shutdown(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, Common.emptyRequest input, long TTL, boolean serverHighPriority) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse response = new RPCResponse(null);
         client.sendRequest(server, authHeader, userCreds, 10001, 21, input, null, response, false, (client.getRequestTimeout() < TTL) ? client.getRequestTimeout() : TTL, serverHighPriority);
         return response;
    }

    public RPCResponse xtreemfs_shutdown(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, long TTL, boolean serverHighPriority) throws IOException {
         
         return xtreemfs_shutdown(server, authHeader, userCreds, null, (client.getRequestTimeout() < TTL) ? client.getRequestTimeout() : TTL, serverHighPriority);
    }

    public RPCResponse<DIR.Configuration> xtreemfs_configuration_get(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, DIR.configurationGetRequest input) throws IOException {
         return xtreemfs_configuration_get(server, authHeader, userCreds, input, client.getRequestTimeout(), false);
    }

    public RPCResponse<DIR.Configuration> xtreemfs_configuration_get(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String uuid) throws IOException {
         final DIR.configurationGetRequest msg = DIR.configurationGetRequest.newBuilder().setUuid(uuid).build();
         return xtreemfs_configuration_get(server, authHeader, userCreds,msg, client.getRequestTimeout(), false);
    }

    public RPCResponse<DIR.Configuration> xtreemfs_configuration_get(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, DIR.configurationGetRequest input, long TTL, boolean serverHighPriority) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<DIR.Configuration> response = new RPCResponse<DIR.Configuration>(DIR.Configuration.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 10001, 22, input, null, response, false, (client.getRequestTimeout() < TTL) ? client.getRequestTimeout() : TTL, serverHighPriority);
         return response;
    }

    public RPCResponse<DIR.Configuration> xtreemfs_configuration_get(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String uuid, long TTL, boolean serverHighPriority) throws IOException {
         final DIR.configurationGetRequest msg = DIR.configurationGetRequest.newBuilder().setUuid(uuid).build();
         return xtreemfs_configuration_get(server, authHeader, userCreds, msg, (client.getRequestTimeout() < TTL) ? client.getRequestTimeout() : TTL, serverHighPriority);
    }

    public RPCResponse<DIR.configurationSetResponse> xtreemfs_configuration_set(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, DIR.Configuration input) throws IOException {
         return xtreemfs_configuration_set(server, authHeader, userCreds, input, client.getRequestTimeout(), false);
    }

    public RPCResponse<DIR.configurationSetResponse> xtreemfs_configuration_set(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String uuid, List<GlobalTypes.KeyValuePair> parameter, long version) throws IOException {
         final DIR.Configuration msg = DIR.Configuration.newBuilder().setUuid(uuid).addAllParameter(parameter).setVersion(version).build();
         return xtreemfs_configuration_set(server, authHeader, userCreds,msg, client.getRequestTimeout(), false);
    }

    public RPCResponse<DIR.configurationSetResponse> xtreemfs_configuration_set(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, DIR.Configuration input, long TTL, boolean serverHighPriority) throws IOException {
         if (server == null) server = defaultServer;
         if (server == null) throw new IllegalArgumentException("defaultServer must be set in constructor if you want to pass null as server in calls");
         RPCResponse<DIR.configurationSetResponse> response = new RPCResponse<DIR.configurationSetResponse>(DIR.configurationSetResponse.getDefaultInstance());
         client.sendRequest(server, authHeader, userCreds, 10001, 23, input, null, response, false, (client.getRequestTimeout() < TTL) ? client.getRequestTimeout() : TTL, serverHighPriority);
         return response;
    }

    public RPCResponse<DIR.configurationSetResponse> xtreemfs_configuration_set(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, String uuid, List<GlobalTypes.KeyValuePair> parameter, long version, long TTL, boolean serverHighPriority) throws IOException {
         final DIR.Configuration msg = DIR.Configuration.newBuilder().setUuid(uuid).addAllParameter(parameter).setVersion(version).build();
         return xtreemfs_configuration_set(server, authHeader, userCreds, msg, (client.getRequestTimeout() < TTL) ? client.getRequestTimeout() : TTL, serverHighPriority);
    }

    public boolean clientIsAlive() {
        return client.isAlive();
    }
}