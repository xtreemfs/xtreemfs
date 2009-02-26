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

package org.xtreemfs.new_dir.client;

import java.net.InetSocketAddress;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.foundation.oncrpc.client.ONCRPCClient;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.foundation.oncrpc.client.RPCResponseDecoder;
import org.xtreemfs.interfaces.AddressMappingSet;
import org.xtreemfs.interfaces.DIRInterface.DIRInterface;
import org.xtreemfs.interfaces.DIRInterface.deleteAddressMappingsRequest;
import org.xtreemfs.interfaces.DIRInterface.deleteAddressMappingsResponse;
import org.xtreemfs.interfaces.DIRInterface.deregisterServiceRequest;
import org.xtreemfs.interfaces.DIRInterface.deregisterServiceResponse;
import org.xtreemfs.interfaces.DIRInterface.getAddressMappingsRequest;
import org.xtreemfs.interfaces.DIRInterface.getAddressMappingsResponse;
import org.xtreemfs.interfaces.DIRInterface.getGlobalTimeRequest;
import org.xtreemfs.interfaces.DIRInterface.getGlobalTimeResponse;
import org.xtreemfs.interfaces.DIRInterface.getServiceByUuidRequest;
import org.xtreemfs.interfaces.DIRInterface.getServiceByUuidResponse;
import org.xtreemfs.interfaces.DIRInterface.getServicesByTypeRequest;
import org.xtreemfs.interfaces.DIRInterface.getServicesByTypeResponse;
import org.xtreemfs.interfaces.DIRInterface.registerServiceRequest;
import org.xtreemfs.interfaces.DIRInterface.registerServiceResponse;
import org.xtreemfs.interfaces.DIRInterface.setAddressMappingsRequest;
import org.xtreemfs.interfaces.DIRInterface.setAddressMappingsResponse;
import org.xtreemfs.interfaces.ServiceRegistry;
import org.xtreemfs.interfaces.ServiceRegistrySet;

/**
 *
 * @author bjko
 */
public class DIRClient extends ONCRPCClient {

    public DIRClient(RPCNIOSocketClient client, InetSocketAddress defaultServer) {
        super(client, defaultServer, 1, DIRInterface.getVersion());
    }

    public RPCResponse<Long> getGlobalTime(InetSocketAddress server) {
        getGlobalTimeRequest rq = new getGlobalTimeRequest();
        
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder<Long>() {
            @Override
            public Long getResult(ReusableBuffer data) {
                final getGlobalTimeResponse resp = new getGlobalTimeResponse();
                resp.deserialize(data);
                return resp.getReturnValue();
            }
        });
        return r;
    }

    public RPCResponse<AddressMappingSet> getAddressMapping(InetSocketAddress server, String uuid) {
        getAddressMappingsRequest rq = new getAddressMappingsRequest(uuid);
        
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder<AddressMappingSet>() {

            @Override
            public AddressMappingSet getResult(ReusableBuffer data) {
                final getAddressMappingsResponse resp = new getAddressMappingsResponse();
                resp.deserialize(data);
                return resp.getAddress_mappings();
            }
        });
        return r;
    }

    public RPCResponse<Long> setAddressMapping(InetSocketAddress server, AddressMappingSet addressMappings) {
        setAddressMappingsRequest rq = new setAddressMappingsRequest(addressMappings);
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder<Long>() {

            @Override
            public Long getResult(ReusableBuffer data) {
                setAddressMappingsResponse resp = new setAddressMappingsResponse();
                resp.deserialize(data);
                return resp.getReturnValue();
            }
        });
        return r;
    }

    public RPCResponse deleteAddressMapping(InetSocketAddress server, String uuid) {
        deleteAddressMappingsRequest rq = new deleteAddressMappingsRequest(uuid);
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder() {

            @Override
            public Object getResult(ReusableBuffer data) {
                final deleteAddressMappingsResponse resp = new deleteAddressMappingsResponse();
                resp.deserialize(data);
                return null;
            }
        });
        return r;
    }

    public RPCResponse<Long> registerService(InetSocketAddress server, ServiceRegistry registry) {
        registerServiceRequest rq = new registerServiceRequest(registry);
        
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder<Long>() {

            @Override
            public Long getResult(ReusableBuffer data) {
                final registerServiceResponse resp = new registerServiceResponse();
                resp.deserialize(data);
                return resp.getReturnValue();
            }
        });
        return r;
    }

    public RPCResponse deregisterService(InetSocketAddress server, String uuid) {
        deregisterServiceRequest rq = new deregisterServiceRequest(uuid);
        
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder() {

            @Override
            public Object getResult(ReusableBuffer data) {
                final deregisterServiceResponse resp = new deregisterServiceResponse();
                resp.deserialize(data);
                return null;
            }
        });
        return r;
    }

    public RPCResponse<ServiceRegistrySet> getServicesByType(InetSocketAddress server, int serviceType) {
        getServicesByTypeRequest rq = new getServicesByTypeRequest(serviceType);
        
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder<ServiceRegistrySet>() {

            @Override
            public ServiceRegistrySet getResult(ReusableBuffer data) {
                final getServicesByTypeResponse resp = new getServicesByTypeResponse();
                resp.deserialize(data);
                return resp.getServices();
            }
        });
        return r;
    }

    public RPCResponse<ServiceRegistrySet> getServiceByUuid(InetSocketAddress server, String uuid) {
        getServiceByUuidRequest rq = new getServiceByUuidRequest(uuid);
        
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder<ServiceRegistrySet>() {

            @Override
            public ServiceRegistrySet getResult(ReusableBuffer data) {
                final getServiceByUuidResponse resp = new getServiceByUuidResponse();
                resp.deserialize(data);
                return resp.getServices();
            }
        });
        return r;
    }

}
