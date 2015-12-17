/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.clients;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.xtreemfs.common.clients.internal.ObjectMapper;
import org.xtreemfs.common.clients.internal.ObjectMapper.ObjectRequest;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.common.xloc.Replica;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.PBRPCException;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.client.RPCResponseAvailableListener;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.osd.InternalObjectData;
import org.xtreemfs.osd.replication.ObjectSet;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDWriteResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.REPL_FLAG;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XCap;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XLocSet;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectData;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectList;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_internal_get_file_sizeResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;

/**
 *
 * @author bjko
 */
public class RandomAccessFile {

    public static final int WAIT_MS_BETWEEN_WRITE_SWITCHOVER = 1000;

    private final File parent;
    private final OSDServiceClient osdClient;
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
    private UserCredentials userCreds;

    private final AtomicReference<XCap> uptodateCap;

    RandomAccessFile(File parent, Volume parentVolume, OSDServiceClient client, FileCredentials fc, boolean readOnly, boolean syncMetadata, UserCredentials userCreds) {
        this.parent = parent;
        this.parentVolume = parentVolume;
        this.osdClient = client;
        this.credentials = fc;
        this.readOnly = readOnly;
        this.syncMetadata = syncMetadata;
        this.userCreds = userCreds;
        position = 0;
        currentReplicaNo = 0;
        closed = false;

        numReplicas = credentials.getXlocs().getReplicasCount();
        fileId = fc.getXcap().getFileId();

        uptodateCap = new AtomicReference(fc.getXcap());

        selectReplica(0);
    }

    public void updateCap(XCap newXCap) {
        uptodateCap.set(newXCap);
    }

    protected void setXCap() {
        credentials = credentials.toBuilder().setXcap(uptodateCap.get()).build();
    }

    protected void switchToNextReplica() {
        selectReplica((currentReplicaNo + 1) % numReplicas);
        
    }

    protected void selectReplica(int replicaNo) {
        currentReplicaNo = replicaNo;
        currentReplica = new Replica(credentials.getXlocs().getReplicas(currentReplicaNo), null);
        oMapper = ObjectMapper.getMapper(currentReplica.getStripingPolicy().getPolicy());
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, this,"now using replica %d (%s)",replicaNo,credentials.getXlocs().getReplicasList());
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
        final int numRepl = credentials.getXlocs().getReplicasCount();
        for (int i = 0; i < numRepl; i++) {
            org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica r = credentials.getXlocs().getReplicas(i);
            if (r.getOsdUuids(0).equals(headOSDuuid)) {
                selectReplica(i);
                return;
            }
        }
        throw new IllegalArgumentException("osd "+headOSDuuid+" not in any of the replicas: "+credentials.getXlocs().getReplicasList());
    }

    public int getCurrentReplica() {
        return currentReplicaNo;
    }

    public String getFileId() {
        return credentials.getXcap().getFileId();
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

            if (ors.isEmpty()) {
                return 0;
            }

            final AtomicInteger responseCnt = new AtomicInteger(ors.size());
            RPCResponse<ObjectData>[] resps = new RPCResponse[ors.size()];

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

                setXCap();
                for (int i = 0; i < ors.size(); i++) {
                    ObjectRequest or = ors.get(i);
                    ServiceUUID osd = new ServiceUUID(or.getOsdUUID(), parentVolume.uuidResolver);
                    RPCResponse<ObjectData> r = osdClient.read(osd.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService,
                            credentials, fileId, or.getObjNo(), -1, or.getOffset(), or.getLength());
                    resps[i] = r;
                    r.registerListener(rl);
                }


                synchronized (responseCnt) {
                    if (responseCnt.get() > 0) {
                        responseCnt.wait();
                    }
                }


                //assemble responses
                InternalObjectData[] ods = new InternalObjectData[ors.size()];
                for (int i = 0; i < ors.size(); i++) {
                    ods[i] = new InternalObjectData(resps[i].get(),resps[i].getData());
                }

                int numBytesRead = 0;

                for (InternalObjectData od : ods) {
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
            } catch (PBRPCException ex) {
                if (ex.getErrorType() == ErrorType.REDIRECT) {
                    if (Logging.isDebug())
                        Logging.logMessage(Logging.LEVEL_DEBUG, this,"redirected to: %s",ex.getRedirectToServerUUID());
                    forceReplica(ex.getRedirectToServerUUID());
                    continue;
                } else if (ex.getErrorType() == ErrorType.ERRNO) {
                    cause = ex;
                } else {
                    if (Logging.isDebug())
                        Logging.logMessage(Logging.LEVEL_DEBUG, this,"comm error: %s",ex.toString());
                    cause = new IOException("communication failure", ex);
                }
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
            try {
                Thread.sleep(WAIT_MS_BETWEEN_WRITE_SWITCHOVER);
            } catch (InterruptedException ex) {
                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, this,"comm error: %s",ex.toString());
                throw new IOException("operation aborted", ex);
            }
            switchToNextReplica();

        } while (numTries < numReplicas+parentVolume.getMaxRetries());
        throw cause;
    }


    public int checkObject(long objectNo) throws IOException {

        if (closed) {
            throw new IllegalStateException("file was closed");
        }


        RPCResponse<ObjectData> r = null;
        try {

            ServiceUUID osd = new ServiceUUID(currentReplica.getOSDForObject(objectNo).toString(), parentVolume.uuidResolver);

            setXCap();
            r = osdClient.xtreemfs_check_object(osd.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService,
                    credentials, fileId, objectNo, 0l);
            ObjectData od = r.get();

            if (od.getInvalidChecksumOnOsd()) {
                // try next replica
                throw new InvalidChecksumException("object " + objectNo + " has an invalid checksum");
            }

            return od.getZeroPadding();


        } catch (InterruptedException ex) {
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, this,"comm error: %s",ex.toString());
            throw new IOException("operation aborted", ex);
        } catch (PBRPCException ex) {
            if (ex.getErrorType() == ErrorType.ERRNO)
                throw ex;
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

        if (data.remaining() == 0) {
            return 0;
        }

        List<ObjectRequest> ors = oMapper.writeRequest(data, position, currentReplica);

        if (ors.isEmpty()) {
            return 0;
        }

        int numTries = 0;

        IOException cause = null;
        
        do {
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
                setXCap();
                ObjectData objData = ObjectData.newBuilder().setChecksum(0).setInvalidChecksumOnOsd(false).setZeroPadding(0).build();
                for (int i = 0; i < ors.size(); i++) {
                    ObjectRequest or = ors.get(i);
                    or.getData().position(0);
                    bytesWritten += or.getData().capacity();
                    ServiceUUID osd = new ServiceUUID(or.getOsdUUID(), parentVolume.uuidResolver);

                    RPCResponse<OSDWriteResponse> r = osdClient.write(osd.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService,
                            credentials, fileId, or.getObjNo(), -1, or.getOffset(), 0l,
                            objData, or.getData());
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
                setXCap();
                parentVolume.storeFileSizeUpdate(fileId, owr, userCreds);
                if (syncMetadata) {
                    parentVolume.pushFileSizeUpdate(fileId, userCreds);
                }
                data.flip();

                position += bytesWritten;

                return bytesWritten;

            } catch (InterruptedException ex) {
                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, this,"comm error: %s",ex.toString());
                cause = new IOException("operation aborted", ex);
            } catch (PBRPCException ex) {
                if (ex.getErrorType() == ErrorType.REDIRECT) {
                    if (Logging.isDebug())
                        Logging.logMessage(Logging.LEVEL_DEBUG, this,"redirected to: %s",ex.getRedirectToServerUUID());
                    forceReplica(ex.getRedirectToServerUUID());
                    continue;
                } else if (ex.getErrorType() == ErrorType.ERRNO) {
                    cause = ex;
                } else {
                    if (Logging.isDebug())
                        Logging.logMessage(Logging.LEVEL_DEBUG, this,"comm error: %s",ex.toString());
                    cause = new IOException("communication failure", ex);
                }
            } catch (IOException ex) {
                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, this,"comm error: %s",ex.toString());
                cause = ex;
            } finally {
                for (RPCResponse r : resps) {
                    if (r != null)
                        r.freeBuffers();
                }
            }
            numTries++;
            switchToNextReplica();
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, this,"write failed (%s), switched to replica: %s",cause,currentReplica);
            try {
                Thread.sleep(WAIT_MS_BETWEEN_WRITE_SWITCHOVER);
            } catch (InterruptedException ex) {
                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, this,"comm error: %s",ex.toString());
                throw new IOException("operation aborted", ex);
            }
        } while (numTries < numReplicas+parentVolume.getMaxRetries());
        throw cause;
    }

    public void seek(long position) {
        this.position = position;
    }

    public long getFilePointer() {
        return position;
    }

    public void close() throws IOException {
        this.closed = true;
        parentVolume.closeFile(this,fileId, readOnly, userCreds);
    }

    public void fsync() throws IOException {
        parentVolume.pushFileSizeUpdate(fileId, userCreds);
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
        
        XCap truncCap = parentVolume.truncateFile(fileId, userCreds);
       
        try {

            parentVolume.storeFileSizeUpdate(fileId, OSDWriteResponse.newBuilder()
                    .setSizeInBytes(newFileSize).setTruncateEpoch(truncCap.getTruncateEpoch()).build(),
                userCreds);
            parentVolume.pushFileSizeUpdate(fileId, userCreds);


            if (position > newFileSize)
                position = newFileSize;

        } catch (PBRPCException ex) {
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, this,"comm error: %s",ex.toString());
            throw new IOException("communication failure", ex);
        }
    }

    long getFileSizeOnOSD() throws IOException {
        if (closed) {
            throw new IllegalStateException("file was closed");
        }

        RPCResponse<xtreemfs_internal_get_file_sizeResponse> r = null;
        try {

            ServiceUUID osd = new ServiceUUID(currentReplica.getHeadOsd().toString(), parentVolume.uuidResolver);

            setXCap();
            r = osdClient.xtreemfs_internal_get_file_size(osd.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, credentials, fileId);

            return r.get().getFileSize();

        } catch (InterruptedException ex) {
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, this,"comm error: %s",ex.toString());
            throw new IOException("operation aborted", ex);
        } catch (PBRPCException ex) {
            if (ex.getErrorType() == ErrorType.REDIRECT) {
                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, this,"redirected to: %s",ex.getRedirectToServerUUID());
                forceReplica(ex.getRedirectToServerUUID());
                return getFileSizeOnOSD();
            } else if (ex.getErrorType() == ErrorType.ERRNO) {
                throw ex;
            } else {
                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, this,"comm error: %s",ex.toString());
                throw new IOException("communication failure", ex);
            }
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

            org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica replica = this.credentials.getXlocs().getReplicas(replicaNo);
            if ((replica.getReplicationFlags() & REPL_FLAG.REPL_FLAG_IS_COMPLETE.getNumber()) > 0) {
                return true;
            }
            setXCap();
            StripingPolicyImpl sp = StripingPolicyImpl.getPolicy(replica, 0);
            long lastObjectNo = sp.getObjectNoForOffset(this.credentials.getXlocs().getReadOnlyFileSize() - 1);
            int osdRelPos = 0;
            for (String osdUUID : replica.getOsdUuidsList()) {
                ServiceUUID osd = new ServiceUUID(osdUUID, parentVolume.uuidResolver);

                r = osdClient.xtreemfs_internal_get_object_set(osd.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, credentials, fileId);
                ObjectList ol = r.get();
                r.freeBuffers();
                r = null;

                byte[] serializedBitSet = ol.getSet().toByteArray();
                ObjectSet oset = null;
                try {
                    oset = new ObjectSet(ol.getStripeWidth(), ol.getFirst(), serializedBitSet);
                } catch (Exception ex) {
                    throw new IOException("cannot deserialize object set: " + ex, ex);
                }
                for (long objNo = osdRelPos; objNo <= lastObjectNo; objNo += sp.getWidth()) {
                    if (oset.contains(objNo) == false)
                        return false;
                }

            }
            //FIXME: mark replica as complete
            try {
                parent.setxattr("xtreemfs.mark_replica_complete", replica.getOsdUuids(0));
            } catch (Exception ex) {
                //only an optimization, ignore errors
            }
            return true;
        } catch (InterruptedException ex) {
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, this,"comm error: %s",ex.toString());
            throw new IOException("operation aborted", ex);
        } catch (PBRPCException ex) {
            if (ex.getErrorType() == ErrorType.REDIRECT) {
                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, this,"redirected to: %s",ex.getRedirectToServerUUID());
                forceReplica(ex.getRedirectToServerUUID());
                return isCompleteReplica(replicaNo);
            } else if (ex.getErrorType() == ErrorType.ERRNO) {
                throw ex;
            } else {
                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, this,"comm error: %s",ex.toString());
                throw new IOException("communication failure", ex);
            }
        } finally {
            if (r != null)
                r.freeBuffers();
        }
    }

    public int getReplicaNumber(String headOSDuuid) {
        for (int i = 0; i < this.credentials.getXlocs().getReplicasCount(); i++) {
            org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica replica = this.credentials.getXlocs().getReplicas(i);
            if (replica.getOsdUuidsList().contains(headOSDuuid))
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
            setXCap();
            List<ServiceUUID> osdList = currentReplica.getOSDs();
            List<ServiceUUID> osdListCopy = new ArrayList<ServiceUUID>(currentReplica.getOSDs());
            // take lowest objects of file
            for (int objectNo = 0; osdListCopy.size() != 0; objectNo++) {
                // get index of OSD for this object
                ServiceUUID osd = currentReplica.getOSDForObject(objectNo);
                // remove OSD
                osdListCopy.remove(osd);
                // send request (read only 1 byte)
                RPCResponse<ObjectData> r = osdClient.read(osd.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService,
                        credentials, fileId, objectNo, 0, 0, 1);
                r.get();
                r.freeBuffers();
            }
        } catch (UnknownUUIDException e) {
            // ignore; should not happen
        } catch (PBRPCException ex) {
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "comm error: %s", ex.toString());
            throw new IOException("communication failure", ex);
        } catch (IOException e) {
            throw new IOException("At least one OSD could not be contacted to replicate the file.", e);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    public void setLength(long newLength) throws IOException {
        XCap truncCap = parentVolume.truncateFile(fileId, userCreds);
        FileCredentials tCred = FileCredentials.newBuilder().setXcap(truncCap).setXlocs(this.credentials.getXlocs()).build();
        
        RPCResponse<OSDWriteResponse> r = null;
        try {
            setXCap();
            ServiceUUID osd = new ServiceUUID(currentReplica.getHeadOsd().toString(), parentVolume.uuidResolver);

            r = osdClient.truncate(osd.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, tCred,
                    fileId, newLength, 0);

            OSDWriteResponse resp = r.get();

            parentVolume.storeFileSizeUpdate(fileId, resp, userCreds);
            parentVolume.pushFileSizeUpdate(fileId, userCreds);


            if (position > newLength)
                position = newLength;
        } catch (InterruptedException ex) {
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, this,"comm error: %s",ex.toString());
            throw new IOException("operation aborted", ex);
        } catch (PBRPCException ex) {
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, this,"comm error: %s",ex.toString());
            throw new IOException("communication failure", ex);
        } finally {
            if (r != null)
                r.freeBuffers();
        }
    }
    
    protected FileCredentials getCredentials() {
        return credentials;
    }

    public void flush() throws IOException {
        fsync();
    }


}
