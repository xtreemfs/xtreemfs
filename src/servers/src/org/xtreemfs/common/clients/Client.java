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

package org.xtreemfs.common.clients;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.dir.client.DIRClient;
import org.xtreemfs.dir.client.DIRInterfaceExceptionParser;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.foundation.oncrpc.client.RemoteExceptionParser;
import org.xtreemfs.interfaces.AccessControlPolicyType;
import org.xtreemfs.interfaces.Service;
import org.xtreemfs.interfaces.ServiceSet;
import org.xtreemfs.interfaces.ServiceType;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.StripingPolicy;
import org.xtreemfs.interfaces.UserCredentials;
import org.xtreemfs.interfaces.utils.ONCRPCException;
import org.xtreemfs.mrc.client.MRCClient;
import org.xtreemfs.mrc.client.MRCInterfaceExceptionParser;
import org.xtreemfs.osd.client.OSDClient;
import org.xtreemfs.osd.client.OSDInterfaceExceptionParser;

/**
 *
 * @author bjko
 */
public class Client {

    private final RPCNIOSocketClient mdClient, osdClient;

    private final InetSocketAddress[]  dirAddress;

    private DIRClient dirClient;

    private final UUIDResolver         uuidRes;
    
    private Map<String, Volume>       volumeMap;

    public Client(InetSocketAddress[] dirAddresses, int requestTimeout, int connectionTimeout, SSLOptions ssl) throws IOException {
        this.dirAddress = dirAddresses;
        RemoteExceptionParser[] rexp = getExceptionParsers();
        mdClient = new RPCNIOSocketClient(ssl, requestTimeout, connectionTimeout, rexp);
        osdClient = new RPCNIOSocketClient(ssl, requestTimeout, connectionTimeout, rexp);
        dirClient = new DIRClient(mdClient, dirAddress[0]);
        TimeSync.initializeLocal(0,50);
        uuidRes = UUIDResolver.startNonSingelton(dirClient, 3600, 1000);
        volumeMap = new HashMap<String, Volume>();
    }

    public static RemoteExceptionParser[] getExceptionParsers() {
        return new RemoteExceptionParser[]{new DIRInterfaceExceptionParser(),new OSDInterfaceExceptionParser(),new MRCInterfaceExceptionParser()};
    }

    public Volume getVolume(String volumeName, UserCredentials credentials) throws IOException {
        RPCResponse<ServiceSet> r = null;
        try {
            r = dirClient.xtreemfs_service_get_by_name(null, volumeName);
            final ServiceSet s = r.get();
            if (s.size() == 0) {
                throw new IOException("volume '"+volumeName+"' does not exist");
            }
            final Service vol = s.get(0);
            final String mrcUUIDstr = vol.getData().get("mrc");
            final ServiceUUID mrc = new ServiceUUID(mrcUUIDstr, uuidRes);

            UserCredentials uc = credentials;
            if (uc == null) {
                StringSet grps = new StringSet();
                grps.add("test");
                uc = new UserCredentials("test", grps, "");
            }

            Logging.logMessage(Logging.LEVEL_DEBUG, this,"volume %s on MRC %s/%s",volumeName,mrcUUIDstr,mrc.getAddress());

            Volume v = volumeMap.get(volumeName);
            if (v == null) {
                v = new Volume(new OSDClient(osdClient), new MRCClient(mdClient, mrc.getAddress()),
                    volumeName, uuidRes, uc);
                volumeMap.put(volumeName, v);
            }
            return v;
            
        } catch (ONCRPCException ex) {
            throw new IOException("communication failure", ex);
        } catch (InterruptedException ex) {
            throw new IOException("operation was interrupted", ex);
        } finally {
            if (r != null)
                r.freeBuffers();
        }
    }

    public void createVolume(String volumeName, UserCredentials credentials, StripingPolicy sp, int accessCtrlPolicy, int permissions) throws IOException {
        RPCResponse<ServiceSet> r = null;
        RPCResponse r2 = null;
        try {
            r = dirClient._xtreemfs_service_get_by_type(null, new Object[]{ServiceType.SERVICE_TYPE_MRC});
            ServiceSet mrcs = r.get();

            if (mrcs.size() == 0) {
                throw new IOException("no MRC available for volume creation");
            }

            String uuid = mrcs.get(0).getUuid();
            ServiceUUID mrcUUID = new ServiceUUID(uuid, uuidRes);

            MRCClient m = new MRCClient(mdClient, mrcUUID.getAddress());
            r2 = m.mkvol(null, credentials, volumeName, sp, accessCtrlPolicy, permissions);
            r2.get();

        } catch (ONCRPCException ex) {
            throw new IOException("communication failure", ex);
        } catch (InterruptedException ex) {
            throw new IOException("operation was interrupted", ex);
        } finally {
            if (r != null)
                r.freeBuffers();
            if (r2 != null)
                r2.freeBuffers();
        }
    }

    public ServiceSet getRegistry() throws IOException {
        RPCResponse<ServiceSet> r = null;
        try {
            r = dirClient.xtreemfs_service_get_by_type(null, new Object[]{ServiceType.SERVICE_TYPE_MIXED});
            return r.get();
        } catch (ONCRPCException ex) {
            throw new IOException("communication failure", ex);
        } catch (InterruptedException ex) {
            throw new IOException("operation was interrupted", ex);
        } finally {
            if (r != null)
                r.freeBuffers();
        }

    }

    public void start() throws Exception {
        mdClient.start();
        mdClient.waitForStartup();
        osdClient.start();
        osdClient.waitForStartup();
    }

    public synchronized void stop() {
        if (dirClient != null) {
            try {
                mdClient.shutdown();
                osdClient.shutdown();
                mdClient.waitForShutdown();
                osdClient.waitForShutdown();
                
                for(Volume v: volumeMap.values())
                    v.shutdown();
                
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                dirClient = null;
            }
        }
    }

    public void finalize() {
        stop();
    }
}
