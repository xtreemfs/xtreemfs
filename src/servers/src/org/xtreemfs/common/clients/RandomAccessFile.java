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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.clients.internal.ObjectMapper;
import org.xtreemfs.common.clients.internal.ObjectMapper.ObjectRequest;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.Replica;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.foundation.oncrpc.client.RPCResponseAvailableListener;
import org.xtreemfs.interfaces.FileCredentials;
import org.xtreemfs.interfaces.OSDWriteResponse;
import org.xtreemfs.interfaces.ObjectData;
import org.xtreemfs.interfaces.utils.ONCRPCException;
import org.xtreemfs.osd.client.OSDClient;

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
    private int replCnt;
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
        replCnt = 0;
        closed = false;

        numReplicas = credentials.getXlocs().getReplicas().size();
        fileId = fc.getXcap().getFile_id();

        selectReplica();
    }

    protected void selectReplica() {
        replCnt = (replCnt + 1) % numReplicas;
        currentReplica = new Replica(credentials.getXlocs().getReplicas().get(replCnt), null);
        oMapper = ObjectMapper.getMapper(currentReplica.getStripingPolicy().getPolicy());
    }

    public int read(byte[] data, int offset, int length) throws IOException {
        ReusableBuffer buf = ReusableBuffer.wrap(data, offset, length);
        return read(buf);
    }

    public int read(ReusableBuffer data) throws IOException {

        if (closed) {
            throw new IllegalStateException("file was closed");
        }

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
            throw new IOException("operation aborted", ex);
        } catch (ONCRPCException ex) {
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, this,"comm error: %s",ex.toString());
            throw new IOException("communication failure", ex);
        } catch (Throwable th) {
            th.printStackTrace();
            throw new IOException("nasty!");
        } finally {
            for (RPCResponse r : resps)
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
}
