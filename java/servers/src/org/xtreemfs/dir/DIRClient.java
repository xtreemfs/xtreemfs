/*
 * Copyright (c) 2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */


package org.xtreemfs.dir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.xtreemfs.common.stage.Callback;
import org.xtreemfs.common.stage.Request;
import org.xtreemfs.common.stage.SimpleStageQueue;
import org.xtreemfs.common.stage.Stage;
import org.xtreemfs.common.stage.StageRequest;
import org.xtreemfs.foundation.TimeServerClient;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.client.PBRPCException;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.client.RPCResponseAvailableListener;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.AuthType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.AddressMapping;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.AddressMappingSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Configuration;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceType;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.addressMappingSetResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.configurationSetResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.globalTimeSGetResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.serviceRegisterResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceClient;

import com.google.protobuf.Message;

/**
 * DIR Client with automatic fail-over and redirect support.
 * 
 * @author bjko
 */
public class DIRClient implements TimeServerClient {

    /**
     * Generated DIR service rpc client.
     */
    private final DIRServiceClient rpcClient;
    
    /**
     * Auth used for TimeServerClient calls.
     */
    private final Auth auth;

    /**
     * UserCredentials used for TimeServerClient calls.
     */
    private final UserCredentials user;
    
    /**
     * Thread that executes calls.
     */
    private final Caller callExecutor;

    /**
     * Initializes the DIRClient.
     * @param rpcClient RPC client (must be running).
     * @param servers list of servers to use (must contain at least one item).
     * @param maxRetries maximum retries before throwing exception.
     * @param retryWaitMs time (milliseconds) to wait between retries.
     */
    public DIRClient(DIRServiceClient rpcClient,
                     InetSocketAddress[] servers,
                     int maxRetries,
                     int retryWaitMs) {
        
        this.rpcClient = rpcClient;
        auth = Auth.newBuilder().setAuthType(AuthType.AUTH_NONE).build();
        user = UserCredentials.newBuilder().setUsername("service").addGroups("xtreemfs").build();
        callExecutor = new Caller(servers, maxRetries, retryWaitMs);
        
        callExecutor.start();
        try {
            callExecutor.waitForStartup();
        } catch (Exception e) {
            /* ignored */
        }
    }

    public AddressMappingSet xtreemfs_address_mappings_get(InetSocketAddress server, Auth authHeader, 
            UserCredentials userCreds, String uuid) throws IOException, InterruptedException {
        
        Future<AddressMappingSet> f = new Future<AddressMappingSet>();
        xtreemfs_address_mappings_get(server, authHeader, userCreds, uuid, f);
        return f.get();
    }
    
    public void xtreemfs_address_mappings_get(InetSocketAddress server, final Auth authHeader, 
            final UserCredentials userCreds, final String uuid, Callback listener) 
            throws IOException, InterruptedException {
        
        callExecutor.call(new CallGenerator<AddressMappingSet>() {
            @Override
            public RPCResponse<AddressMappingSet> executeCall(DIRServiceClient client, InetSocketAddress server) 
                throws IOException {
                
                return client.xtreemfs_address_mappings_get(server, authHeader, userCreds, uuid);
            }
        }, listener);
    }

    public void xtreemfs_address_mappings_remove(InetSocketAddress server, final Auth authHeader, 
            final UserCredentials userCreds, final String uuid) 
            throws IOException, InterruptedException {
        
        Future<?> f = new Future<Message>();
        xtreemfs_address_mappings_remove(server, authHeader, userCreds, uuid, f);
        f.get();
    }
    
    public void xtreemfs_address_mappings_remove(InetSocketAddress server, final Auth authHeader, 
            final UserCredentials userCreds, final String uuid, Callback listener) 
            throws IOException, InterruptedException {
        
        callExecutor.call(new CallGenerator() {
            @Override
            public RPCResponse executeCall(DIRServiceClient client, InetSocketAddress server) throws IOException {
                return client.xtreemfs_address_mappings_remove(server, authHeader, userCreds, uuid);
            }
        }, listener);
    }

    public void xtreemfs_address_mappings_set(InetSocketAddress server, final Auth authHeader, 
            final UserCredentials userCreds, final List<AddressMapping> mappings, Callback listener) 
            throws IOException, InterruptedException {
        
        callExecutor.call(new CallGenerator<addressMappingSetResponse>() {
            @Override
            public RPCResponse<addressMappingSetResponse> executeCall(DIRServiceClient client, InetSocketAddress server) 
                    throws IOException {
                
                return client.xtreemfs_address_mappings_set(server, authHeader, userCreds, mappings);
            }
        }, listener);
    }

    public addressMappingSetResponse xtreemfs_address_mappings_set(InetSocketAddress server, Auth authHeader, 
            UserCredentials userCreds, AddressMappingSet input) throws IOException, InterruptedException {
        
        Future<addressMappingSetResponse> f = new Future<addressMappingSetResponse>();
        xtreemfs_address_mappings_set(server, authHeader, userCreds, input, f);
        return f.get();
    }
    
    public void xtreemfs_address_mappings_set(InetSocketAddress server, final Auth authHeader, 
            final UserCredentials userCreds, final AddressMappingSet input, 
            Callback listener) throws IOException, InterruptedException {
        
        callExecutor.call(new CallGenerator<addressMappingSetResponse>() {
            @Override
            public RPCResponse<addressMappingSetResponse> executeCall(DIRServiceClient client, InetSocketAddress server) 
                    throws IOException {
                
                return client.xtreemfs_address_mappings_set(server, authHeader, userCreds, input);
            }
        }, listener);
    }

    public void xtreemfs_service_deregister(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, 
            String uuid) throws IOException, InterruptedException {
        
        Future<?> f = new Future<Message>();
        xtreemfs_service_deregister(server, authHeader, userCreds, uuid, f);
        f.get();
    }
    
    public void xtreemfs_service_deregister(InetSocketAddress server, final Auth authHeader, 
            final UserCredentials userCreds, final String uuid, Callback listener) 
            throws IOException, InterruptedException {
        
        callExecutor.call(new CallGenerator() {
            @Override
            public RPCResponse<?> executeCall(DIRServiceClient client, InetSocketAddress server) throws IOException {
                return client.xtreemfs_service_deregister(server, authHeader, userCreds, uuid);
            }
        }, listener);
    }

    public void xtreemfs_service_offline(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, 
            String name) throws IOException, InterruptedException {
        
        Future<?> f = new Future<Message>();
        xtreemfs_service_offline(server, authHeader, userCreds, name, f);
        f.get();
    }
    
    public void xtreemfs_service_offline(InetSocketAddress server, final Auth authHeader, final UserCredentials userCreds, 
            final String name, Callback listener) throws IOException, InterruptedException {
        
        callExecutor.call(new CallGenerator() {
            @Override
            public RPCResponse executeCall(DIRServiceClient client, InetSocketAddress server) throws IOException {
                
                return client.xtreemfs_service_offline(server, authHeader, userCreds, name);
            }
        }, listener);
    }

    public ServiceSet xtreemfs_service_get_by_name(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, 
            String name) throws IOException, InterruptedException {
        
        Future<ServiceSet> f = new Future<ServiceSet>();
        xtreemfs_service_get_by_name(server, authHeader, userCreds, name, f);
        return f.get();
    }
    
    public void xtreemfs_service_get_by_name(InetSocketAddress server, final Auth authHeader, 
            final UserCredentials userCreds, final String name, Callback listener) 
            throws IOException, InterruptedException {
        
        callExecutor.call(new CallGenerator<ServiceSet>() {
            @Override
            public RPCResponse<ServiceSet> executeCall(DIRServiceClient client, InetSocketAddress server) 
                    throws IOException {
                
                return client.xtreemfs_service_get_by_name(server, authHeader, userCreds, name);
            }
        }, listener);
    }

    public ServiceSet xtreemfs_service_get_by_uuid(InetSocketAddress server, Auth authHeader, UserCredentials userCreds, 
            String uuid) throws IOException, InterruptedException {
        
        Future<ServiceSet> f = new Future<ServiceSet>();
        xtreemfs_service_get_by_uuid(server, authHeader, userCreds, uuid, f);
        return f.get();
    }
    
    public void xtreemfs_service_get_by_uuid(InetSocketAddress server, final Auth authHeader, 
            final UserCredentials userCreds, final String uuid, Callback listener) 
            throws IOException, InterruptedException {
        
        callExecutor.call(new CallGenerator<ServiceSet>() {
            @Override
            public RPCResponse<ServiceSet> executeCall(DIRServiceClient client, InetSocketAddress server) 
                    throws IOException {
                return client.xtreemfs_service_get_by_uuid(server, authHeader, userCreds, uuid);
            }
        }, listener);
    }

    public ServiceSet xtreemfs_service_get_by_type(InetSocketAddress server, Auth authHeader, 
            UserCredentials userCreds, ServiceType type) 
            throws IOException, InterruptedException {
        
        Future<ServiceSet> f = new Future<ServiceSet>();
        xtreemfs_service_get_by_type(server, authHeader, userCreds, type, f);
        return f.get();
    }
    
    public void xtreemfs_service_get_by_type(InetSocketAddress server, final Auth authHeader, 
            final UserCredentials userCreds, final ServiceType type, Callback listener) 
            throws IOException, InterruptedException {
        
        callExecutor.call(new CallGenerator<ServiceSet>() {
            @Override
            public RPCResponse<ServiceSet> executeCall(DIRServiceClient client, InetSocketAddress server) 
                    throws IOException {
                return client.xtreemfs_service_get_by_type(server, authHeader, userCreds, type);
            }
        }, listener);
    }

    public serviceRegisterResponse xtreemfs_service_register(InetSocketAddress server, Auth authHeader, 
            UserCredentials userCreds, Service service) throws IOException, InterruptedException {
        
        Future<serviceRegisterResponse> f = new Future<serviceRegisterResponse>();
        xtreemfs_service_register(server, authHeader, userCreds, service, f);
        return f.get();
    }
    
    public void xtreemfs_service_register(InetSocketAddress server, final Auth authHeader, 
            final UserCredentials userCreds, final Service service, Callback listener) throws IOException, 
            InterruptedException {
        
        callExecutor.call(new CallGenerator<serviceRegisterResponse>() {
            @Override
            public RPCResponse<serviceRegisterResponse> executeCall(DIRServiceClient client, InetSocketAddress server) throws IOException {
                return client.xtreemfs_service_register(server, authHeader, userCreds, service);
            }
        }, listener);
    }

    public Configuration xtreemfs_configuration_get(InetSocketAddress server, Auth authHeader, 
            UserCredentials userCreds, String uuid) throws IOException, InterruptedException {
        
        Future<Configuration> f = new Future<Configuration>();
        xtreemfs_configuration_get(server, authHeader, userCreds, uuid, f);
        return f.get();
    }
    
    public void xtreemfs_configuration_get(InetSocketAddress server, final Auth authHeader, 
            final UserCredentials userCreds, final String uuid, Callback listener) 
            throws IOException, InterruptedException {
        
        callExecutor.call(new CallGenerator<Configuration>() {
            @Override
            public RPCResponse<Configuration> executeCall(DIRServiceClient client, InetSocketAddress server) 
                    throws IOException {
                return client.xtreemfs_configuration_get(server, authHeader, userCreds, uuid);
            }
        }, listener);
    }

    public configurationSetResponse xtreemfs_configuration_set(InetSocketAddress server, Auth authHeader, 
            UserCredentials userCreds, Configuration config) throws IOException, InterruptedException {
        
        Future<configurationSetResponse> f = new Future<configurationSetResponse>();
        xtreemfs_configuration_set(server, authHeader, userCreds, config, f);
        return f.get();
    }
    
    public void xtreemfs_configuration_set(InetSocketAddress server, final Auth authHeader, 
            final UserCredentials userCreds, final Configuration config, Callback listener) throws IOException, 
            InterruptedException {
        
        callExecutor.call(new CallGenerator<configurationSetResponse>() {
            @Override
            public RPCResponse<configurationSetResponse> executeCall(DIRServiceClient client, InetSocketAddress server) 
                    throws IOException {
                
                return client.xtreemfs_configuration_set(server, authHeader, userCreds, config);
            }
        }, listener);
    }

    @Override
    public long xtreemfs_global_time_get(InetSocketAddress server) {
        
        try {
            return xtreemfs_global_time_get();
        } catch (Exception ex) {
            return 0;
        }
    }
    
    public long xtreemfs_global_time_get() throws InterruptedException, PBRPCException, IOException {
        
        Future<globalTimeSGetResponse> f = new Future<globalTimeSGetResponse>();
        xtreemfs_global_time_get(f);
        return f.get().getTimeInSeconds();
    }

    public void xtreemfs_global_time_get(Callback listener) 
            throws InterruptedException, PBRPCException, IOException {
        
        callExecutor.call(new CallGenerator<globalTimeSGetResponse>() {

            @Override
            public RPCResponse<globalTimeSGetResponse> executeCall(DIRServiceClient client, InetSocketAddress server) 
                    throws IOException {
                return client.xtreemfs_global_time_s_get(server, auth, user);
            }
        }, listener);
    }

    public boolean clientIsAlive() {
        
        return rpcClient.clientIsAlive();
    }

    public void stop() {
        
        try {
            callExecutor.shutdown();
            callExecutor.waitForShutdown();
        } catch (Exception e) {
            /* ignored */
        }
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#finalize()
     */
    @Override
    protected void finalize() throws Throwable {
        stop();
        super.finalize();
    }
    
    /**
     * Interface for syncCall which generates the calls. Will be called for each retry.
     */
    public abstract class CallGenerator<T extends Message> implements Request {
        
        /**
         * True, if the client was redirected before.
         */
        private boolean isRedirect = false;
        
        /**
         * Number of calls already produced by this generator.
         */
        private int numTries = 0;
        
        /**
         * Exception caught within the last generated call.
         */
        private Exception lastException = null;
        
        abstract RPCResponse<T> executeCall(DIRServiceClient client, InetSocketAddress server) throws IOException;
        
    }
    
    /**
     * <p>Futures that await responses synchronously.</p>
     * 
     * @author fx.langner
     * @version 1.00, 09/21/11
     * @param <M>
     */
    private final static class Future<M extends Message> implements Callback {

        private boolean finished = false;
        private M result = null;
        private Throwable error = null;
        
        private synchronized M get() throws InterruptedException, PBRPCException, IOException {
            
            if (!finished) wait();
            
            if (error == null) {
                
                return result;
            } else {
                
                if (error instanceof InterruptedException) {
                    throw (InterruptedException) error;
                } else if (error instanceof PBRPCException) {
                    throw (PBRPCException) error;
                } else if (error instanceof IOException) {
                    throw (IOException) error;
                } else {
                    throw new IOException(error);
                }
            }
        }

        /* (non-Javadoc)
         * @see org.xtreemfs.common.stage.Callback#success(java.lang.Object)
         */
        @SuppressWarnings("unchecked")
        @Override
        public synchronized boolean success(Object result) {
            this.result = (M) result;
            finished = true;
            notify();
            
            return true;
        }

        /* (non-Javadoc)
         * @see org.xtreemfs.common.stage.Callback#failed(java.lang.Exception)
         */
        @Override
        public synchronized void failed(Throwable error) {
            this.error = error;
            finished = true;
            notify();
        }
    }

    /**
     * <p>{@link Stage} that executes DIR client requests.</p>
     * 
     * @author fx.langner
     * @version 1.00, 09/21/11
     */
    private final class Caller extends Stage<CallGenerator> {

        /**
         * List of DIR servers.
         */
        private final InetSocketAddress[] servers;

        /**
         * number of server currently used.
         */
        private final AtomicInteger currentServer;
        /**
         * Maximum number of retries.
         */
        private final int maxRetries;

        /**
         * Time to wait between retries (in milliseconds).
         */
        private final int retryWaitMs;
        
        /**
         * <p>Instantiates a new DIR request Caller {@link Stage} with unlimited queue.</p>
         */
        private Caller(InetSocketAddress[] servers, int maxRetries, int retryWaitMs) {
            super("DIRClientCaller", new SimpleStageQueue<CallGenerator>(1000));
            
            if (servers.length == 0) {
                throw new IllegalArgumentException("Must provide at least one directory service address.");
            }
            this.maxRetries = maxRetries;
            this.servers = servers;
            this.currentServer = new AtomicInteger(0);
            this.retryWaitMs = retryWaitMs;
        }
        
        private void call(final CallGenerator generator, final Callback callback) {
            
            generator.numTries++;
            if (generator.numTries <= maxRetries) {
                
                
                RPCResponse<Message> response = null;
                try {
                    
                    // try to execute the call
                    response = generator.executeCall(rpcClient, servers[currentServer.get()]);
                    response.registerListener(new RPCResponseAvailableListener<Message>() {
                        
                        @Override
                        public void responseAvailable(RPCResponse<Message> r) {
                            
                            try {
                                
                                // finish request
                                callback.success(r.get());
                            } catch (Exception e) {
                                
                                // retry
                                generator.lastException = e;
                                enter(generator, callback);
                            } finally {
                                
                                if (r != null) {
                                    r.freeBuffers();
                                }
                            }
                        }
                    });
                } catch (Exception e) {
                    
                    // retry
                    generator.lastException = e;
                    enter(generator, callback);
                }
            } else {
                
                // failure if no retries are remaining
                callback.failed(new IOException("Request finally failed after "+ (generator.numTries - 1) + 
                        " tries.", generator.lastException));
            }
        }

        /* (non-Javadoc)
         * @see org.xtreemfs.common.stage.Stage#processMethod(org.xtreemfs.common.stage.StageRequest)
         */
        @Override
        public void processMethod(StageRequest<CallGenerator> method) {
            
            final CallGenerator generator = method.getRequest();
            final Callback callback = method.getCallback();
            
            // evaluate the last caught exception
            if (generator.lastException != null && generator.numTries < maxRetries) {
                try {
                    handleException(generator.lastException, generator);
                } catch (Exception e) {
                    
                    callback.failed(e);
                    return;
                }
            }
            
            // execute call
            generator.numTries++;
            if (generator.numTries <= maxRetries) {
                
                
                RPCResponse<Message> response = null;
                try {
                    
                    // try to execute the call
                    response = generator.executeCall(rpcClient, servers[currentServer.get()]);
                    response.registerListener(new RPCResponseAvailableListener<Message>() {
                        
                        @Override
                        public void responseAvailable(RPCResponse<Message> r) {
                            
                            try {
                                
                                // finish request
                                callback.success(r.get());
                            } catch (Exception e) {
                                
                                // retry
                                generator.lastException = e;
                                enter(generator, callback);
                            } finally {
                                
                                if (r != null) {
                                    r.freeBuffers();
                                }
                            }
                        }
                    });
                } catch (Exception e) {
                    
                    // retry
                    generator.lastException = e;
                    enter(generator, callback);
                }
            } else {
                
                // failure if no retries are remaining
                callback.failed(new IOException("Request finally failed after "+ (generator.numTries - 1) + 
                        " tries.", generator.lastException));
            }
        }
        
        /**
         * Method to interpret caught request exceptions.
         * 
         * @param e
         * @param generator
         * @throws Exception
         */
        private void handleException(Exception e, CallGenerator generator) throws Exception {
            
            if (e instanceof PBRPCException) {
                
                PBRPCException ex = (PBRPCException) e;
                
                switch (ex.getErrorType()) {
                    case REDIRECT: {
                        redirect(ex, generator.isRedirect);
                        generator.isRedirect = true;
                        break;
                    }
                    case ERRNO: throw ex;
                    default: {
                        failover(ex);
                        break;
                    }
                }
            } else if (e instanceof IOException) {
                
                IOException ex = (IOException) e;
                Logging.logMessage(Logging.LEVEL_INFO, Category.net, this, "Request failed due to exception: %s", ex);
                generator.lastException = ex;
                failover(ex);
            } else {
                throw e;
            }
        }

        /**
         * Method to redirect requests to another server.
         * 
         * @param exception
         * @param isRedirected
         * @throws InterruptedException
         */
        private void redirect(PBRPCException exception, boolean isRedirected) throws InterruptedException {
            
            assert(exception.getErrorType() == ErrorType.REDIRECT);
            // We "abuse" the UUID field for the address (hostname:port) as the
            // DIR service has no UUIDs.
            String address = exception.getRedirectToServerUUID();
            try {
                
                int colon = address.indexOf(':');
                String hostname = address.substring(0, colon);
                String portStr = address.substring(colon + 1);
                int port = Integer.valueOf(portStr);
                InetSocketAddress redirectTo = new InetSocketAddress(hostname, port);

                // Find the entry in the list.
                for (int i = 0; i < servers.length; i++) {
                    InetSocketAddress server = servers[i];
                    if (server.equals(redirectTo)) {
                        Logging.logMessage(Logging.LEVEL_INFO, Logging.Category.net, this, "redirected to DIR: %s", server);
                        currentServer.set(i);
                        if (isRedirected) {
                            // Wait between redirects, but not for the first redirect.
                            Thread.sleep(retryWaitMs);
                        }
                        return;
                    }
                }
                throw new IOException("Cannot redirect to unknown server: " + redirectTo);
            } catch (InterruptedException ex) {
                
                // Pass on interrupted exception, ignore everything else.
                throw ex;
            } catch (Exception ex) {
                
                Logging.logError(Logging.LEVEL_ERROR, this, ex);
            }
        }

        /**
         * Method to transit to a new server, after the currently chosen server has reported a problem.
         * 
         * @param exception
         * @throws InterruptedException
         */
        private void failover(IOException exception) throws InterruptedException {
            
            int old = currentServer.get();
            Logging.logMessage(Logging.LEVEL_ERROR, Category.net, this, "Request to server %s failed due to exception: %s", 
                    servers[old], exception);
            Thread.sleep(retryWaitMs);
            int newServer = (old >= servers.length) ? 0 : old + 1;
            currentServer.compareAndSet(old, newServer);
            
            Logging.logMessage(Logging.LEVEL_INFO, Category.net, this, "Switching to server %s", servers[newServer]);
        }
    }
}