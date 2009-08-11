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

package org.xtreemfs.foundation.oncrpc.client;

import java.net.InetSocketAddress;
import org.xtreemfs.interfaces.utils.Serializable;
import org.xtreemfs.interfaces.UserCredentials;

/**
 *
 * @author bjko
 */
public abstract class ONCRPCClient {
    
    /**
     * the rpc client
     */
    private final RPCNIOSocketClient client;

    /**
     * default server address
     */
    private InetSocketAddress        defaultServer;

    /**
     * fixed program Id
     */
    private final int programId;

    /**
     * fixed protocol version number used
     */
    private final int versionNumber;

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
    protected RPCResponse sendRequest(InetSocketAddress server, int procId, Serializable request, RPCResponseDecoder decoder) {
        return sendRequest(server, procId, request, decoder, null);
    }

    protected RPCResponse sendRequest(InetSocketAddress server, int procId, Serializable request, RPCResponseDecoder decoder, UserCredentials credentials) {
        RPCResponse rpcresp = new RPCResponse(decoder);
        if ((server == null) && (defaultServer == null))
            throw new IllegalArgumentException("must specify a server address if no default server is defined");
        final InetSocketAddress srvAddr = (server == null) ? defaultServer : server;
        client.sendRequest(rpcresp, srvAddr, programId, versionNumber, procId, request,null,credentials);
        return rpcresp;
    }

    public boolean clientIsAlive() {
        return client.isAlive();
    }

    public InetSocketAddress getDefaultServerAddress() {
        return this.defaultServer;
    }

}
