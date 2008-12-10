/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Björn Kolbeck (ZIB), Jan Stender (ZIB), Christian Lorenz (ZIB)
 */

package org.xtreemfs.common.clients.dir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xtreemfs.common.clients.HttpErrorException;
import org.xtreemfs.common.clients.RPCClient;
import org.xtreemfs.common.clients.RPCResponse;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.pinky.SSLOptions;
import org.xtreemfs.foundation.speedy.MultiSpeedy;

/**
 *
 * @author bjko
 */
public class DIRClient extends RPCClient {

    public static final long        TIMEOUT = 15000;

    private final InetSocketAddress defaultServer;
    
    public static final String HOMEDIR_PREFIX = "user-";

    /** Creates a new instance of DIRClient */
    public DIRClient(MultiSpeedy sharedSpeedy, InetSocketAddress defaultServer) throws IOException {
        super(sharedSpeedy);
        this.defaultServer = defaultServer;
    }

    /** Creates a new instance of DIRClient */
    public DIRClient(MultiSpeedy sharedSpeedy, InetSocketAddress defaultServer, int timeout)
        throws IOException {
        super(sharedSpeedy, timeout);
        this.defaultServer = defaultServer;
    }

    public DIRClient(InetSocketAddress defaultServer, SSLOptions sslOptions, int timeout)
        throws IOException {
        super(timeout, sslOptions);
        this.defaultServer = defaultServer;
    }

    public RPCResponse<Long> registerEntity(InetSocketAddress server, String uuid,
        Map<String, Object> data, long version, String authStr) throws IOException,
        HttpErrorException, JSONException, InterruptedException {

        List<Object> args = new ArrayList(2);
        args.add(uuid);
        args.add(data);
        args.add(version);

        RPCResponse<Long> r = sendRPC(server, "registerEntity", args, authStr, null);
        return r;
    }

    public RPCResponse<Long> registerEntity(String uuid, Map<String, Object> data, long version,
        String authStr) throws IOException, HttpErrorException, JSONException, InterruptedException {
        return registerEntity(defaultServer, uuid, data, version, authStr);
    }

    public RPCResponse<Map<String, Map<String, Object>>> getEntities(InetSocketAddress server,
        Map<String, Object> query, List<String> attrs, String authStr) throws IOException,
        HttpErrorException, JSONException, InterruptedException {

        List<Object> args = new ArrayList(2);
        args.add(query);
        args.add(attrs);

        RPCResponse<Map<String, Map<String, Object>>> r = sendRPC(server, "getEntities", args,
            authStr, null);

        return r;
    }

    public RPCResponse<Map<String, Map<String, Object>>> getEntities(Map<String, Object> query,
        List<String> attrs, String authStr) throws IOException, HttpErrorException, JSONException,
        InterruptedException {
        return getEntities(defaultServer, query, attrs, authStr);
    }

    public RPCResponse deregisterEntity(InetSocketAddress server, String uuid, String authStr)
        throws IOException, HttpErrorException, JSONException, InterruptedException {

        List<Object> args = new ArrayList(1);
        args.add(uuid);

        RPCResponse r = sendRPC(defaultServer, "deregisterEntity", args, authStr, null);

        return r;
    }

    public RPCResponse deregisterEntity(String uuid, String authStr) throws IOException,
        HttpErrorException, JSONException, InterruptedException {
        return deregisterEntity(defaultServer, uuid, authStr);
    }

    public RPCResponse<Long> registerAddressMapping(InetSocketAddress server, String uuid,
        List<Map<String, Object>> mapping, long version, String authStr) throws IOException,
        HttpErrorException, JSONException, InterruptedException {

        List<Object> args = new ArrayList(3);
        args.add(uuid);
        args.add(mapping);
        args.add(version);

        RPCResponse<Long> r = sendRPC(server, "registerAddressMapping", args, authStr, null);
        return r;
    }

    public RPCResponse<Long> registerAddressMapping(String uuid, List<Map<String, Object>> mapping,
        long version, String authStr) throws IOException, HttpErrorException, JSONException,
        InterruptedException {
        return registerAddressMapping(defaultServer, uuid, mapping, version, authStr);
    }

    public RPCResponse<Map<String, List<Map<String, Object>>>> getAddressMapping(
        InetSocketAddress server, String uuid, String authStr) throws IOException,
        HttpErrorException, JSONException, InterruptedException {

        List<Object> args = new ArrayList(1);
        args.add(uuid);

        RPCResponse<Map<String, List<Map<String, Object>>>> r = sendRPC(server,
            "getAddressMapping", args, authStr, null);
        return r;
    }

    public RPCResponse<Map<String, List<Map<String, Object>>>> getAddressMapping(String uuid,
        String authStr) throws IOException, HttpErrorException, JSONException, InterruptedException {
        return getAddressMapping(defaultServer, uuid, authStr);
    }

    public RPCResponse deregisterAddressMapping(InetSocketAddress server, String uuid,
        String authStr) throws IOException, HttpErrorException, JSONException, InterruptedException {

        List<Object> args = new ArrayList(1);
        args.add(uuid);

        RPCResponse r = sendRPC(defaultServer, "deregisterAddressMapping", args, authStr, null);

        return r;
    }

    public RPCResponse deregisterAddressMapping(String uuid, String authStr) throws IOException,
        HttpErrorException, JSONException, InterruptedException {
        return deregisterAddressMapping(defaultServer, uuid, authStr);
    }

    public RPCResponse<Long> getGlobalTime(InetSocketAddress server, String authStr)
        throws IOException, HttpErrorException, JSONException, InterruptedException {

        RPCResponse<Long> r = sendRPC(server, "getGlobalTime", new ArrayList(0), authStr, null);
        return r;
    }

    public RPCResponse<Long> getGlobalTime(String authStr) throws IOException, HttpErrorException,
        JSONException, InterruptedException {

        return getGlobalTime(defaultServer, authStr);
    }
    
    /**
     * Retrieves the volume URL for a user's home volume.
     * @param globalUserId the user's global ID (GUID)
     * @param authStr authentication string for the directory service
     * @return the URL or null if the volume cannot be found
     * @throws java.io.IOException
     * @throws org.xtreemfs.common.clients.HttpErrorException
     * @throws org.xtreemfs.foundation.json.JSONException
     * @throws java.lang.InterruptedException
     */
    public String locateUserHome(String globalUserId, String authStr) throws IOException, HttpErrorException,
        JSONException, InterruptedException {
        
        Map<String,Object> qry = new HashMap();
        qry.put("type","volume");
        qry.put("name",HOMEDIR_PREFIX+globalUserId);
        RPCResponse<Map<String,Map<String,Object>>> r = this.getEntities(qry, null, authStr); 
        Map<String,Map<String,Object>> map = r.get();

        if (map.size() == 0)
            return null;

        String volname = null;
        String mrcuuid = null;
        for (String uuid : map.keySet()) {
            Map<String,Object> data = map.get(uuid);
            mrcuuid = (String) data.get("mrc");
            volname = (String) data.get("name");
            break;
        }
        if (mrcuuid == null)
            return null;
        
        RPCResponse<Map<String,List<Map<String,Object>>>> r2 = this.getAddressMapping(mrcuuid, authStr);
        r2.waitForResponse(2000);
        List<Map<String,Object>> l = r2.get().get(mrcuuid);
        if ((l == null) || (l.size() == 1)) {
            throw new UnknownUUIDException("MRC's uuid "+mrcuuid+" is not registered at directory server");
        }
        List<Map<String,Object>> mappings = (List<Map<String, Object>>) l.get(1);
        for (int i = 0; i < mappings.size(); i++) {
            Map<String,Object> addrMapping = mappings.get(i);
            final String network = (String)addrMapping.get("match_network");
            if (network.equals("*")) {
                final String address = (String)addrMapping.get("address");
                final String protocol = (String)addrMapping.get("protocol");
                final int port = (int) ((Long)addrMapping.get("port")).intValue();
                return protocol+"://"+address+":"+port+"/"+volname;
            }
        }
        return null;
    }
}
