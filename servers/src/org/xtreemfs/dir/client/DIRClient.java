/*  Copyright (c) 2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

    This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
    Grid Operating System, see <http://www.xtreemos.eu> for more details.
    The XtreemOS project has been developed with the financial support of the
    European Commission's IST program under contract #FP6-033576.

    XtreemFS is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 2 of the License, or (at your option)
    any later version.

    XtreemFS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
*/
/*
 * AUTHORS: Björn Kolbeck (ZIB)
 */

package org.xtreemfs.dir.client;

import java.net.InetSocketAddress;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.foundation.oncrpc.client.ONCRPCClient;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.foundation.oncrpc.client.RPCResponseDecoder;
import org.xtreemfs.interfaces.AddressMappingSet;
import org.xtreemfs.interfaces.DIRInterface.DIRInterface;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_address_mappings_getRequest;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_address_mappings_getResponse;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_address_mappings_removeRequest;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_address_mappings_removeResponse;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_address_mappings_setRequest;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_address_mappings_setResponse;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_global_time_getRequest;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_global_time_getResponse;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_service_deregisterRequest;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_service_deregisterResponse;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_service_get_by_typeRequest;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_service_get_by_typeResponse;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_service_get_by_uuidRequest;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_service_get_by_uuidResponse;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_service_registerRequest;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_service_registerResponse;
import org.xtreemfs.interfaces.Service;
import org.xtreemfs.interfaces.ServiceSet;

/**
 *
 * @author bjko
 */
public class DIRClient extends ONCRPCClient {

    public DIRClient(RPCNIOSocketClient client, InetSocketAddress defaultServer) {
        super(client, defaultServer, 1, DIRInterface.getVersion());
    }

    public RPCResponse<Long> xtreemfs_global_time_get(InetSocketAddress server) {
        xtreemfs_global_time_getRequest rq = new xtreemfs_global_time_getRequest();
        
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder<Long>() {
            @Override
            public Long getResult(ReusableBuffer data) {
                final xtreemfs_global_time_getResponse resp = new xtreemfs_global_time_getResponse();
                resp.deserialize(data);
                return resp.getReturnValue();
            }
        });
        return r;
    }

    public RPCResponse<AddressMappingSet> xtreemfs_address_mappings_get(InetSocketAddress server, String uuid) {
        xtreemfs_address_mappings_getRequest rq = new xtreemfs_address_mappings_getRequest(uuid);
        
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder<AddressMappingSet>() {

            @Override
            public AddressMappingSet getResult(ReusableBuffer data) {
                final xtreemfs_address_mappings_getResponse resp = new xtreemfs_address_mappings_getResponse();
                resp.deserialize(data);
                return resp.getAddress_mappings();
            }
        });
        return r;
    }

    public RPCResponse<Long> xtreemfs_address_mappings_set(InetSocketAddress server, AddressMappingSet addressMappings) {
        xtreemfs_address_mappings_setRequest rq = new xtreemfs_address_mappings_setRequest(addressMappings);
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder<Long>() {

            @Override
            public Long getResult(ReusableBuffer data) {
                xtreemfs_address_mappings_setResponse resp = new xtreemfs_address_mappings_setResponse();
                resp.deserialize(data);
                return resp.getReturnValue();
            }
        });
        return r;
    }

    public RPCResponse xtreemfs_address_mappings_remove(InetSocketAddress server, String uuid) {
        xtreemfs_address_mappings_removeRequest rq = new xtreemfs_address_mappings_removeRequest(uuid);
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder() {

            @Override
            public Object getResult(ReusableBuffer data) {
                final xtreemfs_address_mappings_removeResponse resp = new xtreemfs_address_mappings_removeResponse();
                resp.deserialize(data);
                return null;
            }
        });
        return r;
    }

    public RPCResponse<Long> xtreemfs_service_register(InetSocketAddress server, Service registry) {
        xtreemfs_service_registerRequest rq = new xtreemfs_service_registerRequest(registry);
        
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder<Long>() {

            @Override
            public Long getResult(ReusableBuffer data) {
                final xtreemfs_service_registerResponse resp = new xtreemfs_service_registerResponse();
                resp.deserialize(data);
                return resp.getReturnValue();
            }
        });
        return r;
    }

    public RPCResponse xtreemfs_service_deregister(InetSocketAddress server, String uuid) {
        xtreemfs_service_deregisterRequest rq = new xtreemfs_service_deregisterRequest(uuid);
        
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder() {

            @Override
            public Object getResult(ReusableBuffer data) {
                final xtreemfs_service_deregisterResponse resp = new xtreemfs_service_deregisterResponse();
                resp.deserialize(data);
                return null;
            }
        });
        return r;
    }

    public RPCResponse<ServiceSet> xtreemfs_service_get_by_type(InetSocketAddress server, int serviceType) {
        xtreemfs_service_get_by_typeRequest rq = new xtreemfs_service_get_by_typeRequest(serviceType);
        
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder<ServiceSet>() {

            @Override
            public ServiceSet getResult(ReusableBuffer data) {
                final xtreemfs_service_get_by_typeResponse resp = new xtreemfs_service_get_by_typeResponse();
                resp.deserialize(data);
                return resp.getServices();
            }
        });
        return r;
    }

    public RPCResponse<ServiceSet> xtreemfs_service_get_by_uuid(InetSocketAddress server, String uuid) {
        xtreemfs_service_get_by_uuidRequest rq = new xtreemfs_service_get_by_uuidRequest(uuid);
        
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder<ServiceSet>() {

            @Override
            public ServiceSet getResult(ReusableBuffer data) {
                final xtreemfs_service_get_by_uuidResponse resp = new xtreemfs_service_get_by_uuidResponse();
                resp.deserialize(data);
                return resp.getServices();
            }
        });
        return r;
    }

}
