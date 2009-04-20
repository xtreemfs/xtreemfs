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
 * AUTHORS: Nele Andersen (ZIB), Björn Kolbeck (ZIB), Christian Lorenz (ZIB)
 */
package org.xtreemfs.common.clients.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.Replica;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.FileCredentials;
import org.xtreemfs.interfaces.FileCredentialsSet;
import org.xtreemfs.interfaces.NewFileSize;
import org.xtreemfs.interfaces.NewFileSizeSet;
import org.xtreemfs.interfaces.OSDWriteResponse;
import org.xtreemfs.interfaces.OSDtoMRCDataSet;
import org.xtreemfs.interfaces.ObjectData;
import org.xtreemfs.interfaces.Stat;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.StripingPolicy;
import org.xtreemfs.interfaces.UserCredentials;
import org.xtreemfs.interfaces.XCap;
import org.xtreemfs.interfaces.MRCInterface.setxattrResponse;
import org.xtreemfs.interfaces.utils.ONCRPCException;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.client.MRCClient;
import org.xtreemfs.osd.client.OSDClient;

public class RandomAccessFile implements ObjectStore {

    private MRCClient mrcClient;

    private OSDClient osdClient;

    private FileCredentials fileCredentials;

    // all replicas have the same striping policy at the moment
    private StripingPolicyImpl stripingPolicy;

    private final String fileId;
    
    private final int mode;

    private final String pathName;

    private final InetSocketAddress mrcAddress;

    private ByteMapper byteMapper;

    private OSDWriteResponse wresp;

    private long filePos;

    private Map<String, String> capAndXLoc;

    private long capTime;

    private Replica            currentReplica;

    private boolean      isReadOnly;

    private final UserCredentials credentials;

    public RandomAccessFile(String mode, InetSocketAddress mrcAddress, String pathName,
            RPCNIOSocketClient rpcClient,
            String userID, List<String> groupIDs) throws ONCRPCException,InterruptedException,IOException {
            this(mode, mrcAddress, pathName, rpcClient,  MRCClient.getCredentials(userID, groupIDs));
    }

    public RandomAccessFile(String mode, InetSocketAddress mrcAddress, String pathName,
            RPCNIOSocketClient rpcClient, 
            UserCredentials credentials) throws ONCRPCException,InterruptedException,IOException {
        this.pathName = pathName;
        this.mrcAddress = mrcAddress;
        this.mode = translateMode(mode);

        assert (rpcClient != null);

        // use the shared speedy to create an MRC and OSD client
        mrcClient = new MRCClient(rpcClient, mrcAddress);
        osdClient = new OSDClient(rpcClient);

        // create a new file if necessary

        this.credentials = credentials;

        RPCResponse<FileCredentials> r = mrcClient.open(mrcAddress, credentials, pathName, FileAccessManager.O_CREAT, this.mode, 0);
        fileCredentials = r.get();
        r.freeBuffers();


        // all replicas have the same striping policy at the moment
        stripingPolicy = StripingPolicyImpl.getPolicy(fileCredentials.getXlocs().getReplicas().get(0));
        XLocations loc = new XLocations(fileCredentials.getXlocs());
        currentReplica = loc.getReplica(0);
        byteMapper = ByteMapperFactory.createByteMapper(
                stripingPolicy.getPolicyId(), stripingPolicy.getStripeSizeForObject(0), this);

        capTime = System.currentTimeMillis();

        isReadOnly = fileCredentials.getXlocs().getRepUpdatePolicy().equals(Constants.REPL_UPDATE_PC_RONLY);

        fileId = fileCredentials.getXcap().getFile_id();
        wresp = null;
        filePos = 0;
    }

    private static int translateMode(String mode) {
        if (mode.equals("r"))
            return FileAccessManager.O_RDONLY;
        if (mode.startsWith("rw"))
            return FileAccessManager.O_RDWR;
        throw new IllegalArgumentException("invalid mode");  
    }

    private void updateWriteResponse(OSDWriteResponse r) {
        if (r.getNew_file_size().size() > 0) {
           final NewFileSize nfs = r.getNew_file_size().get(0);
           if (wresp == null) {
               wresp = r;
           } else {
               final NewFileSize ofs = wresp.getNew_file_size().get(0);
               if ((nfs.getSize_in_bytes() > ofs.getSize_in_bytes()) && (nfs.getTruncate_epoch() == ofs.getTruncate_epoch())
                       || (nfs.getTruncate_epoch() > ofs.getTruncate_epoch()) ) {
                   wresp = r;
               }
           }
        }
    }

    /**
     *
     * @param resultBuffer
     *            - the buffer into which the data is read.
     * @param offset
     *            - the start offset of the data.
     * @param bytesToRead
     *            - the maximum number of bytes read.
     * @return - the total number of bytes read into the buffer, or -1 if there
     *         is no more data because the end of the file has been reached.
     * @throws Exception
     * @throws IOException
     */
    public int read(byte[] resultBuffer, int offset, int bytesToRead) throws Exception {
    
        int tmp = byteMapper.read(resultBuffer, offset, bytesToRead, filePos);
        filePos += tmp;
        return tmp;
    }

    public ReusableBuffer readObject(long objectNo)
            throws IOException {
            return readObject(objectNo, 0, stripingPolicy.getStripeSizeForObject(objectNo));
    }

    /**
     *
     * @param objectNo
     *            - relative object number.
     * @param firstByteInObject
     *            - the first byte to be read.
     * @param bytesInObject
     *            - the maximal number of bytes to be read.
     * @return a ReusableBuffer containing the data which was read.
     */
    @Override
    public ReusableBuffer readObject(long objectNo, int offset, int length)
            throws IOException {
    
        checkCap();
    
        RPCResponse<ObjectData> response = null;
    
        List<ServiceUUID> osds = sortOSDs((new XLocations(fileCredentials.getXlocs()))
                .getOSDsForObject(objectNo));
    
        int size = 0;
        ObjectData data = null;
        ReusableBuffer buffer = null;
        for (ServiceUUID osd : osds) {
            try {
                response = osdClient.read(osd.getAddress(), fileId, fileCredentials, (long) objectNo, 0,
                        offset, length);
                data = response.get();

                if (data.getInvalid_checksum_on_osd()) {
                    throw new IOException("object " + objectNo + " has an invalid checksum");
                }

                // fill up with padding zeros
                if (data.getZero_padding() == 0) {
                    buffer = data.getData();
                } else {
                    final int dataSize = data.getData().capacity();
                    if (data.getData().enlarge(dataSize + data.getZero_padding())) {
                        data.getData().position(dataSize);
                        while (data.getData().hasRemaining())
                            data.getData().put((byte) 0);
                        buffer = data.getData();
                        buffer.position(0);
                    } else {
                        buffer = BufferPool.allocate(dataSize + data.getZero_padding());
                        buffer.put(data.getData());
                        BufferPool.free(data.getData());
                        while (data.getData().hasRemaining())
                            data.getData().put((byte) 0);
                        buffer.position(0);
                    }
                }
                break;
            } catch (ONCRPCException ex) {
                if (osds.lastIndexOf(osd) == osds.size() - 1) { // last osd in list
                    System.out.println(ex.toString());
                    ex.printStackTrace();
                    throw new IOException("cannot read object", ex);
                } else
                    continue;
            } catch (InterruptedException ex) {
                if (osds.lastIndexOf(osd) == osds.size() - 1) { // last osd in list
                    throw new IOException("cannot read object", ex);
                } else
                    continue;
            } finally {
                if (response != null) {
                    response.freeBuffers();
                }
            }
        }
        return buffer;
    }

    /**
     * Sorts the OSD list after a specific pattern (original, random, ...).
     * @param osds
     * @return
     */
    private List<ServiceUUID> sortOSDs(List<ServiceUUID> osds) {
        List<ServiceUUID> list = new ArrayList<ServiceUUID>(osds);
//        Collections.shuffle(list);
        return list;
    }

    public int checkObject(long objectNo) throws IOException {
        checkCap();

        RPCResponse<ObjectData> response = null;

        List<ServiceUUID> osds = sortOSDs((new XLocations(fileCredentials.getXlocs()))
                .getOSDsForObject(objectNo));

        int size = 0;
        ObjectData data = null;
        ReusableBuffer buffer = null;
        for (ServiceUUID osd : osds) {
            try {
                response = osdClient.check_object(osd.getAddress(), fileId, fileCredentials, objectNo, 0);
                data = response.get();

                if (data.getInvalid_checksum_on_osd()) {
                    throw new IOException("object " + objectNo + " has an invalid checksum");
                }

                size = data.getZero_padding();

                break;
            } catch (ONCRPCException ex) {
                if (osds.lastIndexOf(osd) == osds.size() - 1) { // last osd in list
                    throw new IOException("cannot read object: " + ex.toString(), ex);
                } else
                    continue;
            } catch (InterruptedException ex) {
                if (osds.lastIndexOf(osd) == osds.size() - 1) { // last osd in list
                    throw new IOException("cannot read object: " + ex.toString(), ex);
                } else
                    continue;
            } finally {
                if (response != null) {
                    response.freeBuffers();
                }
                if ((data != null) && (data.getData() != null)) {
                    BufferPool.free(data.getData());
                    data.setData(null);
                }
            }
        }
        return size;
    }
    /**
     * Writes bytesToWrite bytes from the writeFromBuffer byte array starting at
     * offset to this file.
     *
     * @param writeFromBuffer
     * @param offset
     * @param bytesToWrite
     * @return the number of bytes written
     * @throws Exception
     */
    public int write(byte[] writeFromBuffer, int offset, int bytesToWrite) throws Exception {

        int tmp = byteMapper.write(writeFromBuffer, offset, bytesToWrite, filePos);
        filePos += bytesToWrite;
        return tmp;
    }

    /**
     * Writes...
     *
     * @param firstByteInObject
     *            - the start offset in the file
     * @param objectNo
     *            - the relative object number
     * @param data
     *            - the data to be written.....
     */
    public void writeObject(long firstByteInObject, long objectNo, ReusableBuffer data)
            throws IOException {

        // check whether capability needs to be renewed
        checkCap();

        if (isReadOnly)
            throw new IOException("File is marked as read-only. You cannot write anymore.");

        RPCResponse<OSDWriteResponse> response = null;
        try {
            ServiceUUID osd = currentReplica.getOSDs().get(stripingPolicy.getOSDforObject(objectNo));
            ObjectData odata = new ObjectData(data, 0, 0, false);
            response = osdClient.write(osd.getAddress(), fileId, fileCredentials, objectNo, 0, (int)firstByteInObject, 0, odata);
            OSDWriteResponse owr = response.get();
            this.updateWriteResponse(owr);
        } catch (ONCRPCException ex) {
            throw new IOException("cannot write object",ex);
        } catch (InterruptedException ex) {
            throw new IOException("cannot write object",ex);
        } finally {
            if (response != null)
                response.freeBuffers();
        }

    }

    public void flush() throws IOException {
        if (wresp != null) {
            RPCResponse r = null;
            try {
                r = mrcClient.xtreemfs_update_file_size(mrcAddress, fileCredentials.getXcap(), wresp);
                r.get();
                wresp = null;
            } catch (ONCRPCException ex) {
                throw new IOException("cannot write object",ex);
            } catch (InterruptedException ex) {
                throw new IOException("cannot write object",ex);
            } finally {
                if (r != null)
                    r.freeBuffers();
            }
        }
    }

    public void delete() throws Exception {
        checkCap();

        if (fileCredentials.getXlocs().getReplicas().size() == 1) {
            RPCResponse<FileCredentialsSet> r = null;
            RPCResponse delR = null;
            try {
                r = mrcClient.unlink(mrcAddress, credentials,pathName);
                FileCredentialsSet fcreds = r.get();
                if (fcreds.size() > 0) {
                    //must delete on OSDs too!
                    final FileCredentials delCred = fcreds.get(0);
                    for (ServiceUUID osd : currentReplica.getOSDs()) {
                        delR = osdClient.unlink(osd.getAddress(), fileId, delCred);
                        delR.get();
                        delR.freeBuffers();
                    }
                }
            } catch (ONCRPCException ex) {
                throw new IOException("cannot write object",ex);
            } catch (InterruptedException ex) {
                throw new IOException("cannot write object",ex);
            } finally {
                if (r != null)
                    r.freeBuffers();
            }
        } else {
            throw new IOException("There is more than 1 replica existing. Delete all replicas first.");
        }
    }

    public long length() throws IOException {
        RPCResponse<Stat> r = null;
        try {
            r = mrcClient.getattr(mrcAddress, credentials, pathName);
            Stat statInfo = r.get();

            //decide what to use...
            if (wresp != null) {
                final NewFileSize localFS = wresp.getNew_file_size().get(0);

                //check if we know a larger file size locally
                if (localFS.getTruncate_epoch() < statInfo.getTruncate_epoch())
                    return statInfo.getSize();
                if (localFS.getSize_in_bytes() > statInfo.getSize())
                    return localFS.getSize_in_bytes();
            }
            return statInfo.getSize();
        } catch (ONCRPCException ex) {
            throw new IOException("cannot write object",ex);
        } catch (InterruptedException ex) {
            throw new IOException("cannot write object",ex);
        } finally {
            if (r != null)
                r.freeBuffers();
        }


    }

    public void close() throws IOException {
        flush();
    }

    public void setReadOnly(boolean mode) throws Exception {
        if (isReadOnly == mode)
            return;

        try {
            if (mode) {
                flush();

                // set read only
                RPCResponse<setxattrResponse> r = mrcClient.setxattr(mrcAddress, credentials, pathName,
                        "xtreemfs.read_only", "true", 0);
                r.get();
                r.freeBuffers();

                forceXCapUpdate();

                // get correct filesize
                RPCResponse<Long> r2 = osdClient.internal_get_file_size(currentReplica.getHeadOsd()
                        .getAddress(), fileId, fileCredentials);
                long filesize = r2.get();
                r2.freeBuffers();

                // set filesize on mrc
                forceFileSize(filesize);

                // TODO: maybe request all OSDs to inform them that the file is read-only now

                forceFileCredentialsUpdate(translateMode("r"));
            } else {
                if (fileCredentials.getXlocs().getReplicas().size() > 1)
                    throw new IOException("File has still replicas.");
                else {
                    // set read only
                    RPCResponse<setxattrResponse> r = mrcClient.setxattr(mrcAddress, credentials, pathName,
                            "xtreemfs.read_only", "false", 0);
                    r.get();
                    r.freeBuffers();

                    forceFileCredentialsUpdate(this.mode);
                }
            }
        } catch (ONCRPCException ex) {
            throw new IOException("cannot change objects read-only-state", ex);
        } catch (InterruptedException ex) {
            throw new IOException("cannot change objects read-only-state", ex);
        }
    }

    public void addReplica(List<ServiceUUID> osds, StripingPolicy spPolicy) throws Exception {
        XLocations xLoc = new XLocations(fileCredentials.getXlocs());

        // check correct parameters
        if (osds.size() != spPolicy.getWidth())
            throw new IllegalArgumentException("Too many or less OSDs in list.");
        for (ServiceUUID osd : osds) {
            if (xLoc.containsOSD(osd)) // OSD is used for any replica so far
                throw new IllegalArgumentException(
                        "At least one OSD from list is already used for this file.");
        }

        if (isReadOnly) {
            StringSet osdSet = new StringSet();
            for (ServiceUUID osd : osds) {
                osdSet.add(osd.toString());
            }

            org.xtreemfs.interfaces.Replica newReplica = new org.xtreemfs.interfaces.Replica(spPolicy, 0,
                    osdSet);
            RPCResponse r = mrcClient.xtreemfs_replica_add(mrcAddress, credentials, fileId, newReplica);
            r.get();
            r.freeBuffers();

            forceFileCredentialsUpdate(translateMode("r"));
        } else
            throw new IOException("file is not marked as read-only.");
    }

    public void removeReplica(Replica replica) throws Exception {
            removeReplica(replica.getHeadOsd());
    }

    public void removeReplica(ServiceUUID osd) throws Exception {
        if (isReadOnly) {
            RPCResponse<XCap> r = mrcClient
                    .xtreemfs_replica_remove(mrcAddress, credentials, fileId, osd.toString());
            XCap deleteCap = r.get();
            r.freeBuffers();
            
            RPCResponse r2 = osdClient.unlink(osd.getAddress(), fileId, new FileCredentials(fileCredentials.getXlocs(),
                    deleteCap));
            r2.get();
            r2.freeBuffers();

            forceFileCredentialsUpdate(translateMode("r"));
        } else
            throw new IOException("file is not marked as read-only.");
    }

    /**
     * removes "all" replicas, so that only one (the first) replica exists
     */
    public void removeAllReplicas() throws Exception {
        XLocations xLoc = new XLocations(fileCredentials.getXlocs());
        List<Replica> replicas = xLoc.getReplicas();
        for (int i = 1; i < replicas.size(); i++) {
            removeReplica(replicas.get(i));
        }
    }

    public List<ServiceUUID> getSuitableOSDsForAReplica() throws Exception {
        XLocations xLoc = new XLocations(fileCredentials.getXlocs());

        RPCResponse<StringSet> r = mrcClient.xtreemfs_get_suitable_osds(mrcAddress, fileId);
        StringSet osds = r.get();
        r.freeBuffers();

        ArrayList<ServiceUUID> osdList = new ArrayList<ServiceUUID>();
        for (String osd : osds) {
           ServiceUUID uuid = new ServiceUUID(osd);
           if (!xLoc.containsOSD(uuid)) // OSD is not used for any replica so far
               osdList.add(uuid);
        }
        return osdList;
    }

    /**
     * returns the StripingPolicy of the first replica (but at the moment it is the same for all replicas)
     * @return
     */
    public StripingPolicy getStripingPolicy() {
        return stripingPolicy.getPolicy();
    }

    public long getStripeSize() {
        // the stripe size of a file is constant.
        return stripingPolicy.getStripeSizeForObject(0);
    }

    public XLocations getXLoc() {
        return new XLocations(fileCredentials.getXlocs());
    }
    
    public boolean isReadOnly() {
        return isReadOnly;
    }

    public long noOfObjects() throws Exception {
        // all replicas have the same striping policy at the moment
        return (length() / stripingPolicy.getStripeSizeForObject(0)) + 1;
    }

    /**
     * uses only the OSDs of the first replica
     * @return
     */
    public ServiceUUID getOSDId(long objectNo) {
        // FIXME: use more than only the first replica
        return currentReplica.getOSDs().get(stripingPolicy.getOSDforObject(objectNo));
    }


    public String getFileId() {
        return fileId;
    }

    public String getPath() {
        return pathName;
    }

    public void seek(long pos) {
        filePos = pos;
    }

    public long getFilePointer() {
        return filePos;
    }

    public String getStripingPolicyAsString() {
        return fileCredentials.getXlocs().getReplicas().get(0).toString();
    }

    public FileCredentials getCredentials() {
        return this.fileCredentials;
    }

    private void checkCap() throws IOException {

        long time = System.currentTimeMillis();

        if (time - capTime > (Capability.DEFAULT_VALIDITY - 60) * 1000) {
            try {
                forceXCapUpdate();
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
    }

    private void forceXCapUpdate() throws IOException {
        // update Xcap
        try {
            RPCResponse<XCap> r = mrcClient.xtreemfs_renew_capability(mrcAddress, fileCredentials.getXcap());
            XCap cap = r.get();
            r.freeBuffers();
            
            fileCredentials.setXcap(cap);

            capTime = System.currentTimeMillis();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    /**
     * @throws ONCRPCException
     * @throws IOException
     * @throws InterruptedException
     */
    private void forceFileCredentialsUpdate(int mode) throws ONCRPCException, IOException, InterruptedException {
        RPCResponse<FileCredentials> r = mrcClient.open(mrcAddress, credentials, pathName, FileAccessManager.O_CREAT, mode, 0);
        fileCredentials = r.get();
        r.freeBuffers();
        
        isReadOnly = fileCredentials.getXlocs().getRepUpdatePolicy().equals(Constants.REPL_UPDATE_PC_RONLY);
    }

    public void forceFileSize(long newFileSize) throws IOException {
        NewFileSizeSet newfsset = new NewFileSizeSet();
        newfsset.add(new NewFileSize(newFileSize, fileCredentials.getXcap().getTruncate_epoch()));
        OSDWriteResponse wr = new OSDWriteResponse(newfsset, new OSDtoMRCDataSet());
        RPCResponse r = null;
        try {
            r = mrcClient.xtreemfs_update_file_size(mrcAddress, fileCredentials.getXcap(), wr);
            r.get();
        } catch (ONCRPCException ex) {
            throw new IOException("cannot update file size",ex);
        } catch (InterruptedException ex) {
            throw new IOException("cannot update file size",ex);
        } finally {
            if (r != null)
                r.freeBuffers();
        }
    }
}
