/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin and
Consiglio Nazionale delle Ricerche.

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
 * AUTHORS: Björn Kolbeck (ZIB), Jan Stender (ZIB), Christian Lorenz (ZIB),
 *          Eugenio Cesario (CNR)
 */
package org.xtreemfs.new_osd.storage;

import java.io.IOException;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.xloc.Replica;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.interfaces.Exceptions.OSDException;
import org.xtreemfs.interfaces.NewFileSize;
import org.xtreemfs.interfaces.OSDWriteResponse;
import org.xtreemfs.interfaces.ObjectData;
import org.xtreemfs.new_osd.ErrorCodes;
import org.xtreemfs.new_osd.OSDRequestDispatcher;
import org.xtreemfs.new_osd.stages.Stage;
import org.xtreemfs.new_osd.stages.StorageStage.ReadObjectCallback;
import org.xtreemfs.new_osd.stages.StorageStage.TruncateCallback;
import org.xtreemfs.new_osd.stages.StorageStage.WriteObjectCallback;

public class StorageThread extends Stage {

    public static final int STAGEOP_READ_OBJECT = 1;

    public static final int STAGEOP_WRITE_OBJECT = 2;

    public static final int STAGEOP_TRUNCATE = 3;

    private MetadataCache cache;

    private StorageLayout layout;

    private Striping striping;

    private OSDRequestDispatcher master;

    public StorageThread(int id, OSDRequestDispatcher dispatcher, Striping striping,
            MetadataCache cache, StorageLayout layout) {

        super("OSD Storage Thread " + id);

        this.cache = cache;
        this.layout = layout;
        this.master = dispatcher;
        this.striping = striping;
    }

    @Override
    protected void processMethod(StageRequest method) {

        try {

            switch (method.getStageMethod()) {
                case STAGEOP_READ_OBJECT:
                    processRead(method);
                    break;
                case STAGEOP_WRITE_OBJECT:
                    processWrite(method);
                    break;
                case STAGEOP_TRUNCATE:
                    processTruncate(method);
                    break;
            }

        } catch (Throwable th) {
            method.sendInternalServerError(th);
        }
    }

    private void processRead(StageRequest rq) {
        final ReadObjectCallback cback = (ReadObjectCallback) rq.getCallback();
        try {
            final String fileId = (String) rq.getArgs()[0];
            final long objNo = (Long) rq.getArgs()[1];
            final StripingPolicyImpl sp = (StripingPolicyImpl) rq.getArgs()[2];
            final int offset = (Integer) rq.getArgs()[3];
            final int length = (Integer) rq.getArgs()[4];


            final long stripeSize = sp.getStripeSizeForObject(objNo);
            final FileInfo fi = layout.getFileInfo(sp, fileId);
            //final boolean rangeRequested = (offset > 0) || (length < stripeSize);

            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "READ: " + fileId + "-" + objNo + ".");
            }

            int objVer = fi.getObjectVersion(objNo);
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "getting objVer " + objVer);
            }

            String objChksm = fi.getObjectChecksum(objNo);
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "checksum is " + objChksm);
            }

            
            ObjectInformation obj = layout.readObject(fileId, objNo, objVer, objChksm, sp);
            obj.setLastLocalObjectNo(fi.getLastObjectNumber());
            cback.readComplete(obj, null);
            /*
            ObjectData data = readLocalObject(fileId, objNo, objVer, objChksm, sp);
            if (rangeRequested) {
                if (data.getData() != null) {
                    /*ReusableBuffer newData = BufferPool.allocate((int)length);
                    data.getData().range( offset,  length);
                    newData.put(data.getData());
                    newData.flip();
                    BufferPool.free(data.getData());
                    data.setData(newData);* /
                    data.getData().range(offset, length);
                } else {
                    if (data.getZero_padding() > length) {
                        data.setZero_padding(length);
                    }
                }
            }
            */
            
        } catch (IOException ex) {
            cback.readComplete(null, ex);
        }

    }

    /*private ObjectData readLocalObject(String fileId, long objNo, int objVer, String objChksm, StripingPolicyImpl sp) throws IOException {
        final int stripeSize = sp.getStripeSizeForObject(objNo);
        final FileInfo fi = layout.getFileInfo(sp, fileId);

        final long lastObjectNo = fi.getLastObjectNumber();

        // retrieve the object from the storage layout
        ReusableBuffer data = layout.readObject(fileId, objNo, objVer, objChksm, sp);

        ObjectData result;

        if (data == null) {
            //object does not exists on disk
            //check if it must be padded with zeros, or if we have an eof situation
            if (objNo < lastObjectNo) {
                result = new ObjectData("", stripeSize, false, null);
            } else {
                //EOF
                result = new ObjectData("", 0, false, null);
            }
        } else {
            //regular read
            boolean checksum_invalid = !layout.checkObject(data, objChksm);
            if ((data.capacity() < stripeSize) && (objNo < lastObjectNo)) {
                result = new ObjectData("", stripeSize - data.capacity(), checksum_invalid, data);
            } else {
                result = new ObjectData("", 0, checksum_invalid, data);
            }
        }
        return result;

    }*/

    private void processWrite(StageRequest rq) {
        final WriteObjectCallback cback = (WriteObjectCallback) rq.getCallback();
        try {
            final String fileId = (String) rq.getArgs()[0];
            final long objNo = (Long) rq.getArgs()[1];
            final StripingPolicyImpl sp = (StripingPolicyImpl) rq.getArgs()[2];
            int offset = (Integer) rq.getArgs()[3];
            final ReusableBuffer data = (ReusableBuffer) rq.getArgs()[4];
            final CowPolicy cow = (CowPolicy) rq.getArgs()[5];

            final int dataLength = data.remaining();
            final int stripeSize = sp.getStripeSizeForObject(objNo);
            final FileInfo fi = layout.getFileInfo(sp, fileId);
            final String checksum = fi.getObjectChecksum(objNo);
            final boolean rangeRequested = (offset > 0) || (data.capacity() < stripeSize);


            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "WRITE: " + fileId + "-" + objNo + "." + " last objNo=" + fi.getLastObjectNumber()+" dataSize="+dataLength+" at offset="+offset);
            }

            if (offset + data.capacity() > stripeSize) {
                BufferPool.free(data);
                cback.writeComplete(null, new OSDException(ErrorCodes.INVALID_PARAMS, "offset+data.length must be <= stripe size", ""));
                return;
            }

            // determine obj version to write
            int currentV = fi.getObjectVersion(objNo);
            if (currentV == 0) {
                currentV++;
            }
            int nextV = currentV;

            assert (data != null);


            ReusableBuffer writeData = data;
            if (cow.isCOW((int) objNo)) {
                nextV++;

                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, this, "incremented version: " + fileId + "-" + objNo + "." + nextV);
                }
                // increment version number and copy old object, if only a range
                // is written
                // otherwise simply write data to new object version

                if (rangeRequested) {
                    ObjectInformation obj = layout.readObject(fileId, objNo, currentV, checksum, sp);
                    ObjectData oldObject = obj.getObjectData(fi.getLastObjectNumber()==objNo);
                    if (oldObject.getData() == null) {
                        if (oldObject.getZero_padding() > 0) {
                            //create a zero padded object
                            writeData = BufferPool.allocate(stripeSize);
                            for (int i = 0; i < stripeSize; i++) {
                                writeData.put((byte) 0);
                            }
                            writeData.position(offset);
                            writeData.put(data);
                            writeData.position(0);
                            BufferPool.free(data);
                        } else {
                            //write beyond EOF
                            if (offset > 0) {
                                writeData = BufferPool.allocate(offset + data.capacity());
                                for (int i = 0; i < offset; i++) {
                                    writeData.put((byte) 0);
                                }
                                writeData.put(data);
                                writeData.position(0);
                                BufferPool.free(data);
                            } else {
                                writeData = data;
                            }
                        }
                    } else {
                        //object data exists on disk
                        if (oldObject.getData().capacity() >= offset + data.capacity()) {
                            //old object is large enough
                            writeData = oldObject.getData();
                            writeData.position(offset);
                            writeData.put(data);
                            BufferPool.free(data);
                        } else {
                            //copy old data and then new data
                            writeData = BufferPool.allocate(offset + data.capacity());
                            writeData.put(oldObject.getData());
                            BufferPool.free(oldObject.getData());
                            writeData.position(offset);
                            writeData.put(data);
                            BufferPool.free(data);
                        }
                    }

                }
                //COW writes always start at offset 0
                offset = 0;
                cow.objectChanged((int) objNo);
            }

            writeData.position(0);
            layout.writeObject(fileId, objNo, writeData, nextV, offset, checksum, sp);
            String newChecksum = layout.createChecksum(fileId, objNo, writeData.capacity() == sp.getStripeSizeForObject(objNo) ? writeData : null, nextV, checksum);


            fi.getObjVersions().put(objNo, nextV);
            fi.getObjChecksums().put(objNo, newChecksum);

            OSDWriteResponse response = new OSDWriteResponse();

            // if the write refers to the last known object or to an object
            // beyond, i.e. the file size and globalMax are potentially
            // affected:
            if (objNo >= fi.getLastObjectNumber()) {

                long newObjSize = dataLength + offset;

                // calculate new filesize...
                long newFS = 0;
                if (objNo > 0) {
                    newFS = sp.getObjectEndOffset(objNo - 1) + 1 + newObjSize;
                } else {
                    newFS = newObjSize;
                }

                // check whether the file size might have changed; in this case,
                // ensure that the X-New-Filesize header will be set
                if (newFS > fi.getFilesize() && objNo >= fi.getLastObjectNumber()) {
                    // Metadata meta = info.getMetadata();
                    // meta.putKnownSize(newFS);
                    Logging.logMessage(Logging.LEVEL_DEBUG, this, "new filesize: " + newFS);
                    response.getNew_file_size().add(new NewFileSize(newFS, (int) fi.getTruncateEpoch()));
                } else {
                    Logging.logMessage(Logging.LEVEL_DEBUG, this, "no new filesize: " + newFS + "/" + fi.getFilesize() + ", " + fi.getLastObjectNumber() + "/" + objNo);
                }

                // update file size and last object number
                fi.setFilesize(newFS);

                // if the written object has a larger ID than the largest
                // locally-known object of the file, send 'globalMax' messages
                // to all other OSDs and update local globalMax
                if (objNo > fi.getLastObjectNumber()) {

                    fi.setLastObjectNumber(objNo);

                //FIXME: send GMAX!

                /*List<UDPMessage> msgs = striping.createGmaxMessages(new ASCIIString(fileId),
                newFS, objNo, fi.getTruncateEpoch(), details.getCurrentReplica());

                for (UDPMessage msg : msgs) {
                master.sendUDP(msg.buf.createViewBuffer(), msg.addr);
                }

                // one buffer has been allocated, which will not be freed
                // automatically; this has to be done here
                BufferPool.free(msgs.get(0).buf);*/
                }
            }

            BufferPool.free(writeData);
            cback.writeComplete(response, null);

        } catch (IOException ex) {
            ex.printStackTrace();
            cback.writeComplete(null, ex);
        }
    }

    private void processTruncate(StageRequest rq) throws IOException {

        final TruncateCallback cback = (TruncateCallback) rq.getCallback();
        try {
            final String fileId = (String) rq.getArgs()[0];
            final long newFileSize = (Long) rq.getArgs()[1];
            final StripingPolicyImpl sp = (StripingPolicyImpl) rq.getArgs()[2];
            final long epochNumber = (Long) rq.getArgs()[3];
            final Replica currentReplica = (Replica) rq.getArgs()[4];
            
            final FileInfo fi = layout.getFileInfo(sp, fileId);


            if (fi.getTruncateEpoch() >= epochNumber) {
                cback.truncateComplete(null, new OSDException(ErrorCodes.EPOCH_OUTDATED,
                "invalid truncate epoch for file " + fileId + ": " + epochNumber + ", current one is " + fi.getTruncateEpoch(),""));
                return;
            }

            // find the offset of the local OSD in the current replica's locations
            // list
            // FIXME: unify OSD IDs
            final int relativeOSDNumber = currentReplica.getOSDs().indexOf(master.getConfig().getUUID());

            long newLastObject = -1;

            if (newFileSize == 0) {
                // truncate to zero: remove all objects
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, this, "truncate to 0");
                }
                layout.deleteAllObjects(fileId);
                fi.getObjChecksums().clear();
                fi.getObjVersions().clear();
            } else if (fi.getFilesize() > newFileSize) {
                // shrink file
                newLastObject = truncateShrink(fileId, newFileSize, epochNumber, sp, fi, relativeOSDNumber);
            } else if (fi.getFilesize() < newFileSize) {
                // extend file
                newLastObject = truncateExtend(fileId, newFileSize, epochNumber, sp, fi, relativeOSDNumber);
            } else if (fi.getFilesize() == newFileSize) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "truncate to local size: " + newFileSize);
                newLastObject = fi.getLastObjectNumber();
            }

            // set the new file size and last object number
            fi.setFilesize(newFileSize);
            fi.setLastObjectNumber(newLastObject);
            fi.setTruncateEpoch(epochNumber);

            // store the truncate epoch persistently
            layout.setTruncateEpoch(fileId, epochNumber);

            // append the new file size and epoch number to the response
            OSDWriteResponse response = new OSDWriteResponse();
            response.getNew_file_size().add(new NewFileSize(newFileSize, (int) epochNumber));
            cback.truncateComplete(response, null);
        } catch (Exception ex) {
            cback.truncateComplete(null, ex);
        }

    }

    private ReusableBuffer padWithZeros(ReusableBuffer data, int stripeSize) {
        int oldSize = data.capacity();
        if (!data.enlarge(stripeSize)) {
            ReusableBuffer tmp = BufferPool.allocate(stripeSize);
            data.position(0);
            tmp.put(data);
            while (tmp.hasRemaining()) {
                tmp.put((byte) 0);
            }
            BufferPool.free(data);
            return tmp;

        } else {
            data.position(oldSize);
            while (data.hasRemaining()) {
                data.put((byte) 0);
            }
            return data;
        }
    }

    private long truncateShrink(String fileId, long fileSize, long epoch, StripingPolicyImpl sp,
            FileInfo fi, int relOsdId) throws IOException, OSDException {
        // first find out which is the new "last object"
        final long newLastObject = sp.getObjectNoForOffset(fileSize-1);
        final long oldLastObject = fi.getLastObjectNumber();
        assert (newLastObject <= oldLastObject);

        Logging.logMessage(Logging.LEVEL_DEBUG, this, "truncate shrink to: " + fileSize + " old last: " + fi.getLastObjectNumber() + "   new last: " + newLastObject);

        // remove all unnecessary objects
        final long oldRow = sp.getRow(oldLastObject);
        final long lastRow = sp.getRow(newLastObject);

        for (long r = oldRow; r >= lastRow; r--) {
            final long rowObj = r * sp.getWidth() + relOsdId - 1;
            if (rowObj == newLastObject) {
                // is local and needs to be shrunk
                final long newObjSize = fileSize - sp.getObjectStartOffset(newLastObject);
                truncateObject(fileId, newLastObject, sp, newObjSize, relOsdId);
            } else if (rowObj > newLastObject) {
                // delete objects
                final int v = fi.getObjectVersion(rowObj);
                layout.deleteObject(fileId, rowObj, v);
                fi.deleteObject(rowObj);
            }
        }

        // make sure that last objects exist
        for (long obj = newLastObject - 1; obj > newLastObject - sp.getWidth(); obj--) {
            if (obj > 0 && sp.isLocalObject(obj, relOsdId)) {
                int v = fi.getObjectVersion(obj);
                if (v == 0) {
                    // does not exist
                    createPaddingObject(fileId, obj, sp, 1, sp.getStripeSizeForObject(obj), fi);
                }
            }
        }

        return newLastObject;
    }

    private long truncateExtend(String fileId, long fileSize, long epoch, StripingPolicyImpl sp,
            FileInfo fi, int relOsdId) throws IOException, OSDException {
        // first find out which is the new "last object"
        final long newLastObject = sp.getObjectEndOffset(fileSize - 1);
        final long oldLastObject = fi.getLastObjectNumber();
        assert (newLastObject >= oldLastObject);

        Logging.logMessage(Logging.LEVEL_DEBUG, this, "truncate extend to: " + fileSize + " old last: " + oldLastObject + "   new last: " + newLastObject);

        if ((sp.getOSDforObject(newLastObject) == relOsdId) && newLastObject == oldLastObject) {
            // simply extend the old one
            truncateObject(fileId, newLastObject, sp, fileSize - sp.getObjectStartOffset(newLastObject),
                    relOsdId);
        } else {
            if ((oldLastObject > -1) && (sp.isLocalObject(oldLastObject, relOsdId))) {
                truncateObject(fileId, oldLastObject, sp, sp.getStripeSizeForObject(oldLastObject), relOsdId);
            }

            // create padding objects
            if (sp.isLocalObject(newLastObject, relOsdId)) {
                long objSize = fileSize - sp.getObjectStartOffset(newLastObject);
                createPaddingObject(fileId, newLastObject, sp, 1, objSize, fi);
            }

            // make sure that last objects exist
            for (long obj = newLastObject - 1; obj > newLastObject - sp.getWidth(); obj--) {
                if (obj > 0 && sp.isLocalObject(obj, relOsdId)) {
                    int v = fi.getObjectVersion(obj);
                    if (v == 0) {
                        // does not exist
                        createPaddingObject(fileId, obj, sp, 1, sp.getStripeSizeForObject(obj), fi);
                    }
                }
            }
        }

        return newLastObject;
    }

    private void truncateObject(String fileId, long objNo, StripingPolicyImpl sp, long newSize,
            long relOsdId) throws IOException, OSDException {

        assert (newSize > 0) : "new size is " + newSize + " but should be > 0";
        assert (newSize <= sp.getStripeSizeForObject(objNo));
        assert (objNo >= 0) : "objNo is " + objNo;
        assert (sp.getOSDforObject(objNo) == relOsdId);

        final FileInfo fi = layout.getFileInfo(sp, fileId);
        final int version = fi.getObjectVersion(objNo);
        final String checksum = fi.getObjectChecksum(objNo);

        ObjectInformation obj = layout.readObject(fileId, objNo, version, checksum, sp);
        ReusableBuffer oldData = obj.getData();
        if (obj.getStatus() == ObjectInformation.ObjectStatus.DOES_NOT_EXIST) {
            // handles the POSIX behavior of read beyond EOF
            oldData = BufferPool.allocate(0);
            oldData.position(0);
        }

        // test the checksum
        if (!layout.checkObject(oldData, checksum)) {
            Logging.logMessage(Logging.LEVEL_WARN, this, "invalid checksum: file=" + fileId + ", objNo=" + objNo);
            BufferPool.free(oldData);
            throw new OSDException(ErrorCodes.INVALID_CHECKSUM, "invalid checksum", "");
        }

        // no extension necessary when size is correct
        if (oldData.capacity() == newSize) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "truncate not necessary, object " + objNo + " is " + newSize);
            BufferPool.free(oldData);
            return;
        }

        Logging.logMessage(Logging.LEVEL_DEBUG, this, "truncate object " + objNo + " to " + newSize);

        ReusableBuffer newData = BufferPool.allocate((int) newSize);
        if (newSize < oldData.capacity()) {
            oldData.shrink((int) newSize);
        }
        newData.put(oldData);
        BufferPool.free(oldData);

        // fill the remaining buffer with zeros
        while (newData.position() < newData.capacity()) {
            newData.put((byte) 0);
        }

        layout.writeObject(fileId, objNo, newData, version + 1, 0, checksum, sp);
        String newChecksum = layout.createChecksum(fileId, objNo, newData, version + 1, checksum);

        BufferPool.free(newData);

        fi.getObjVersions().put(objNo, version + 1);
        fi.getObjChecksums().put(objNo, newChecksum);
    }

    private void createPaddingObject(String fileId, long objNo, StripingPolicyImpl sp, int version,
            long size, FileInfo fi) throws IOException {
        String checksum = layout.createPaddingObject(fileId, objNo, sp, version, size);
        fi.getObjVersions().put(objNo, version);
        fi.getObjChecksums().put(objNo, checksum);
    }
}
