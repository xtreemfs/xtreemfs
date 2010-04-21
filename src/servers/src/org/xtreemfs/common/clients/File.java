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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.Stat;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.UserCredentials;

/**
 *
 * @author bjko
 */
public class File {
    
    public static final String XTREEMFSSET_REPL_UPDATE_POLICY_XATTR = "xtreemfs.set_repl_update_policy";

    public static final String XTREEMFS_DEFAULT_RP = "xtreemfs.default_rp";

    private final Volume volume;

    private final String path;

    private final UserCredentials userCreds;

    File(Volume volume, UserCredentials userCreds, String path) {
        this.volume = volume;
        this.path = path;
        this.userCreds = userCreds;
    }

    public String getPath() {
        return path;
    }


    /**
     * check if path is a file
     * @see java.io.File
     * @return true if it is a file, false otherwise (also if path does not exist)
     */
    public boolean isFile() throws IOException {
        Stat stat = volume.stat(path);
        if (stat != null)
            return (stat.getMode() & Constants.SYSTEM_V_FCNTL_H_S_IFREG) > 0;
        else
            return false;
    }

    /**
     * check if path is a directory
     * @see java.io.File
     * @return true if it is a directory, false otherwise (also if path does not exist)
     */
    public boolean isDirectory() throws IOException {
        Stat stat = volume.stat(path);
        if (stat != null)
            return (stat.getMode() & Constants.SYSTEM_V_FCNTL_H_S_IFDIR) > 0;
        else
            return false;
    }

    /**
     * check if path exists (file or directory)
     * @see java.io.File
     * @return true if it exists, false otherwise
     */
    public boolean exists() throws IOException {
        try {
            Stat stat = volume.stat(path);
        } catch (FileNotFoundException ex) {
            return false;
        }
        return true;
    }

    public boolean canRead() throws IOException {
        try {
            Stat stat = volume.stat(path);
            return (stat.getMode() & 0400) > 0;
        } catch (FileNotFoundException ex) {
            return false;
        }
    }

    public boolean canWrite() throws IOException {
        try {
            Stat stat = volume.stat(path);
            return (stat.getMode() & 0200) > 0;
        } catch (FileNotFoundException ex) {
            return false;
        }
    }

    public long lastModified() throws IOException {
        Stat stat = volume.stat(path);
        return stat.getMtime_ns()/1000000;
    }

    /**
     * get file size
     * @return the files size in bytes, or 0L if it does not exist
     * @throws IOException
     */
    public long length() throws IOException {

        // if the volume is a snapshot, perform a size glimpse at the OSD
        if (volume.isSnapshot()) {
            
            RPCResponse<Long> fs = null;
            try {
                RandomAccessFile file = volume.openFile(this, Constants.SYSTEM_V_FCNTL_H_O_RDONLY, 0);
                fs = volume.osdClient.internal_get_file_size(getReplica(0).getOSDAddress(0),
                    file.getFileId(), file.getCredentials());
                return fs.get();
            } catch (Exception exc) {
                exc.printStackTrace();
                return 0;
            } finally {
                if (fs != null)
                    fs.freeBuffers();
            }
            
        }

        // otherwise, fetch the file size from the MRC
        else {
            Stat stat = volume.stat(path);
            if (stat != null) {
                return stat.getSize();
            } else
                return 0L;
        }
    }

    public void mkdir(int permissions) throws IOException {
        volume.mkdir(path, permissions);
    }

    public void createFile() throws IOException {
        volume.touch(path);
    }

    public Stat stat() throws IOException {
        return volume.stat(path);
    }

    public void renameTo(File dest) throws IOException {
        volume.rename(this.path,dest.path);
    }

    public void delete() throws IOException {
        volume.unlink(this.path);
    }

    public String getxattr(String name) throws IOException {
        return volume.getxattr(path, name);
    }
    
    public String[] listXAttrs() throws IOException {
        return volume.listxattr(path);
    }

    public void setxattr(String name, String value) throws IOException {
        volume.setxattr(path, name, value);
    }

    public RandomAccessFile open(String openMode, int permissions) throws IOException {
        int flags = 0;
        if (openMode.contains("rw")) {
            flags |= Constants.SYSTEM_V_FCNTL_H_O_RDWR;
            flags |= Constants.SYSTEM_V_FCNTL_H_O_CREAT;
        } else if (openMode.contains("r")) {
            flags |= Constants.SYSTEM_V_FCNTL_H_O_RDONLY;
        } 
            
        if (openMode.contains("t")) {
            flags |= Constants.SYSTEM_V_FCNTL_H_O_TRUNC;
        }
        if (openMode.contains("d") || openMode.contains("s")) {
            flags |= Constants.SYSTEM_V_FCNTL_H_O_SYNC;
        }
        return volume.openFile(this, flags, permissions);
    }

    public int getNumReplicas() throws IOException {
        try {
            Map<String,Object> xloc = getLocations();
            List<Map<String,Object>> replicas = (List<Map<String, Object>>) xloc.get("replicas");
            return replicas.size();
        } catch (ClassCastException ex) {
            throw new IOException("cannot parse file's location list: "+ex,ex);
        }
    }

    public Replica getReplica(int replicaNo) throws IOException {
       try {
            Map<String,Object> xloc = getLocations();
            List<Map<String,Object>> replicas = (List<Map<String, Object>>) xloc.get("replicas");
            if (replicas.size() <= replicaNo)
                throw new IllegalArgumentException("replicaNo is out of bounds");
            return new Replica(this,replicas.get(replicaNo));
       } catch (JSONException ex) {
           throw new IOException("cannot parse file's location list: "+ex,ex);
        } catch (ClassCastException ex) {
            throw new IOException("cannot parse file's location list: "+ex,ex);
        }
    }

    public Replica getReplica(String osdUUID) throws IOException {
        Replica[] repls = getReplicas();
        for (Replica r : repls) {
            for (int i = 0; i < r.getStripeWidth(); i++) {
                if (r.getOSDUuid(i).equals(osdUUID))
                    return r;
            }
        }
        return null;
    }

    public Replica[] getReplicas() throws IOException {
       try {
            Map<String,Object> xloc = getLocations();
            List<Map<String,Object>> replicas = (List<Map<String, Object>>) xloc.get("replicas");
            Replica[] repls = new Replica[replicas.size()];
            for (int i = 0; i < repls.length; i++)
                repls[i] = new Replica(this,replicas.get(i));
            return repls;
       } catch (JSONException ex) {
           throw new IOException("cannot parse file's location list",ex);
        } catch (ClassCastException ex) {
            throw new IOException("cannot parse file's location list",ex);
        }
    }

    public void setDefaultReplication(String policy, int numReplicas) throws IOException {
        String JSON = "{ \"name\" : \""+policy+"\", \"numRepls\" : "+numReplicas+" }";
        if (!isDirectory())
            throw new IOException("only diretories (including root) have a default replication policy");
        volume.setxattr(path, XTREEMFS_DEFAULT_RP, JSON);
    }

    public boolean isReadOnlyReplicated() throws IOException {
        try {
            Map<String,Object> xloc = getLocations();
            String uPolicy = (String) xloc.get("update-policy");
            return uPolicy.equals(Constants.REPL_UPDATE_PC_RONLY);
        } catch (ClassCastException ex) {
            throw new IOException("cannot parse file's location list",ex);
        }
    }

    public void setReadOnly(boolean mode) throws Exception {

        boolean currentMode = Boolean.valueOf(getxattr("xtreemfs.read_only"));

        if (currentMode == mode)
            return;

        if (mode) {
            //make sure the file is not open!

            //open file
            RandomAccessFile raf = open("r", 0);
            //fetch file sizes
            long osd_file_size = raf.getFileSizeOnOSD();
            long mrc_file_size = length();

            //update file size if incorrect on MRC
            if (osd_file_size != mrc_file_size) {
                raf.forceFileSize(osd_file_size);
            }

            setxattr("xtreemfs.read_only", "true");
        } else {
            if (getNumReplicas() > 1)
                throw new IOException("File has still replicas.");
            else {
                // set read only
                setxattr("xtreemfs.read_only", "false");
            }
        }
    }

    public boolean isReadOnly() throws IOException {
        return Boolean.valueOf(getxattr("xtreemfs.read_only"));
    }

    public boolean isReplicated() throws IOException {
        Map<String,Object> l = getLocations();
        String updatePolicy = (String)l.get("update-policy");
        return !updatePolicy.equals(Constants.REPL_UPDATE_PC_NONE);
    }

    public String[] getSuitableOSDs(int numOSDs) throws IOException {
        StringSet osds = volume.getSuitableOSDs(this, numOSDs);
        return osds.toArray(new String[osds.size()]);
    }

    public void addReplica(int width, String[] osdUuids, int flags) throws IOException {
        StringSet osdSet = new StringSet();
        for (String osd : osdUuids) {
            if (osdSet.size() == width)
                break;
            osdSet.add(osd);
        }
        if (osdSet.size() != width)
            throw new IllegalArgumentException("number of OSDs must be equal to width!");
        
        volume.addReplica(this, width, osdSet, flags);
    }

    public void setReplicaUpdatePolicy(String policy) throws IOException {
        volume.setxattr(this.getPath(), XTREEMFSSET_REPL_UPDATE_POLICY_XATTR, policy);
    }

    public String getReplicaUpdatePolicy() throws IOException {
        return volume.getxattr(this.getPath(), XTREEMFSSET_REPL_UPDATE_POLICY_XATTR);
    }



    Map<String,Object> getLocations() throws IOException {
        try {
            String loc = this.volume.getxattr(this.getPath(), "xtreemfs.locations");
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

    void removeReplica(String headOSDuuid) throws IOException {
        if (!this.isFile())
            throw new IOException("cannot remove replica from a non-file object");
        
        volume.removeReplica(this,headOSDuuid);
    }



}
