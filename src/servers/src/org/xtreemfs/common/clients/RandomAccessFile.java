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
 * AUTHORS: Björn Kolbeck (ZIB), Christian Lorenz (ZIB)
 */


package org.xtreemfs.common.clients;

import java.io.IOException;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.clients.io.ByteMapper;
import org.xtreemfs.common.clients.io.ByteMapperFactory;
import org.xtreemfs.common.clients.io.ObjectStore;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.Replica;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.FileCredentials;
import org.xtreemfs.interfaces.OSDInterface.OSDException;
import org.xtreemfs.interfaces.OSDWriteResponse;
import org.xtreemfs.interfaces.ObjectData;
import org.xtreemfs.interfaces.utils.ONCRPCException;
import org.xtreemfs.osd.ErrorCodes;
import org.xtreemfs.osd.client.OSDClient;

/**
 *
 * @author bjko
 */
public class RandomAccessFile implements ObjectStore {

    private final File parent;

    private final OSDClient osdClient;

    private final Volume parentVolume;

    private final boolean readOnly;

    private long position;

    private FileCredentials credentials;

    private Replica         currentReplica;

    private int             replCnt;

    private final int      numReplicas;

    private final String   fileId;

    private ByteMapper      byteMapper;

    private boolean closed;

    RandomAccessFile(File parent, Volume parentVolume, OSDClient client, FileCredentials fc, boolean readOnly) {
        this.parent = parent;
        this.parentVolume = parentVolume;
        this.osdClient = client;
        this.credentials = fc;
        this.readOnly = readOnly;
        position = 0;
        replCnt = 0;
        closed = false;
        
        numReplicas = credentials.getXlocs().getReplicas().size();
        fileId = fc.getXcap().getFile_id();

        selectReplica();
    }

    protected void selectReplica() {
        replCnt = (replCnt+1)%numReplicas;
        currentReplica = new Replica(credentials.getXlocs().getReplicas().get(replCnt),null);
        byteMapper = ByteMapperFactory.createByteMapper(currentReplica.getStripingPolicy().getPolicyId(), currentReplica.getStripingPolicy().
                getStripeSizeForObject(0), this);
    }


    public int read(byte[] resultBuffer, int offset, int bytesToRead) throws Exception {
        if (closed)
            throw new IllegalStateException("file was closed");

        int tmp = byteMapper.read(resultBuffer, offset, bytesToRead, position);
        position += tmp;
        return tmp;
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

        if (closed)
            throw new IllegalStateException("file was closed");

        int tmp = byteMapper.write(writeFromBuffer, offset, bytesToWrite, position);
        position += bytesToWrite;
        return tmp;
    }

    @Override
    public ReusableBuffer readObject(long objectNo, int offset, int length) throws IOException, InterruptedException, ONCRPCException {
        RPCResponse<ObjectData> response = null;

        int size = 0;
        ObjectData data = null;
        ReusableBuffer buffer = null;

        int numTries = numReplicas;

        while (numTries-- > 0) { // will be aborted, if object could be read
            // get OSD
            ServiceUUID osd = currentReplica.getOSDForObject(objectNo);
            try {
                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.tool, this,
                        "%s:%d - read object from OSD %s", fileId, objectNo, osd);

                response = osdClient.read(osd.getAddress(), fileId, credentials, objectNo, 0, offset,
                    length);
                data = response.get();

                if (data.getInvalid_checksum_on_osd()) {
                    // try next replica
                    selectReplica();
                    continue;
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
                        while (buffer.hasRemaining())
                            buffer.put((byte) 0);
                        buffer.position(0);
                        BufferPool.free(data.getData());
                    }
                }

                break;

            } catch (OSDException ex) {
                if (buffer != null)
                    BufferPool.free(buffer);
                // all replicas had been tried or replication has been failed
                if (ex.getError_code() != ErrorCodes.IO_ERROR) {
                    selectReplica();
                    continue;
                }
                throw new IOException("cannot read object", ex);
            } catch (ONCRPCException ex) {
                if (buffer != null)
                    BufferPool.free(buffer);
                // all replicas had been tried or replication has been failed
                throw new IOException("cannot read object: " + ex.getMessage(), ex);
            } catch (IOException ex) {
                if (buffer != null)
                    BufferPool.free(buffer);
                // all replicas had been tried
                selectReplica();
                continue;
            } catch (InterruptedException ex) {
                // ignore
            } finally {
                if (response != null) {
                    response.freeBuffers();
                }
            }
        }
        if (buffer == null)
            throw new IOException("read object failed, cannot contact any replica for file "+fileId);
        return buffer;
    }

    @Override
    public void writeObject(long firstByteInObject, long objectNo, ReusableBuffer data) throws IOException {


        if (readOnly)
            throw new IOException("File is marked as read-only.");

        RPCResponse<OSDWriteResponse> response = null;
        try {
            // uses always first replica
            ServiceUUID osd = currentReplica.getOSDForObject(objectNo);
            ObjectData odata = new ObjectData(0, false, 0, data);
            response = osdClient.write(osd.getAddress(), fileId, credentials, objectNo, 0,
                (int) firstByteInObject, 0, odata);
            OSDWriteResponse owr = response.get();
            parentVolume.storeFileSizeUpdate(fileId, owr);
        } catch (ONCRPCException ex) {
            throw new IOException("cannot write object: " + ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            throw new IOException("cannot write object", ex);
        } finally {
            if (response != null)
                response.freeBuffers();
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
        parentVolume.closeFile(fileId,readOnly);
    }

    public void fsync() throws IOException {
        parentVolume.pushFileSizeUpdate(fileId);
    }





}
