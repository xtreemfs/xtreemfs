/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.clients;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Stat;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_internal_get_file_sizeResponse;

/**
 *
 * @author bjko
 */
public class File {
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
     * @param userCreds the user's credentials
     * @see java.io.File
     * @return true if it is a file, false otherwise (also if path does not exist)
     */
    public boolean isFile(UserCredentials userCreds) throws IOException {
        Stat stat = volume.stat(path, userCreds);
        if (stat != null)
            return (stat.getMode() & SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_S_IFREG.getNumber()) > 0;
        else
            return false;
    }
    
    /**
     * check if path is a file
     * @see java.io.File
     * @return true if it is a file, false otherwise (also if path does not exist)
     */
    public boolean isFile() throws IOException {
        return isFile(userCreds);
    }

    /**
     * check if path is a directory
     * @param userCreds the user's credentials
     * @see java.io.File
     * @return true if it is a directory, false otherwise (also if path does not exist)
     */
    public boolean isDirectory(UserCredentials userCreds) throws IOException {
        Stat stat = volume.stat(path, userCreds);
        if (stat != null)
            return (stat.getMode() & SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_S_IFDIR.getNumber()) > 0;
        else
            return false;
    }
    
    /**
     * check if path is a directory
     * @see java.io.File
     * @return true if it is a directory, false otherwise (also if path does not exist)
     */
    public boolean isDirectory() throws IOException {
        return isDirectory(userCreds);
    }

    /**
     * check if path exists (file or directory)
     * @param userCreds the user's credentials
     * @see java.io.File
     * @return true if it exists, false otherwise
     */
    public boolean exists(UserCredentials userCreds) throws IOException {
        try {
            Stat stat = volume.stat(path, userCreds);
        } catch (FileNotFoundException ex) {
            return false;
        }
        return true;
    }
    
    /**
     * check if path exists (file or directory)
     * @see java.io.File
     * @return true if it exists, false otherwise
     */
    public boolean exists() throws IOException {
        return exists(userCreds);
    }
    
    public boolean canRead(UserCredentials userCreds) throws IOException {
        try {
            Stat stat = volume.stat(path, userCreds);
            return (stat.getMode() & 0400) > 0;
        } catch (FileNotFoundException ex) {
            return false;
        }
    }

    public boolean canRead() throws IOException {
        return canRead(userCreds);
    }
    
    public boolean canWrite(UserCredentials userCreds) throws IOException {
        try {
            Stat stat = volume.stat(path, userCreds);
            return (stat.getMode() & 0200) > 0;
        } catch (FileNotFoundException ex) {
            return false;
        }
    }

    public boolean canWrite() throws IOException {
        return canWrite(userCreds);
    }
    
    public long lastModified(UserCredentials userCreds) throws IOException {
        Stat stat = volume.stat(path, userCreds);
        return stat.getMtimeNs()/1000000;
    }

    public long lastModified() throws IOException {
        return lastModified(userCreds);
    }
    
    /**
     * get file size
     * @param userCreds the user's credentials
     * @return the files size in bytes, or 0L if it does not exist
     * @throws IOException
     */
    public long length(UserCredentials userCreds) throws IOException {

        // if the volume is a snapshot, perform a size glimpse at the OSD
        if (volume.isSnapshot()) {
            
            RPCResponse<xtreemfs_internal_get_file_sizeResponse> fs = null;
            try {
                RandomAccessFile file = volume.openFile(this, SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDONLY.getNumber(), 0, userCreds);
                fs = volume.osdClient.xtreemfs_internal_get_file_size(getReplica(0).getOSDAddress(0), RPCAuthentication.authNone, RPCAuthentication.userService,
                    file.getCredentials(), file.getFileId());
                return fs.get().getFileSize();
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
            Stat stat = volume.stat(path, userCreds);
            if (stat != null) {
                return stat.getSize();
            } else
                return 0L;
        }
    }

    /**
     * get file size
     * @return the files size in bytes, or 0L if it does not exist
     * @throws IOException
     */
    public long length() throws IOException {
        return length(userCreds);
    }
    
    public void mkdir(int permissions, UserCredentials userCreds) throws IOException {
        volume.mkdir(path, permissions, userCreds);
    }

    public void mkdir(int permissions) throws IOException {
        mkdir(permissions, userCreds);
    }
    
    public void createFile(UserCredentials userCreds) throws IOException {
        volume.touch(path, userCreds);
    }

    public void createFile() throws IOException {
        createFile(userCreds);
    }
    
    public Stat stat(UserCredentials userCreds) throws IOException {
        return volume.stat(path, userCreds);
    }

    public Stat stat() throws IOException {
        return stat(userCreds);
    }

    public void renameTo(File dest, UserCredentials userCreds) throws IOException {
        volume.rename(this.path,dest.path, userCreds);
    }
    
    public void renameTo(File dest) throws IOException {
        renameTo(dest, userCreds);
    }
    
    public void delete(UserCredentials userCreds) throws IOException {
        volume.unlink(this.path, userCreds);
    }

    public void delete() throws IOException {
        delete(userCreds);
    }

    public String getxattr(String name, UserCredentials userCreds) throws IOException {
        return volume.getxattr(path, name, userCreds);
    }
    
    public String getxattr(String name) throws IOException {
        return getxattr(name, userCreds);
    }
    
    public String[] listXAttrs(UserCredentials userCreds) throws IOException {
        return volume.listxattr(path, userCreds);
    }
    
    public String[] listXAttrs() throws IOException {
        return listXAttrs(userCreds);
    }

    public void setxattr(String name, String value, UserCredentials userCreds) throws IOException {
        volume.setxattr(path, name, value, userCreds);
    }
    
    public void setxattr(String name, String value) throws IOException {
        setxattr(name, value, userCreds);
    }
    
    public void chmod(int mode, UserCredentials userCreds) throws IOException {
        volume.chmod(path, mode, userCreds);
    }
    
    public void chmod(int mode) throws IOException {
        chmod(mode, userCreds);
    }
    
    public void chown(String user, UserCredentials userCreds) throws IOException {
        volume.chown(path, user, userCreds);
    }
    
    public void chown(String user) throws IOException {
        chown(user, userCreds);
    }
    
    public void chgrp(String group, UserCredentials userCreds) throws IOException {
        volume.chgrp(path, group, userCreds);
    }
    
    public void chgrp(String group) throws IOException {
        chgrp(group, userCreds);
    }
    
    public void setACL(Map<String, Object> aclEntries, UserCredentials userCreds) throws IOException {
        volume.setACL(path, aclEntries, userCreds);
    }
    
    public void setACL(Map<String, Object> aclEntries) throws IOException {
        setACL(aclEntries, userCreds);
    }
    
    public Map<String, Object> getACL(UserCredentials userCreds) throws IOException {
        return volume.getACL(path, userCreds);
    }
    
    public Map<String, Object> getACL() throws IOException {
        return getACL(userCreds);
    }
    
    public RandomAccessFile open(String openMode, int permissions, UserCredentials userCreds) throws IOException {
        int flags = 0;
        if (openMode.contains("rw")) {
            flags |= SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber();
            flags |= SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber();
        } else if (openMode.contains("r")) {
            flags |= SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDONLY.getNumber();
        } 
            
        if (openMode.contains("t")) {
            flags |= SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber();
        }
        if (openMode.contains("d") || openMode.contains("s")) {
            flags |= SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_SYNC.getNumber();
        }
        return volume.openFile(this, flags, permissions, userCreds);
    }

    public RandomAccessFile open(String openMode, int permissions) throws IOException {
        return open(openMode, permissions, userCreds);
    }
    
    public int getNumReplicas(UserCredentials userCreds) throws IOException {
        try {
            Map<String,Object> xloc = getLocations(userCreds);
            List<Map<String,Object>> replicas = (List<Map<String, Object>>) xloc.get("replicas");
            return replicas.size();
        } catch (ClassCastException ex) {
            throw new IOException("cannot parse file's location list: "+ex,ex);
        }
    }

    public int getNumReplicas() throws IOException {
        return getNumReplicas(userCreds);
    }
    
    public Replica getReplica(int replicaNo, UserCredentials userCreds) throws IOException {
        try {
             Map<String,Object> xloc = getLocations(userCreds);
             List<Map<String,Object>> replicas = (List<Map<String, Object>>) xloc.get("replicas");
             if (replicas.size() <= replicaNo)
                 throw new IllegalArgumentException("replicaNo is out of bounds");
             return new Replica(this,replicas.get(replicaNo),userCreds);
        } catch (JSONException ex) {
            throw new IOException("cannot parse file's location list: "+ex,ex);
         } catch (ClassCastException ex) {
             throw new IOException("cannot parse file's location list: "+ex,ex);
         }
     }

    public Replica getReplica(int replicaNo) throws IOException {
       return getReplica(replicaNo, userCreds);
    }
    
    public Replica getReplica(String osdUUID, UserCredentials userCreds) throws IOException {
        Replica[] repls = getReplicas(userCreds);
        for (Replica r : repls) {
            for (int i = 0; i < r.getStripeWidth(); i++) {
                if (r.getOSDUuid(i).equals(osdUUID))
                    return r;
            }
        }
        return null;
    }

    public Replica getReplica(String osdUUID) throws IOException {
        return getReplica(osdUUID, userCreds);
    }
    
    public Replica[] getReplicas(UserCredentials userCreds) throws IOException {
        try {
             Map<String,Object> xloc = getLocations(userCreds);
             List<Map<String,Object>> replicas = (List<Map<String, Object>>) xloc.get("replicas");
             Replica[] repls = new Replica[replicas.size()];
             for (int i = 0; i < repls.length; i++)
                 repls[i] = new Replica(this,replicas.get(i),userCreds);
             return repls;
        } catch (JSONException ex) {
            throw new IOException("cannot parse file's location list",ex);
         } catch (ClassCastException ex) {
             throw new IOException("cannot parse file's location list",ex);
         }
     }

    public Replica[] getReplicas() throws IOException {
       return getReplicas(userCreds);
    }
    
    public void setDefaultReplication(String policy, int numReplicas, UserCredentials userCreds) throws IOException {
        String JSON = "{ \"update-policy\" : \""+policy+"\", \"replication-factor\" : "+numReplicas+" }";
        if (!isDirectory())
            throw new IOException("only diretories (including root) have a default replication policy");
        volume.setxattr(path, XTREEMFS_DEFAULT_RP, JSON, userCreds);
    }

    public void setDefaultReplication(String policy, int numReplicas) throws IOException {
        setDefaultReplication(policy, numReplicas, userCreds);
    }
    
    public boolean isReadOnlyReplicated(UserCredentials userCreds) throws IOException {
        try {
            Map<String,Object> xloc = getLocations(userCreds);
            String uPolicy = (String) xloc.get("update-policy");
            return uPolicy.equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY);
        } catch (ClassCastException ex) {
            throw new IOException("cannot parse file's location list",ex);
        }
    }

    public boolean isReadOnlyReplicated() throws IOException {
        return isReadOnlyReplicated(userCreds);
    }
    
    public void setReadOnly(boolean mode, UserCredentials userCreds) throws Exception {

        boolean currentMode = Boolean.valueOf(getxattr("xtreemfs.read_only"));

        if (currentMode == mode)
            return;

        if (mode) {
            //make sure the file is not open!

            //open file
            RandomAccessFile raf = open("r", 0, userCreds);
            //fetch file sizes
            long osd_file_size = raf.getFileSizeOnOSD();
            long mrc_file_size = length(userCreds);

            //update file size if incorrect on MRC
            if (osd_file_size != mrc_file_size) {
                raf.forceFileSize(osd_file_size);
            }

            volume.setReplicaUpdatePolicy(path, ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY, userCreds);
        } else {
            if (getNumReplicas() > 1)
                throw new IOException("File has still replicas.");
            else {
                // set read only
                volume.setReplicaUpdatePolicy(path, ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE, userCreds);
            }
        }
    }

    public void setReadOnly(boolean mode) throws Exception {
        setReadOnly(mode, userCreds);
    }

    public boolean isReadOnly(UserCredentials userCreds) throws IOException {
        return Boolean.valueOf(getxattr("xtreemfs.read_only", userCreds));
    }
    
    public boolean isReadOnly() throws IOException {
       return isReadOnly(userCreds);
    }
    
    public boolean isReplicated(UserCredentials userCreds) throws IOException {
        Map<String,Object> l = getLocations(userCreds);
        String updatePolicy = (String)l.get("update-policy");
        return !updatePolicy.equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE);
    }

    public boolean isReplicated() throws IOException {
        return isReplicated(userCreds);
    }

    public String[] getSuitableOSDs(int numOSDs, UserCredentials userCreds) throws IOException {
        List<String> osds = volume.getSuitableOSDs(this, numOSDs, userCreds);
        return osds.toArray(new String[osds.size()]);
    }
    
    public String[] getSuitableOSDs(int numOSDs) throws IOException {
        return getSuitableOSDs(numOSDs, userCreds);
    }

    public void addReplica(int width, String[] osdUuids, int flags, UserCredentials userCreds) throws IOException {
        List<String> osdSet = new ArrayList(20);
        for (String osd : osdUuids) {
            if (osdSet.size() == width)
                break;
            osdSet.add(osd);
        }
        if (osdSet.size() != width)
            throw new IllegalArgumentException("number of OSDs must be equal to width!");
        
        volume.addReplica(this, width, osdSet, flags, userCreds);
    }
    
    public void addReplica(int width, String[] osdUuids, int flags) throws IOException {
        addReplica(width, osdUuids, flags, userCreds);
    }

    public void setReplicaUpdatePolicy(String policy, UserCredentials userCreds) throws IOException {
        volume.setReplicaUpdatePolicy(this.getPath(), policy, userCreds);
    }
    
    public void setReplicaUpdatePolicy(String policy) throws IOException {
        setReplicaUpdatePolicy(policy, userCreds);
    }
    
    public String getReplicaUpdatePolicy(UserCredentials userCreds) throws IOException {
        try {
            String loc = this.volume.getxattr(this.getPath(), "xtreemfs.locations", userCreds);
            if ( (loc != null) && (loc.length() > 0) ) {
                Map<String,Object> location = (Map<String, Object>) JSONParser.parseJSON(new JSONString(loc));
                return (String) location.get("update-policy");
            } else {
                throw new IOException("cannot retrieve file's location list (is empty)");
            }
        } catch (JSONException ex) {
            throw new IOException("cannot parse file's location list",ex);
        } catch (ClassCastException ex) {
            throw new IOException("cannot parse file's location list",ex);
        }
    }

    public String getReplicaUpdatePolicy() throws IOException {
        return getReplicaUpdatePolicy(userCreds);
    }

    Map<String,Object> getLocations(UserCredentials userCreds) throws IOException {
        try {
            String loc = this.volume.getxattr(this.getPath(), "xtreemfs.locations", userCreds);
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

    void removeReplica(String headOSDuuid, UserCredentials userCreds) throws IOException {
        if (!this.isFile())
            throw new IOException("cannot remove replica from a non-file object");
        
        volume.removeReplica(this, headOSDuuid, userCreds);
    }



}
