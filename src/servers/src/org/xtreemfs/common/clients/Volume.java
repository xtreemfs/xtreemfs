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
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.foundation.ErrNo;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.DirectoryEntry;
import org.xtreemfs.interfaces.DirectoryEntrySet;
import org.xtreemfs.interfaces.FileCredentials;
import org.xtreemfs.interfaces.FileCredentialsSet;
import org.xtreemfs.interfaces.MRCInterface.MRCException;
import org.xtreemfs.interfaces.NewFileSize;
import org.xtreemfs.interfaces.OSDWriteResponse;
import org.xtreemfs.interfaces.Replica;
import org.xtreemfs.interfaces.Stat;
import org.xtreemfs.interfaces.StatVFS;
import org.xtreemfs.interfaces.UserCredentials;
import org.xtreemfs.interfaces.VivaldiCoordinates;
import org.xtreemfs.interfaces.XCap;
import org.xtreemfs.interfaces.utils.ONCRPCException;
import org.xtreemfs.mrc.client.MRCClient;
import org.xtreemfs.osd.client.OSDClient;

/**
 *
 * @author bjko
 */
public class Volume {

    private final MRCClient    mrcClient;

    private final UUIDResolver uuidResolver;

    private final String       volumeName;

    private final UserCredentials userCreds;

    private final OSDClient osdClient;

    private final OpenFileList ofl;

    public Volume(OSDClient osdClient, MRCClient client, String volumeName, UUIDResolver uuidResolver, UserCredentials userCreds) {
        this.mrcClient = client;
        this.volumeName = volumeName.endsWith("/") ? volumeName : volumeName+"/";
        this.uuidResolver = uuidResolver;
        this.userCreds = userCreds;
        this.osdClient = osdClient;
        this.ofl = new OpenFileList(client);
        ofl.start();
    }

    /**
     * same semantics as File.list()
     * @param path
     * @param cred
     * @return
     * @throws IOException
     */
    public String[] list(String path) throws IOException {
        RPCResponse<DirectoryEntrySet> response = null;
        path = path.replace("//", "/");
        final String fullPath = fixPath(volumeName+path);
        try {
            response = mrcClient.readdir(mrcClient.getDefaultServerAddress(), userCreds, fullPath);
            DirectoryEntrySet entries = response.get();
            String[] list = new String[entries.size()];
            for (int i = 0; i < list.length; i++) {
                list[i] = entries.get(i).getName();
            }
            return list;
        } catch (MRCException ex) {
            if (ex.getError_code() == ErrNo.ENOENT)
                return null;
            throw wrapException(ex);
        } catch (ONCRPCException ex) {
            throw wrapException(ex);
        } catch (InterruptedException ex) {
            throw wrapException(ex);
        } finally {
            if (response != null)
                response.freeBuffers();
        }
    }

    public DirectoryEntry[] listEntries(String path) throws IOException {
        RPCResponse<DirectoryEntrySet> response = null;
        path = path.replace("//", "/");
        final String fullPath = fixPath(volumeName+path);
        try {
            response = mrcClient.readdir(mrcClient.getDefaultServerAddress(), userCreds, fullPath);
            DirectoryEntrySet entries = response.get();
            DirectoryEntry[] list = new DirectoryEntry[entries.size()];
            for (int i = 0; i < list.length; i++) {
                list[i] = entries.get(i);
                final Stat s = list[i].getStbuf();
                OSDWriteResponse r = ofl.getLocalFS(s.getFile_id());
                if (r != null) {
                    final NewFileSize fs = r.getNew_file_size().get(0);
                    //update with local file size, if cahced
                    if ( (fs.getTruncate_epoch() > s.getTruncate_epoch()) ||
                         (fs.getTruncate_epoch() == s.getTruncate_epoch()) &&
                         (fs.getSize_in_bytes() > s.getSize()) ) {
                        s.setSize(fs.getSize_in_bytes());
                        s.setTruncate_epoch(fs.getTruncate_epoch());
                    }
                }
            }
            return list;
        } catch (MRCException ex) {
            if (ex.getError_code() == ErrNo.ENOENT)
                return null;
            throw wrapException(ex);
        } catch (ONCRPCException ex) {
            throw wrapException(ex);
        } catch (InterruptedException ex) {
            throw wrapException(ex);
        } finally {
            if (response != null)
                response.freeBuffers();
        }
    }


    
    public File getFile(String path) {
        return new File(this,userCreds,path);
    }

    String fixPath(String path) {
        path = path.replace("//", "/");
        if (path.endsWith("/"))
            return path.substring(0, path.length()-1);
        return path;
    }

    public long getFreeSpace() throws IOException {
        RPCResponse<StatVFS> response = null;
        try {
            response = mrcClient.statfs(mrcClient.getDefaultServerAddress(), userCreds, volumeName.replace("/", ""));
            StatVFS fsinfo = response.get();
            return fsinfo.getBavail()*fsinfo.getBsize();
        } catch (MRCException ex) {
            throw wrapException(ex);
        } catch (ONCRPCException ex) {
            throw wrapException(ex);
        } catch (InterruptedException ex) {
            throw wrapException(ex);
        } finally {
            if (response != null)
                response.freeBuffers();
        }
    }

    public boolean isReplicateOnClose() throws IOException {
        String numRepl = getxattr(fixPath(volumeName),"xtreemfs.repl_factor");
        if (numRepl == null)
            return false;
        return numRepl.equals("1");
    }

    public int getDefaultReplicationFactor() throws IOException {
        String numRepl = getxattr(fixPath(volumeName),"xtreemfs.repl_factor");
        try {
            return Integer.valueOf(numRepl);
        } catch (Exception ex) {
            throw new IOException("cannot fetch replication factor",ex);
        }
    }
    
    public void setDefaultReplicationFactor(int replicationFactor) throws IOException {
        setxattr(fixPath(volumeName),"xtreemfs.repl_factor",Integer.toString(replicationFactor));
    }

    public long getUsedSpace() throws IOException {
        RPCResponse<StatVFS> response = null;
        try {
            response = mrcClient.statfs(mrcClient.getDefaultServerAddress(), userCreds, volumeName.replace("/", ""));
            StatVFS fsinfo = response.get();
            return (fsinfo.getBlocks()-fsinfo.getBavail())*fsinfo.getBsize();
        } catch (MRCException ex) {
            throw wrapException(ex);
        } catch (ONCRPCException ex) {
            throw wrapException(ex);
        } catch (InterruptedException ex) {
            throw wrapException(ex);
        } finally {
            if (response != null)
                response.freeBuffers();
        }
    }

    public long getDefaultObjectSize() throws IOException {
        RPCResponse<StatVFS> response = null;
        try {
            response = mrcClient.statfs(mrcClient.getDefaultServerAddress(), userCreds, volumeName.replace("/", ""));
            StatVFS fsinfo = response.get();
            return fsinfo.getBsize();
        } catch (MRCException ex) {
            throw wrapException(ex);
        } catch (ONCRPCException ex) {
            throw wrapException(ex);
        } catch (InterruptedException ex) {
            throw wrapException(ex);
        } finally {
            if (response != null)
                response.freeBuffers();
        }
    }

    Stat stat(String path) throws IOException {
        RPCResponse<Stat> response = null;
        try {
            response = mrcClient.getattr(mrcClient.getDefaultServerAddress(), userCreds, fixPath(volumeName+path));
            Stat s = response.get();
            OSDWriteResponse r = ofl.getLocalFS(s.getFile_id());
            if (r != null) {
                final NewFileSize fs = r.getNew_file_size().get(0);
                //update with local file size, if cahced
                if ( (fs.getTruncate_epoch() > s.getTruncate_epoch()) ||
                     (fs.getTruncate_epoch() == s.getTruncate_epoch()) &&
                     (fs.getSize_in_bytes() > s.getSize()) ) {
                    s.setSize(fs.getSize_in_bytes());
                    s.setTruncate_epoch(fs.getTruncate_epoch());
                }
            }
            return s;
        } catch (MRCException ex) {
            throw wrapException(ex);
        } catch (ONCRPCException ex) {
            throw wrapException(ex);
        } catch (InterruptedException ex) {
            throw wrapException(ex);
        } finally {
            if (response != null)
                response.freeBuffers();
        }
    }

    String getxattr(String path, String name) throws IOException {
        RPCResponse<String> response = null;
        try {
            response = mrcClient.getxattr(mrcClient.getDefaultServerAddress(), userCreds, fixPath(volumeName+path), name);
            return response.get();
        } catch (MRCException ex) {
            if (ex.getError_code() == ErrNo.ENODATA)
                return null;
            throw wrapException(ex);
        } catch (ONCRPCException ex) {
            throw wrapException(ex);
        } catch (InterruptedException ex) {
            throw wrapException(ex);
        } finally {
            if (response != null)
                response.freeBuffers();
        }
    }

    void setxattr(String path, String name, String value) throws IOException {
        RPCResponse response = null;
        try {
            response = mrcClient.setxattr(mrcClient.getDefaultServerAddress(), userCreds, fixPath(volumeName+path), name, value, 0);
            response.get();
        } catch (MRCException ex) {
            throw wrapException(ex);
        } catch (ONCRPCException ex) {
            throw wrapException(ex);
        } catch (InterruptedException ex) {
            throw wrapException(ex);
        } finally {
            if (response != null)
                response.freeBuffers();
        }
    }

    void mkdir(String path) throws IOException {
        RPCResponse response = null;
        try {
            response = mrcClient.mkdir(mrcClient.getDefaultServerAddress(), userCreds, fixPath(volumeName+path), 0700);
            response.get();
        } catch (MRCException ex) {
           throw wrapException(ex);
        } catch (ONCRPCException ex) {
            throw wrapException(ex);
        } catch (InterruptedException ex) {
            throw wrapException(ex);
        } finally {
            if (response != null)
                response.freeBuffers();
        }
    }

    void touch(String path) throws IOException {
        RPCResponse response = null;
        try {
            response = mrcClient.create(mrcClient.getDefaultServerAddress(), userCreds, fixPath(volumeName+path), 0700);
            response.get();
        } catch (MRCException ex) {
           throw wrapException(ex);
        } catch (ONCRPCException ex) {
            throw wrapException(ex);
        } catch (InterruptedException ex) {
            throw wrapException(ex);
        } finally {
            if (response != null)
                response.freeBuffers();
        }
    }

    void rename(String src, String dest) throws IOException {
        RPCResponse response = null;
        try {
            response = mrcClient.rename(mrcClient.getDefaultServerAddress(), userCreds, fixPath(volumeName+src),fixPath(volumeName+dest));
            response.get();
        } catch (MRCException ex) {
           throw wrapException(ex);
        } catch (ONCRPCException ex) {
            throw wrapException(ex);
        } catch (InterruptedException ex) {
            throw wrapException(ex);
        } finally {
            if (response != null)
                response.freeBuffers();
        }
    }

    void unlink(String path) throws IOException {
        RPCResponse<FileCredentialsSet> response = null;
        RPCResponse ulnkResp = null;
        try {
            response = mrcClient.unlink(mrcClient.getDefaultServerAddress(), userCreds, fixPath(volumeName+path));
            FileCredentialsSet fcs = response.get();
            if (fcs.size() > 0) {
                //delete on OSDs
                final FileCredentials fc = fcs.get(0);
                for (Replica r : fc.getXlocs().getReplicas()) {
                    final String headOSDuuid = r.getOsd_uuids().get(0);
                    final ServiceUUID osdAddr = new ServiceUUID(headOSDuuid, uuidResolver);
                    osdAddr.resolve();
                    ulnkResp = osdClient.unlink(osdAddr.getAddress(), fc.getXcap().getFile_id(), fc);
                    ulnkResp.get();
                    ulnkResp.freeBuffers();
                    ulnkResp = null;
                }
            }
        } catch (MRCException ex) {
           throw wrapException(ex);
        } catch (ONCRPCException ex) {
            throw wrapException(ex);
        } catch (InterruptedException ex) {
            throw wrapException(ex);
        } finally {
            if (response != null)
                response.freeBuffers();
            if (ulnkResp != null)
                ulnkResp.freeBuffers();
        }
    }

    void storeFileSizeUpdate(String fileId, OSDWriteResponse resp) {
        ofl.fsUpdate(fileId, resp);
    }

    void pushFileSizeUpdate(String fileId) throws IOException {
        OSDWriteResponse owr = ofl.sendFsUpdate(fileId);
        if (owr != null) {
            XCap cap = ofl.getCapability(fileId);
            RPCResponse response = null;
            try {
                response = mrcClient.xtreemfs_update_file_size(mrcClient.getDefaultServerAddress(), cap, owr);
                response.get();
            } catch (MRCException ex) {
               throw wrapException(ex);
            } catch (ONCRPCException ex) {
                throw wrapException(ex);
            } catch (InterruptedException ex) {
                throw wrapException(ex);
            } finally {
                if (response != null)
                    response.freeBuffers();
            }
        }
    }

    void closeFile(String fileId, boolean readOnly) throws IOException {
        try {
            pushFileSizeUpdate(fileId);
            XCap cap = ofl.getCapability(fileId);
            if (!readOnly && cap.getReplicate_on_close()) {
                //notify MRC that file has been closed
                RPCResponse response = null;
                try {
                    response = mrcClient.close(mrcClient.getDefaultServerAddress(), new VivaldiCoordinates(), cap);
                    response.get();
                } catch (Exception ex) {
                   throw new IOException("cannot mark file as read-only for automatic replication",ex);
                } finally {
                    if (response != null)
                        response.freeBuffers();
                }
            }
        } finally {
            ofl.closeFile(fileId);
        }


    }

    RandomAccessFile openFile(File parent, int flags, int mode) throws IOException {
        RPCResponse<FileCredentials> response = null;
        try {
            response = mrcClient.open(mrcClient.getDefaultServerAddress(), userCreds, fixPath(volumeName+parent.getPath()),flags,mode,0,new VivaldiCoordinates());
            FileCredentials cred = response.get();
            ofl.openFile(cred.getXcap());
            boolean rdOnly = ( (mode == Constants.SYSTEM_V_FCNTL_H_O_RDONLY) ||
                 (cred.getXlocs().getReplica_update_policy().equals(Constants.REPL_UPDATE_PC_RONLY)) );
            return new RandomAccessFile(parent, this, osdClient, cred, rdOnly);
        } catch (MRCException ex) {
           throw wrapException(ex);
        } catch (ONCRPCException ex) {
            throw wrapException(ex);
        } catch (InterruptedException ex) {
            throw wrapException(ex);
        } finally {
            if (response != null)
                response.freeBuffers();
        }
    }

    static IOException wrapException(MRCException ex) {
        return new IOException(ex.getError_message(),ex);
    }

    static IOException wrapException(ONCRPCException ex) {
        return new IOException("communication failure",ex);
    }

    static IOException wrapException(InterruptedException ex) {
        return new IOException("operation was interruped",ex);
    }

    public void finalize() {
        ofl.shutdown();
    }





}
