/*
 * Copyright (c) 2009-2010, Konrad-Zuse-Zentrum fuer Informationstechnik Berlin
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this 
 * list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, 
 * this list of conditions and the following disclaimer in the documentation 
 * and/or other materials provided with the distribution.
 * Neither the name of the Konrad-Zuse-Zentrum fuer Informationstechnik Berlin 
 * nor the names of its contributors may be used to endorse or promote products 
 * derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */
/*
 * AUTHORS: Bjoern Kolbeck (ZIB)
 */

package org.xtreemfs.foundation.oncrpc.client;

import java.net.InetSocketAddress;

/**
 *
 * @author bjko
 */
public abstract class ONCRPCClient {
    
    /**
     * the rpc client
     */
    private final RPCNIOSocketClient    client;

    /**
     * default server address
     */
    private volatile InetSocketAddress  defaultServer;

    /**
     * fixed program Id
     */
    private final int                   programId;

    /**
     * fixed protocol version number used
     */
    private final int                   versionNumber;

    /**
     * Creates a new ONCRPC client
     * @param client the RPC client to send the rquests with
     * @param defaultServer default server address (can be null)
     * @param programId the program Id
     * @param versionNumber the version number of the interface
     */
    public ONCRPCClient(RPCNIOSocketClient client, InetSocketAddress defaultServer,
            int programId, int versionNumber) {
        if (client == null)
            throw new IllegalArgumentException("A valid client is required, null is not allowed.");
        this.client = client;
        this.defaultServer = defaultServer;
        this.versionNumber = versionNumber;
        this.programId = programId;
    }

    public ONCRPCClient(RPCNIOSocketClient client, int programId, int versionNumber) {
        this(client, null, programId, versionNumber);
    }
    
    @SuppressWarnings("unchecked")
    protected RPCResponse sendRequest(InetSocketAddress server, int procId, yidl.runtime.Object request, RPCResponseDecoder decoder) {
        return sendRequest(server, procId, request, decoder, null);
    }

    protected RPCResponse<?> sendRequest(InetSocketAddress server, int procId, 
            yidl.runtime.Object request, RPCResponseDecoder<?> decoder, 
            yidl.runtime.Object credentials) {
        
        return sendRequest(server, procId, request, decoder, credentials, false);
    }

    @SuppressWarnings("unchecked")
    protected RPCResponse sendRequest(InetSocketAddress server, int procId, yidl.runtime.Object request, RPCResponseDecoder decoder, yidl.runtime.Object credentials,
            boolean highPriority) {
        RPCResponse rpcresp = new RPCResponse(decoder);
        if ((server == null) && (defaultServer == null))
            throw new IllegalArgumentException("must specify a server address if no default server is defined");
        final InetSocketAddress srvAddr = (server == null) ? defaultServer : server;
        client.sendRequest(rpcresp, srvAddr, programId, versionNumber, procId, request,null,credentials,highPriority);
        return rpcresp;
    }

    public boolean clientIsAlive() {
        return client.isAlive();
    }

    public InetSocketAddress getDefaultServerAddress() {
        return this.defaultServer;
    }

    public void updateDefaultServerAddress(InetSocketAddress address) {
        assert (address != null) : "New address must not be null!";
        this.defaultServer = address;
    }
    
    public RPCNIOSocketClient getClient() {
        return this.client;
    }
}
