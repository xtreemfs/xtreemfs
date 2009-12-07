/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.common.clients;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;
import org.xtreemfs.interfaces.Constants;

/**
 *
 * @author bjko
 */
public class FileReplication {

    private final File file;

    private final Volume parentVolume;

    FileReplication(File file, Volume parentVolume) {
        this.file = file;
        this.parentVolume = parentVolume;
    }

    public int getNumReplicas() throws IOException {
        try {
            Map<String,Object> xloc = getLocations();
            List<Map<String,Object>> replicas = (List<Map<String, Object>>) xloc.get("replicas");
            return replicas.size();
        } catch (ClassCastException ex) {
            throw new IOException("cannot parse file's location list",ex);
        }
    }

    public boolean isReadOnlyReplication() throws IOException {
        try {
            Map<String,Object> xloc = getLocations();
            String uPolicy = (String) xloc.get("update-policy");
            return uPolicy.equals(Constants.REPL_UPDATE_PC_RONLY);
        } catch (ClassCastException ex) {
            throw new IOException("cannot parse file's location list",ex);
        }
    }

    public InetSocketAddress[] getReplicaLocations() throws IOException {
        try {
            Map<String,Object> xloc = getLocations();
            List<Map<String,Object>> replicas = (List<Map<String, Object>>) xloc.get("replicas");
            InetSocketAddress[] addrs = new InetSocketAddress[replicas.size()];
            for (int i = 0; i < addrs.length; i++) {
                List<Map<String,Object>> osds = (List<Map<String, Object>>) replicas.get(i).get("osds");
                ServiceUUID addr = new ServiceUUID((String) osds.get(0).get("uuid"),parentVolume.uuidResolver);
                addrs[i] = addr.getAddress();
            }
            return addrs;
        } catch (ClassCastException ex) {
            throw new IOException("cannot parse file's location list",ex);
        }
    }

    Map<String,Object> getLocations() throws IOException {
        try {
            String loc = parentVolume.getxattr(file.getPath(), "xtreemfs.locations");
            if ( (loc != null) && (loc.length() > 0) ) {
                return (Map<String, Object>) JSONParser.parseJSON(new JSONString(loc));
            } else {
                throw new IOException("cannot retrieve file's location list (is empty)");
            }
        } catch (JSONException ex) {
            throw new IOException("cannot parse file's location list",ex);
        } catch (ClassCastException ex) {
            throw new IOException("cannot parse file's location list",ex);
        }
    }

}
