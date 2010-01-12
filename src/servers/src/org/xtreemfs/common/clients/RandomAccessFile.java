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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.clients.internal.ObjectMapper;
import org.xtreemfs.common.clients.internal.ObjectMapper.ObjectRequest;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.common.xloc.Replica;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.foundation.oncrpc.client.RPCResponseAvailableListener;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.FileCredentials;
import org.xtreemfs.interfaces.NewFileSize;
import org.xtreemfs.interfaces.NewFileSizeSet;
import org.xtreemfs.interfaces.OSDWriteResponse;
import org.xtreemfs.interfaces.OSDtoMRCDataSet;
import org.xtreemfs.interfaces.ObjectData;
import org.xtreemfs.interfaces.ObjectList;
import org.xtreemfs.interfaces.XLocSet;
import org.xtreemfs.interfaces.utils.ONCRPCException;
import org.xtreemfs.osd.client.OSDClient;
import org.xtreemfs.osd.replication.ObjectSet;

/**
 *
 * @author bjko
 */
public class RandomAccessFile {

    private final File parent;
    private final OSDClient osdClient;
    private final Volume parentVolume;
    private final boolean readOnly;
    private final boolean syncMetadata;
    private long position;
    private FileCredentials credentials;
    private Replica currentReplica;
    private int currentReplicaNo;
    private final int numReplicas;
    private final String fileId;
    private ObjectMapper oMapper;
    private boolean closed;

    RandomAccessFile(File parent, Volume parentVolume, OSDClient client, FileCredentials fc, boolean readOnly, boolean syncMetadata) {
        this.parent = parent;
        this.parentVolume = parentVolume;
        this.osdClient = client;
        this.credentials = fc;
        this.readOnly = readOnly;
        this.syncMetadata = syncMetadata;
        position = 0;
        currentReplicaNo = 0;
        closed = false;

        numReplicas = credentials.getXlocs().getReplicas().size();
        fileId = fc.getXcap().getFile_id();

        selectReplica(0);
    }

    protected void switchToNextReplica() {
        selectReplica((currentReplicaNo + 1) % numReplicas);
        
    }

    protected void selectReplica(int replicaNo) {
        currentReplicaNo = replicaNo;
        currentReplica = new Replica(credentials.getXlocs().getReplicas().get(currentReplicaNo), null);
        oMapper = ObjectMapper.getMapper(currentReplica.getStripingPolicy().getPolicy());
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, this,"now using replica %d (%s)",replicaNo,credentials.getXlocs().getReplicas());
    }
    
    public XLocSet getLocationsList() {
        return credentials.getXlocs();
    }

    public int getCurrentReplicaStripeSize() {
        return currentReplica.getStripingPolicy().getStripeSizeForObject(0);
    }

    public int getCurrentReplicaStripeingWidth() {
        return currentReplica.getStripingPolicy().getWidth();
    }

    public void forceReplica(int replicaNo) {
        if ((replicaNo > numReplicas-1) || (replicaNo < 0))
            throw new IllegalArgumentException("invalid replica number");
        selectReplica(replicaNo);
    }

    public void forceReplica(String headOSDuuid) {
        final int numRepl = credentials.getXlocs().getReplicas().size();
        for (int i = 0; i < numRepl; i++) {
            org.xtreemfs.interfaces.Replica r = credentials.getXlocs().getReplicas().get(i);
            if (r.getOsd_uuids().get(0).equals(headOSDuuid)) {
                selectReplica(i);
                return;
            }
        }
        throw new IllegalArgumentException("osd not in any of the replicas");
    }

    public int getCurrentReplica() {
        return currentReplicaNo;
    }

    public String getFileId() {
        return credentials.getXcap().getFile_id();
    }

    public File getFile() {
        return parent;
    }

    public boolean isReadOnly() {
        return this.readOnly;
    }

    public int read(byte[] data, int offset, int length) throws IOException {
        ReusableBuffer buf = ReusableBuffer.wrap(data, offset, length);
        return read(buf);
    }

    public int read(ReusableBuffer data) throws IOException {

        if (closed) {
            throw new IllegalStateException("file was closed");
        }

        int numTries = 0;
        IOException cause = null;
        do {

            List<ObjectRequest> ors = oMapper.readRequest(data.remaining(), position, currentReplica);

            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, this,"read from file %s: %d bytes from offset %d (= %d obj rqs)",fileId,data.remaining(),position,ors.size());

            if (ors.size() == 0) {
                return 0;
            }

            final AtomicInteger responseCnt = new AtomicInteger(ors.size());
            RPCResponse[] resps = new RPCResponse[ors.size()];

            try {


                final RPCResponseAvailableListener rl = new RPCResponseAvailableListener<ObjectData>() {

                    @Override
                    public void responseAvailable(RPCResponse<ObjectData> r) {
                        if (responseCnt.decrementAndGet() == 0) {
                            //last response
                            synchronized (responseCnt) {
                                responseCnt.notify();
                            }
                        }
                    }
                };

                for (int i = 0; i < ors.size(); i++) {
                    ObjectRequest or = ors.get(i);
                    ServiceUUID osd = new ServiceUUID(or.getOsdUUID(), parentVolume.uuidResolver);
                    RPCResponse<ObjectData> r = osdClient.read(osd.getAddress(),
                            fileId, credentials, or.getObjNo(), -1, or.getOffset(), or.getLength());
                    resps[i] = r;
                    r.registerListener(rl);
                }


                synchronized (responseCnt) {
                    if (responseCnt.get() > 0) {
                        responseCnt.wait();
                    }
                }


                //assemble responses
                ObjectData[] ods = new ObjectData[ors.size()];
                for (int i = 0; i < ors.size(); i++) {
                    ods[i] = (ObjectData) resps[i].get();
                }

                int numBytesRead = 0;

                for (ObjectData od : ods) {
                    if (od.getData() != null) {
                        numBytesRead += od.getData().remaining();
                        data.put(od.getData());
                        BufferPool.free(od.getData());
                    }
                    if (od.getZero_padding() > 0) {
                        numBytesRead += od.getZero_padding();
                        for (int i = 0; i < od.getZero_padding(); i++)
                            data.put((byte)0);
                    }
                }


                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, this,"read returned %d bytes",numBytesRead);

                position += numBytesRead;

                return numBytesRead;

            } catch (InterruptedException ex) {
                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, this,"comm error: %s",ex.toString());
                cause = new IOException("operation aborted", ex);
            } catch (ONCRPCException ex) {
                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, this,"comm error: %s",ex.toString());
                cause = new IOException("communication failure", ex);
            } catch (IOException ex) {
                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, this,"comm error: %s",ex.toString());
                cause = ex;
            } catch (Throwable th) {
                th.printStackTrace();
                throw new IOException("nasty!");
            } finally {
                for (RPCResponse r : resps)
                    r.freeBuffers();
            }
            numTries++;
            switchToNextReplica();
        } while (numTries < numReplicas);
        throw cause;
    }


    public int checkObject(long objectNo) throws IOException {

        if (closed) {
            throw new IllegalStateException("file was closed");
        }


        RPCResponse<ObjectData> r = null;
        try {

            ServiceUUID osd = new ServiceUUID(currentReplica.getOSDForObject(objectNo).toString(), parentVolume.uuidResolver);

            r = osdClient.check_object(osd.getAddress(),
                    fileId, credentials, objectNo, 0l);
            ObjectData od = r.get();


            if (od.getInvalid_checksum_on_osd()) {
                // try next replica
                throw new InvalidChecksumException("object " + objectNo + " has an invalid checksum");
            }

            return od.getZero_padding();


        } catch (InterruptedException ex) {
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, this,"comm error: %s",ex.toString());
            throw new IOException("operation aborted", ex);
        } catch (ONCRPCException ex) {
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, this,"comm error: %s",ex.toString());
            throw new IOException("communication failure", ex);
        } finally {
            if (r != null)
                r.freeBuffers();
        }
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
    public int write(byte[] data, int offset, int length) throws IOException {
        ReusableBuffer buf = ReusableBuffer.wrap(data, offset, length);
        return write(buf);
    }

    public int write(ReusableBuffer data) throws IOException {

        if (readOnly) {
            throw new IOException("File is marked as read-only.");
        }
        if (closed) {
            throw new IllegalStateException("file was closed");
        }

        List<ObjectRequest> ors = oMapper.writeRequest(data, position, currentReplica);

        if (ors.size() == 0) {
            return 0;
        }

        final AtomicInteger responseCnt = new AtomicInteger(ors.size());
        RPCResponse[] resps = new RPCResponse[ors.size()];

        try {


            final RPCResponseAvailableListener rl = new RPCResponseAvailableListener<OSDWriteResponse>() {

                @Override
                public void responseAvailable(RPCResponse<OSDWriteResponse> r) {
                    if (responseCnt.decrementAndGet() == 0) {
                        //last response
                        synchronized (responseCnt) {
                            responseCnt.notify();
                        }
                    }
                }
            };

            int bytesWritten = 0;
            for (int i = 0; i < ors.size(); i++) {
                ObjectRequest or = ors.get(i);
                bytesWritten += or.getData().capacity();
                ServiceUUID osd = new ServiceUUID(or.getOsdUUID(), parentVolume.uuidResolver);
                RPCResponse<OSDWriteResponse> r = osdClient.write(osd.getAddress(),
                        fileId, credentials, or.getObjNo(), -1, or.getOffset(), 0l,
                        new ObjectData(0, false, 0, or.getData()));
                resps[i] = r;
                r.registerListener(rl);
            }


            synchronized (responseCnt) {
                if (responseCnt.get() > 0) {
                    responseCnt.wait();
                }
            }


            //assemble responses
            OSDWriteResponse owr = null;
            for (int i = 0; i < ors.size(); i++) {
                owr = (OSDWriteResponse) resps[i].get();
            }
            parentVolume.storeFileSizeUpdate(fileId, owr);
            if (syncMetadata) {
                parentVolume.pushFileSizeUpdate(fileId);
            }
            data.flip();

            position += bytesWritten;

            return bytesWritten;

        } catch (InterruptedException ex) {
            throw new IOException("operation aborted", ex);
        } catch (ONCRPCException ex) {
            throw new IOException("communication failure", ex);
        } finally {
            for (RPCResponse r : resps)
                r.freeBuffers();
        }
    }

    public void seek(long position) {
        this.position = position;
    }

    public long getFilePointer() {
        return position;
    }

    public void close() throws IOException {
        this.closed = true;
        parentVolume.closeFile(fileId, readOnly);
    }

    public void fsync() throws IOException {
        parentVolume.pushFileSizeUpdate(fileId);
    }

    public long length() throws IOException {
        return parent.length();
    }

    public long getNumObjects() throws IOException {
        long flength = length();
        if (flength > 0)
            return currentReplica.getStripingPolicy().getObjectNoForOffset(flength-1)+1;
        else
            return 0;
    }

    public void forceFileSize(long newFileSize) throws IOException {
        NewFileSizeSet newfsset = new NewFileSizeSet();
        newfsset.add(new NewFileSize(newFileSize, credentials.getXcap().getTruncate_epoch()));
        OSDWriteResponse wr = new OSDWriteResponse(newfsset, new OSDtoMRCDataSet());
        parentVolume.storeFileSizeUpdate(fileId, wr);
        parentVolume.pushFileSizeUpdate(fileId);
    }

    long getFileSizeOnOSD() throws IOException {
        if (closed) {
            throw new IllegalStateException("file was closed");
        }


        RPCResponse<Long> r = null;
        try {

            ServiceUUID osd = new ServiceUUID(currentReplica.getHeadOsd().toString(), parentVolume.uuidResolver);

            r = osdClient.internal_get_file_size(osd.getAddress(), fileId, credentials);

            return r.get();

        } catch (InterruptedException ex) {
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, this,"comm error: %s",ex.toString());
            throw new IOException("operation aborted", ex);
        } catch (ONCRPCException ex) {
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, this,"comm error: %s",ex.toString());
            throw new IOException("communication failure", ex);
        } finally {
            if (r != null)
                r.freeBuffers();
        }
    }

    boolean isCompleteReplica(int replicaNo) throws IOException {
        if (closed) {
            throw new IllegalStateException("file was closed");
        }


        RPCResponse<ObjectList> r = null;
        try {

            org.xtreemfs.interfaces.Replica replica = this.credentials.getXlocs().getReplicas().get(replicaNo);
            if ((replica.getReplication_flags() & Constants.REPL_FLAG_IS_COMPLETE) > 0) {
                return true;
            }
            StripingPolicyImpl sp = StripingPolicyImpl.getPolicy(replica, 0);
            long lastObjectNo = sp.getObjectNoForOffset(this.credentials.getXlocs().getRead_only_file_size());
            int osdRelPos = 0;
            for (String osdUUID : replica.getOsd_uuids()) {
                ServiceUUID osd = new ServiceUUID(osdUUID, parentVolume.uuidResolver);

                r = osdClient.internal_getObjectList(osd.getAddress(), fileId, credentials);
                ObjectList ol = r.get();
                ReusableBuffer set = ol.getSet();
                r.freeBuffers();
                r = null;

                byte[] serializedBitSet = new byte[set.capacity()];
                set.position(0);
                set.get(serializedBitSet);
                BufferPool.free(set);
                ObjectSet oset = null;
                try {
                     oset = new ObjectSet(replicaNo, replicaNo, serializedBitSet);
                } catch (Exception ex) {
                    throw new IOException("cannot deserialize object set: "+ex,ex);
                }
                for (long objNo = osdRelPos; objNo <= lastObjectNo; objNo += sp.getWidth()) {
                    if (oset.contains(objNo) == false)
                        return false;
                }

            }
            //FIXME: mark replica as complete
            try {
                parent.setxattr("xtreemfs.mark_replica_complete", replica.getOsd_uuids().get(0));
            } catch (Exception ex) {
                //only an optimization, ignore errors
            }
            return true;
        } catch (InterruptedException ex) {
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, this,"comm error: %s",ex.toString());
            throw new IOException("operation aborted", ex);
        } catch (ONCRPCException ex) {
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, this,"comm error: %s",ex.toString());
            throw new IOException("communication failure", ex);
        } finally {
            if (r != null)
                r.freeBuffers();
        }
    }

    public int getReplicaNumber(String headOSDuuid) {
        for (int i = 0; i < this.credentials.getXlocs().getReplicas().size(); i++) {
            org.xtreemfs.interfaces.Replica replica = this.credentials.getXlocs().getReplicas().get(i);
            if (replica.getOsd_uuids().contains(headOSDuuid))
                return i;
        }
        throw new IllegalArgumentException("osd '"+headOSDuuid+"' is not in any replica of this file");
    }
    
    /**
     * Triggers the initial replication of the current replica by reading the
     * first object on each OSD.
     */
    public void triggerInitialReplication() throws IOException {
        
        // send requests to all OSDs of this replica
        try {
            List<ServiceUUID> osdList = currentReplica.getOSDs();
            List<ServiceUUID> osdListCopy = new ArrayList<ServiceUUID>(currentReplica.getOSDs());
            // take lowest objects of file
            for (int objectNo = 0; osdListCopy.size() != 0; objectNo++) {
                // get index of OSD for this object
                ServiceUUID osd = currentReplica.getOSDForObject(objectNo);
                // remove OSD
                osdListCopy.remove(osd);
                // send request (read only 1 byte)
                RPCResponse<ObjectData> r = osdClient.read(osd.getAddress(), fileId, credentials, objectNo,
                    0, 0, 1);
                r.get();
                r.freeBuffers();
            }
        } catch (UnknownUUIDException e) {
            // ignore; should not happen
        } catch (IOException e) {
            throw new IOException("At least one OSD could not be contacted to replicate the file.", e);
        } catch (ONCRPCException ex) {
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "comm error: %s", ex.toString());
            throw new IOException("communication failure", ex);
        } catch (InterruptedException e) {
            // ignore
        }
    }

}
