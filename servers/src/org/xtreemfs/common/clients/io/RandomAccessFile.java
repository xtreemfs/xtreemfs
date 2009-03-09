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
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.auth.NullAuthProvider;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
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
import org.xtreemfs.interfaces.ObjectData;
import org.xtreemfs.interfaces.StripingPolicy;
import org.xtreemfs.interfaces.XCap;
import org.xtreemfs.interfaces.stat_;
import org.xtreemfs.interfaces.utils.ONCRPCException;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.client.MRCClient;
import org.xtreemfs.new_osd.client.OSDClient;
import org.xtreemfs.utils.utils;

public class RandomAccessFile implements ObjectStore {

    private MRCClient mrcClient;

    private OSDClient osdClient;

    private FileCredentials fcred;

    // all replicas have the same striping policy at the moment
    private StripingPolicyImpl stripingPolicy;

    private final String fileId;

    private final String pathName;

    private final InetSocketAddress mrcAddress;

    private ByteMapper byteMapper;

    private OSDWriteResponse wresp;

    private long filePos;

    private Map<String, String> capAndXLoc;

    private long capTime;

    private final String userID;

    private final List<String> groupIDs;

    private Replica            currentReplica;

    private final boolean      isReadOnly;
    
    public RandomAccessFile(String mode, InetSocketAddress mrcAddress, String pathName,
            RPCNIOSocketClient rpcClient, 
            String userID, List<String> groupIDs) throws ONCRPCException,InterruptedException,IOException {
        this.pathName = pathName;
        this.mrcAddress = mrcAddress;
        this.userID = userID;
        this.groupIDs = groupIDs;

        assert (rpcClient != null);

        // use the shared speedy to create an MRC and OSD client
        mrcClient = new MRCClient(rpcClient, mrcAddress);
        osdClient = new OSDClient(rpcClient);

        // create a new file if necessary

        RPCResponse<FileCredentials> r = mrcClient.open(mrcAddress, userID, groupIDs, pathName, FileAccessManager.O_CREAT, translateMode(mode));
        fcred = r.get();
        r.freeBuffers();


        // all replicas have the same striping policy at the moment
        stripingPolicy = StripingPolicyImpl.getPolicy(fcred.getXlocs().getReplicas().get(0));
        XLocations loc = new XLocations(fcred.getXlocs());
        currentReplica = loc.getReplica(0);
        byteMapper = ByteMapperFactory.createByteMapper(
                stripingPolicy.getPolicyId(), stripingPolicy.getStripeSizeForObject(0), this);

        capTime = System.currentTimeMillis();

        isReadOnly = fcred.getXlocs().getRepUpdatePolicy().equals(Constants.REPL_UPDATE_PC_RONLY);

        fileId = fcred.getXcap().getFile_id();
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

    /**
     *
     * @param objectNo
     *            - relative object number.
     * @return the number of bytes in the object
     * @throws HttpErrorException
     * @throws IOException
     * @throws JSONException
     * @throws InterruptedException
     */
    /*public int readObject(int objectNo) throws HttpErrorException, IOException, JSONException,
            InterruptedException {
        // check whether capability needs to be renewed
        checkCap();

        RPCResponse<ObjectData> response = null;

        ServiceUUID osd = currentReplica.getOSDs().get(sp.getOSDforObject(objectNo));

        int size = 0;
        ObjectData data = null;
        try {

            response = osdClient.read(mrcAddress, fileId, fcred, (long)objectNo, 0, 0, sp.getStripeSizeForObject(objectNo));
            data = response.get();

            if (data.getInvalid_checksum_on_osd()) {
                throw new IOException("object " + objectNo + " has an invalid checksum");
            }

            response.getBody().flip();
            data = response.getBody();
            if (data != null) {
                size = data.limit();
            }
            break;
        } catch (IOException e) {
            continue; // try next OSD
        } finally {
            if (response != null) {
                response.freeBuffers();
            }
            if (data != null)
                BufferPool.free(data.getData());
        }
        return size;
    }*/

    /**
     * Sorts the OSD list after a specific pattern (original, random, ...).
     * @param osds
     * @return
     */
    private List<ServiceUUID> sortOSDs(List<ServiceUUID> osds) {
        // TODO Auto-generated method stub
        return osds;
    }

    public ReusableBuffer readObject(long objectNo)
            throws IOException {
            return readObject(objectNo, 0, stripingPolicy.getStripeSizeForObject(objectNo));
    }

    public int checkObject(long objectNo) throws IOException {
        checkCap();

        RPCResponse<ObjectData> response = null;

        ServiceUUID osd = currentReplica.getOSDs().get(stripingPolicy.getOSDforObject(objectNo));

        int size = 0;
        ObjectData data = null;
        ReusableBuffer buffer = null;
        try {

            response = osdClient.check_object(osd.getAddress(), fileId, fcred, objectNo, 0);
            data = response.get();

            if (data.getInvalid_checksum_on_osd()) {
                throw new IOException("object " + objectNo + " has an invalid checksum");
            }

            size = data.getZero_padding();

        } catch (ONCRPCException ex) {
            throw new IOException("cannot read object: "+ex.toString(), ex);
        } catch (InterruptedException ex) {
            throw new IOException("cannot read object: "+ex.toString(), ex);
        } finally {
            if (response != null) {
                response.freeBuffers();
            }
        }
        return size;
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

        ServiceUUID osd = currentReplica.getOSDs().get(stripingPolicy.getOSDforObject(objectNo));

        int size = 0;
        ObjectData data = null;
        ReusableBuffer buffer = null;
        try {
            System.out.println("read: "+objectNo+" of="+offset+" len="+length);
            response = osdClient.read(osd.getAddress(), fileId, fcred, (long)objectNo, 0, offset,length);
            data = response.get();

            if (data.getInvalid_checksum_on_osd()) {
                throw new IOException("object " + objectNo + " has an invalid checksum");
            }

            //fill up with padding zeros
            if (data.getZero_padding() == 0) {
                buffer = data.getData();
            } else {
                final int dataSize = data.getData().capacity();
                if (data.getData().enlarge(dataSize+data.getZero_padding())) {
                    data.getData().position(dataSize);
                    while (data.getData().hasRemaining())
                        data.getData().put((byte)0);
                    buffer = data.getData();
                    buffer.position(0);
                } else {
                    buffer = BufferPool.allocate(dataSize+data.getZero_padding());
                    buffer.put(data.getData());
                    BufferPool.free(data.getData());
                    while (data.getData().hasRemaining())
                        data.getData().put((byte)0);
                    buffer.position(0);
                }
            }

        } catch (ONCRPCException ex) {
            System.out.println(ex.toString());
            ex.printStackTrace();
            throw new IOException("cannot read object", ex);
        } catch (InterruptedException ex) {
            throw new IOException("cannot read object", ex);
        } finally {
            if (response != null) {
                response.freeBuffers();
            }
        }
        return buffer;
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
            ObjectData odata = new ObjectData("", 0, false, data);
            response = osdClient.write(osd.getAddress(), fileId, fcred, objectNo, 0, (int)firstByteInObject, 0, odata);
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
                r = mrcClient.xtreemfs_update_file_size(mrcAddress, fcred.getXcap(), wresp);
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

        if (fcred.getXlocs().getReplicas().size() == 1) {
            RPCResponse<FileCredentialsSet> r = null;
            RPCResponse delR = null;
            try {
                r = mrcClient.unlink(mrcAddress, userID,groupIDs,pathName);
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
        RPCResponse<stat_> r = null;
        try {
            r = mrcClient.getattr(mrcAddress, userID, groupIDs, pathName);
            stat_ statInfo = r.get();

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
        /*Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("read_only", mode ? "true" : "false");
        mrcClient.setXAttrs(mrcAddress, pathName, attrs, authString);
         * */
        //FIXME: set FS on mrc
        //FIXME: set read only
    }

    /*public void addReplica(List<ServiceUUID> osds) throws Exception {
        List<String> osdList = new ArrayList<String>();
        for (ServiceUUID osd : osds) {
            osdList.add(osd.toString());
        }
        mrcClient.addReplica(mrcAddress, fileId, stripingPolicy.asMap(), osdList, authString);

        // update Xloc
        try {
            capAndXLoc = mrcClient.renew(mrcAddress, capAndXLoc, authString);
            capTime = System.currentTimeMillis();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public void removeReplica(List<ServiceUUID> osds) throws Exception {
        List<String> osdList = new ArrayList<String>();
        for (ServiceUUID osd : osds) {
            osdList.add(osd.toString());
        }
        mrcClient.removeReplica(mrcAddress, fileId, stripingPolicy.asMap(), osdList, authString);

        // update Xloc
        try {
            capAndXLoc = mrcClient.renew(mrcAddress, capAndXLoc, authString);
            capTime = System.currentTimeMillis();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public String getStripingPolicy() {
        return stripingPolicy.toString();
    }*/

    public long getStripeSize() {
        // the stripe size of a file is constant.
        return stripingPolicy.getStripeSizeForObject(0);
    }

//    /**
//     * uses only the OSDs of the first replica
//     * @return
//     */
//    public List<ServiceUUID> getOSDs() {
//        // FIXME: use more than only the first replica
//        return locations.getLocation(0).getOSDs();
//    }
//
//    public long noOfObjects() throws Exception {
//        // all replicas have the same striping policy at the moment
//        return (length() / locations.getLocation(0).getStripingPolicy().getStripeSize(0)) + 1;
//    }
//
//    /**
//     * uses only the OSDs of the first replica
//     * @return
//     */
//    public ServiceUUID getOSDId(long objectNo) {
//        // FIXME: use more than only the first replica
//        return locations.getOSDsByObject(objectNo).get(0);
//    }


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
        return fcred.getXlocs().getReplicas().get(0).toString();
    }

    private void checkCap() throws IOException {

        long time = System.currentTimeMillis();

        if (time - capTime > (Capability.DEFAULT_VALIDITY - 60) * 1000) {
            try {
                RPCResponse<XCap> r = mrcClient.xtreemfs_renew_capability(mrcAddress, fcred.getXcap());
                XCap cap = r.get();
                r.freeBuffers();

                fcred.setXcap(cap);

                capTime = time;
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
    }
}
