/*  Copyright (c) 2008-2010 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xtreemfs.babudb.config.ReplicationConfig;
import org.xtreemfs.foundation.TimeServerClient;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.foundation.oncrpc.client.RPCResponseAvailableListener;
import org.xtreemfs.foundation.oncrpc.client.RPCResponseDecoder;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCException;
import org.xtreemfs.interfaces.AddressMappingSet;
import org.xtreemfs.interfaces.Service;
import org.xtreemfs.interfaces.ServiceSet;
import org.xtreemfs.interfaces.ServiceType;
import org.xtreemfs.interfaces.DIRInterface.InvalidArgumentException;
import org.xtreemfs.interfaces.DIRInterface.RedirectException;

/**
 * <p>
 * Redirection-layer of the DIRClient. Requests will be retried for the 
 * {@link InetSocketAddress} returned within a {@link RedirectException} if 
 * thrown.
 * </p>
 * 
 * @author flangner
 */
public class DIRClient extends DIRClientBackend implements TimeServerClient {
    
    private final Map<String, Method> methods;
    
    private final Set<InetSocketAddress> servers;
    
    private final int tries;
    
    public DIRClient(RPCNIOSocketClient client, InetSocketAddress[] servers) {
        super(client, servers[0]);
        this.tries = (servers.length > 3) ? servers.length : 3;
        this.methods = new HashMap<String, Method>();
        for (Method m : DIRClientBackend.class.getMethods())
            methods.put(m.getName(),m);
        
        this.servers = new HashSet<InetSocketAddress>(Arrays.asList(servers));
    }
    
    public DIRClient(RPCNIOSocketClient client, InetSocketAddress defaultServer) {
        super(client, defaultServer);
        this.tries = 3;
        this.methods = new HashMap<String, Method>();
        for (Method m : DIRClientBackend.class.getMethods())
            methods.put(m.getName(),m);
        
        this.servers = null;
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
    
    /*
     * (non-Javadoc)
     * @see org.xtreemfs.common.TimeServerClient#xtreemfs_global_time_get(java.net.InetSocketAddress)
     */
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
    
    @SuppressWarnings("unchecked")
    public RPCResponse<ServiceSet> xtreemfs_service_get_by_type(
            InetSocketAddress server, Object[] args) {
        return (RPCResponse<ServiceSet>) redirectableRequest(server, 
                "_xtreemfs_service_get_by_type", args);
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
        assert (method != null) : "Method '"+methodName+"' not found!";
        
        final RPCResponse<?> result = new RPCResponse(new Decoder());
        new Request(this, servers, (server == null), 0, method, result, args)
            .execute(server);

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
        
        /*
         * (non-Javadoc)
         * @see org.xtreemfs.foundation.oncrpc.client.RPCResponseDecoder#getResult(org.xtreemfs.foundation.buffer.ReusableBuffer)
         */
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
        private List<InetSocketAddress> otherServers = null;
        
        /**
         * Default constructor.
         * 
         * @param root - the invoking DIRCLient. 
         * @param update - whether the address should be updated or not.
         * @param depth - the actual retry-depth.
         * @param method - the method to invoke.
         * @param result - the result to replace with the request result.
         * @param args - the arguments for the request.
         * @param servers - other DIR-servers
         */
        Request(DIRClient root, Set<InetSocketAddress> servers, boolean update, 
                int depth, Method method, RPCResponse result, Object... args) {

            this.depth = depth;
            this.method = method;
            this.result = result;
            this.args = args;
            this.root = root;
            this.update = update;
            if (servers != null) {
                this.otherServers = new LinkedList<InetSocketAddress>(servers);
                this.otherServers.remove(root.getDefaultServerAddress());
            }
        }
        
        private Request(DIRClient root, List<InetSocketAddress> servers, boolean update, 
                int depth, Method method, RPCResponse result, Object... args) {
  
            this.depth = depth;
            this.method = method;
            this.result = result;
            this.args = args;
            this.root = root;
            this.update = update;
            this.otherServers = servers;
        }
        
        /**
         * Starts a request and instantiates a new listener.
         * 
         * @param server - address to run the method at, can be null on depth 0.
         */
        private void execute(InetSocketAddress server) {           
            this.address = server;
            if (server != null && this.otherServers != null) {
                this.otherServers.remove(server);
            } 
                
            try { 
                // method has only the "server" parameter
                if (method.getParameterTypes().length == 1) {
                    ((RPCResponse) method.invoke(root, server))
                        .registerListener(this);
                } else {
                    ((RPCResponse) method.invoke(root, server, args))
                        .registerListener(this);
                }
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
            // got a new address to look at for a running master-server
            } catch (RedirectException e) {
                if (e.getAddress() != null && depth < tries) {
                    InetSocketAddress newAddress = new InetSocketAddress(
                            e.getAddress(),e.getPort());
                    
                    if (!newAddress.equals(this.address)) {
                        this.otherServers.add(this.address);
                        redirect(newAddress);
                    } else {
                        Logging.logMessage(Logging.LEVEL_INFO, this.root, 
                                "'%s' seems to perform a handover at the " +
                                "moment.", this.address.toString());
                        
                        if (this.otherServers != null && 
                            this.otherServers.size() != 0) {
                            reconnect();
                        } else {
                            result.fill(r);
                            return;
                        }
                    }
                    
                    if (r != null) r.freeBuffers();
                    return;
                } else {
                    Logging.logMessage(Logging.LEVEL_ERROR, root, 
                            "Giving up, because the redirect-address was %s " +
                            "and the request was already processed at" +
                            " %d addresses.",
                            e.getAddress(),depth);
                }
            // the server could not process the request, because its invalid!
            } catch (InvalidArgumentException e) {
                // giving up
                Logging.logMessage(Logging.LEVEL_ERROR, root, 
                        "Request could not be processed, because: "+e.getError_message());
                Logging.logError(Logging.LEVEL_DEBUG, this, e);
                
            // an internal server error occurred
            } catch (ONCRPCException e) {
                // giving up
                Logging.logMessage(Logging.LEVEL_ERROR, root, 
                        "Request could not be processed, because: "+e.getMessage());
                Logging.logError(Logging.LEVEL_INFO, this, e);
                
            // could not connect to the given address, try a random one from the
            // servers list, if available
            } catch (Exception e) {
                Logging.logMessage(Logging.LEVEL_WARN, root, 
                        "Could not connect to server '%s', because %s.", (
                                (address != null) ? address : 
                                 root.getDefaultServerAddress()), e.getMessage());
                
                if (otherServers != null && otherServers.size() > 0) {
                    reconnect();
                    
                    if (r != null) r.freeBuffers();
                    return;
                } else {
                    // giving up
                    Logging.logMessage(Logging.LEVEL_ERROR, root, 
                            "Could not connect to the server and no alternative" +
                            " servers are available.");
                    Logging.logError(Logging.LEVEL_DEBUG, this, e);
                }
            } 
            result.fill(r);
        }
        
        private void redirect(InetSocketAddress to) {
            Logging.logMessage(Logging.LEVEL_INFO, root, "redirected to: %s",
                    to.toString());
            
            Request nextLevel = new Request(root, otherServers, update, 
                    (depth+1), method, result, args);
            nextLevel.execute(to);
        }
        
        /**
         * delay the client request to ensure that a new leaseholder
         * for the replicated DIR could be found
         */
        private void reconnect() {
            long delay = (ReplicationConfig.LEASE_TIMEOUT * 2) /
                                    otherServers.size();
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) { /* ignored */ }
            
            InetSocketAddress newAddress = otherServers.get(0);
            Logging.logMessage(Logging.LEVEL_INFO, root, "trying next: %s",
                    newAddress.toString());
            
            Request next = new Request(root, otherServers, true, 1, 
                    method, result, args);
            next.execute(newAddress);
        }
    }
}
