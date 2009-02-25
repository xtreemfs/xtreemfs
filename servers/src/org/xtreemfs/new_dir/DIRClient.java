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

package org.xtreemfs.new_dir;

import java.net.InetSocketAddress;
import org.xtreemfs.foundation.oncrpc.client.ONCRPCClient;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
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

/**
 *
 * @author bjko
 */
public class DIRClient extends ONCRPCClient {

    public DIRClient(RPCNIOSocketClient client, InetSocketAddress defaultServer) {
        super(client, defaultServer, 1, DIRInterface.getVersion());
    }

    public RPCResponse<getGlobalTimeResponse> getGlobalTime(InetSocketAddress server) {
        getGlobalTimeRequest rq = new getGlobalTimeRequest();
        getGlobalTimeResponse resp = new getGlobalTimeResponse();
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, resp);
        return r;
    }

    public RPCResponse<getAddressMappingsResponse> getAddressMapping(InetSocketAddress server, String uuid) {
        getAddressMappingsRequest rq = new getAddressMappingsRequest(uuid);
        getAddressMappingsResponse resp = new getAddressMappingsResponse();
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, resp);
        return r;
    }

    public RPCResponse<setAddressMappingsResponse> setAddressMapping(InetSocketAddress server, AddressMappingSet addressMappings) {
        setAddressMappingsRequest rq = new setAddressMappingsRequest(addressMappings);
        setAddressMappingsResponse resp = new setAddressMappingsResponse();
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, resp);
        return r;
    }

    public RPCResponse<deleteAddressMappingsResponse> deleteAddressMapping(InetSocketAddress server, String uuid) {
        deleteAddressMappingsRequest rq = new deleteAddressMappingsRequest(uuid);
        deleteAddressMappingsResponse resp = new deleteAddressMappingsResponse();
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, resp);
        return r;
    }

    public RPCResponse<registerServiceResponse> registerService(InetSocketAddress server, ServiceRegistry registry) {
        registerServiceRequest rq = new registerServiceRequest(registry);
        registerServiceResponse resp = new registerServiceResponse();
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, resp);
        return r;
    }

    public RPCResponse<deregisterServiceResponse> deregisterService(InetSocketAddress server, String uuid) {
        deregisterServiceRequest rq = new deregisterServiceRequest(uuid);
        deregisterServiceResponse resp = new deregisterServiceResponse();
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, resp);
        return r;
    }

    public RPCResponse<getServicesByTypeResponse> getServicesByType(InetSocketAddress server, int serviceType) {
        getServicesByTypeRequest rq = new getServicesByTypeRequest(serviceType);
        getServicesByTypeResponse resp = new getServicesByTypeResponse();
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, resp);
        return r;
    }

    public RPCResponse<getServiceByUuidResponse> getServiceByUuid(InetSocketAddress server, String uuid) {
        getServiceByUuidRequest rq = new getServiceByUuidRequest(uuid);
        getServiceByUuidResponse resp = new getServiceByUuidResponse();
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, resp);
        return r;
    }

}
