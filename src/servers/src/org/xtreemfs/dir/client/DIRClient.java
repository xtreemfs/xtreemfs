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
 * AUTHORS: Felix Langner (ZIB)
 */

package org.xtreemfs.dir.client;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.foundation.oncrpc.client.RPCResponseAvailableListener;
import org.xtreemfs.foundation.oncrpc.client.RPCResponseDecoder;
import org.xtreemfs.foundation.oncrpc.utils.XDRUnmarshaller;
import org.xtreemfs.interfaces.AddressMappingSet;
import org.xtreemfs.interfaces.Service;
import org.xtreemfs.interfaces.ServiceSet;
import org.xtreemfs.interfaces.ServiceType;
import org.xtreemfs.interfaces.UserCredentials;
import org.xtreemfs.interfaces.DIRInterface.RedirectException;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_replication_to_masterRequest;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_replication_to_masterResponse;


/**
 * <p>
 * Redirection-layer of the DIRClient. 
 * Requests will be retried for the {@link InetSocketAddress} 
 * returned within a {@link RedirectException} if thrown.
 * </p>
 * 
 * @author flangner
 */
public class DIRClient extends DIRClientBackend {

    public final static int MAX_TRIES = 3;
    
    private final Map<String, Method> methods;
    
    public DIRClient(RPCNIOSocketClient client, InetSocketAddress defaultServer) {
        super(client, defaultServer);
        methods = new HashMap<String, Method>();
        for (Method m : DIRClientBackend.class.getMethods())
            methods.put(m.getName(),m);
    }
    
    @SuppressWarnings("unchecked")
    public RPCResponse<Object> replication_toMaster(InetSocketAddress server, 
            UserCredentials credentials) {
        
        xtreemfs_replication_to_masterRequest rq = new xtreemfs_replication_to_masterRequest();
        RPCResponse r = sendRequest(server, rq.getTag(), rq, 
                new RPCResponseDecoder<Object>() {

            @Override
            public Object getResult(ReusableBuffer data) {
                final xtreemfs_replication_to_masterResponse resp = 
                    new xtreemfs_replication_to_masterResponse();
                resp.unmarshal(new XDRUnmarshaller(data));
                return null;
            }
        }, credentials);
        return r;
    }
    
    @SuppressWarnings("unchecked")
    public RPCResponse<AddressMappingSet> xtreemfs_address_mappings_get(
            InetSocketAddress server, String uuid) {
        return (RPCResponse<AddressMappingSet>) redirectableRequest(server, 
                "_xtreemfs_address_mappings_get", uuid);
    }
    
    @SuppressWarnings("unchecked")
    public RPCResponse<Long> xtreemfs_address_mappings_set(InetSocketAddress 
            server, AddressMappingSet addressMappings) {
        return (RPCResponse<Long>) redirectableRequest(server, 
                "_xtreemfs_address_mappings_set", addressMappings);
    }
    
    @SuppressWarnings("unchecked")
    public RPCResponse xtreemfs_service_deregister(InetSocketAddress server, 
            String uuid) {
        return redirectableRequest(server, "_xtreemfs_service_deregister", uuid);
    }
    
    @SuppressWarnings("unchecked")
    public RPCResponse<ServiceSet> xtreemfs_service_get_by_type(InetSocketAddress 
            server, ServiceType serviceType) {
        return (RPCResponse<ServiceSet>) redirectableRequest(server, 
                "_xtreemfs_service_get_by_type", serviceType);
    }
    
    @SuppressWarnings("unchecked")
    public RPCResponse xtreemfs_service_offline(InetSocketAddress server, 
            String uuid) {
        return redirectableRequest(server, "_xtreemfs_service_offline", uuid);
    }
    
    @SuppressWarnings("unchecked")
    public RPCResponse xtreemfs_address_mappings_remove(InetSocketAddress server, 
            String uuid) {
        return redirectableRequest(server, "_xtreemfs_address_mappings_remove", uuid);
    }
    
    @SuppressWarnings("unchecked")
    public RPCResponse<ServiceSet> xtreemfs_service_get_by_uuid(InetSocketAddress 
            server, String uuid) {
        return (RPCResponse<ServiceSet>) redirectableRequest(server, 
                "_xtreemfs_service_get_by_uuid", uuid);
    }
    
    @SuppressWarnings("unchecked")
    public RPCResponse<Long> xtreemfs_global_time_get(InetSocketAddress server) {
        return (RPCResponse<Long>) redirectableRequest(server, 
                "_xtreemfs_global_time_get");
    }
    
    @SuppressWarnings("unchecked")
    public RPCResponse<ServiceSet> xtreemfs_service_get_by_name(InetSocketAddress 
            server, String volumeName) {
        return (RPCResponse<ServiceSet>) redirectableRequest(server, 
                "_xtreemfs_service_get_by_name", volumeName);
    }
    
    @SuppressWarnings("unchecked")
    public RPCResponse<Long> xtreemfs_service_register(InetSocketAddress server, 
            Service registry) {
        return (RPCResponse<Long>) redirectableRequest(server, 
                "_xtreemfs_service_register", registry);
    }
    
    /**
     * Default behavior of a redirectAble request.
     * Belongs to the DIR replication mechanism. 
     * Maximal count of retries before the client gives up is 3.
     * 
     * @param server
     * @param methodName
     * @param args
     * @return the {@link RPCResponse}.
     */
    @SuppressWarnings("unchecked")
    private RPCResponse<?> redirectableRequest(InetSocketAddress server, 
            String methodName, Object... args) {
        
        Method method = methods.get(methodName);
        assert (method != null);
        
        RPCResponse<?> result = new RPCResponse(new Decoder());
        new Request(this, (server == null), 0, method, result, args).execute(server);

        return result;
    }
    
    /**
     * Decoder for the result response.
     *
     * @since 11/14/2009
     * @author flangner
     * @param <T>
     */
    private final class Decoder<T> implements RPCResponseDecoder<T> {

        private T result;
        
        @Override
        public T getResult(ReusableBuffer data) {
            return result;
        }
        
        void setResult(T result) {
            this.result = result;
        }
    }
    
    
    /**
     * Default listener behavior.
     * 
     * @author flangner
     * @since 11/13/2009
     */
    @SuppressWarnings("unchecked")
    private final class Request implements RPCResponseAvailableListener {
        private final int depth;
        private final Method method;
        private final RPCResponse result;
        private final Object[] args;
        private final DIRClient root;
        private InetSocketAddress address;
        private final boolean update;
        
        /**
         * Default constructor.
         * 
         * @param root - the invoking DIRCLient. 
         * @param update - whether the address should be updated or not.
         * @param depth - the actual retry-depth.
         * @param method - the method to invoke.
         * @param result - the result to replace with the request result.
         * @param args - the arguments for the request.
         */
        Request(DIRClient root, boolean update, int depth, Method method, RPCResponse result, 
                Object... args) {
            
            this.depth = depth;
            this.method = method;
            this.result = result;
            this.args = args;
            this.root = root;
            this.update = update;
        }
        
        /**
         * Starts a request and instantiates a new listener.
         * 
         * @param server - address to run the method at, can be null on depth 0.
         */
        private void execute(InetSocketAddress server) {           
            this.address = server;
                
            try { 
                // method has only the "server" parameter
                if (method.getParameterTypes().length == 1)
                    ((RPCResponse) method.invoke(root, server)).registerListener(
                        this);
                else
                    ((RPCResponse) method.invoke(root, server, args))
                        .registerListener(this);
                
            } catch (Exception e) {
                e.printStackTrace();
                assert (false) : "PROGRAMMERS FAULT! PANIC!!!: " + e.getMessage();
            }
        }
        
        /*
         * (non-Javadoc)
         * @see org.xtreemfs.foundation.oncrpc.client.RPCResponseAvailableListener#responseAvailable(org.xtreemfs.foundation.oncrpc.client.RPCResponse)
         */
        @Override
        public void responseAvailable(RPCResponse r) {
            try {
                Object res = r.get();
                ((Decoder) result.getDecoder()).setResult(res);
                if (update && depth != 0) {
                    assert (address != null);
                    root.updateDefaultServerAddress(address);
                }
            } catch (RedirectException e) {
                if (e.getAddress() != null && depth < MAX_TRIES) {
                    InetSocketAddress newAddress = new InetSocketAddress(
                            e.getAddress(),e.getPort());
                    
                    Request nextLevel = new Request(root, update, (depth+1), 
                            method, result, args);
                    nextLevel.execute(newAddress);
                    return;
                } 
            } catch (Throwable t) { /* ignored */ } 
            result.fill(r);
        }
    }
}
