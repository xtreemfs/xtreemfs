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
 * AUTHORS: Jan Stender (ZIB), Björn Kolbeck (ZIB)
 */

package org.xtreemfs.mrc.brain.storage;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicInteger;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;

import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.foundation.pinky.HTTPUtils;

/**
 * Holds an operation.
 * A log entry has the following format: <PRE>
 * packet length ( 4 bytes )
 * view ID (4 bytes)
 * sequence ID (4 bytes)
 * SliceID (16 bytes)
 * operationType (1 byte)
 * OpName (1 byte) + OpnameLength bytes
 * UserID (1 byte) + UserID bytes
 * GroupID (1 byte) + GroupID bytes
 * Payload (packet length - 27 bytes - OpName+UserID+GroupID bytes)
 * packet length (copy of first integer)
 * @author bjko
 */
public final class LogEntry {

    /**
     * The viewID of the view in which the operation was executed.
     */
    public final int viewID;

    /**
     * SequenceID of operation. Must be unique in each view.
     */
    public final int sequenceID;

    /**
     * Slice ID in which this operation was executed
     */
    public final SliceID slID;

    /**
     * Application specific field. Can be used to multiplex multiple
     * writers into one log.
     */
    public final byte operationType;

    /**
     * Operation name.
     */
    public final String operationName;

    /**
     * The ID of the user who created the log entry
     */
    public final String userID;

    /**
     * The ID of the group who created the log entry
     */
    public final String groupID;

    /**
     * Can be anything. Max MAX_INT bytes.
     */
    public final ReusableBuffer payload;


    /**
     * A listener waiting for this LogEntry to be synced/sent.
     */
    public SyncListener listener;

    /**
     * Something the SyncListener may need to identify the LogEntry
     * e.g. the request to acknowledge
     */
    public MRCRequest attachment;

    public static final int FIXED_HEADER_SIZE = Integer.SIZE/8*2 +
                      SliceID.SIZE_IN_BYTES +
                      Byte.SIZE/8 * 4;

    protected AtomicInteger refCount;


    /**
     * Creates a new empty log entry.
     */
    public LogEntry(int viewID, int sequenceID, SliceID slID, byte operationType,
            String operationName, String userID, String groupID,
            ReusableBuffer payload, MRCRequest attachment) {
        this.viewID = viewID;
        this.sequenceID = sequenceID;
        this.slID = slID;
        this.operationType = operationType;
        this.operationName = operationName;
        this.userID = userID;
        this.groupID = groupID;
        this.payload = payload;
        this.attachment = attachment;
        this.refCount = new AtomicInteger(1);
    }

    private int getLengthField() {
        int plc = (payload == null) ? 0 : payload.capacity();
        int flength = FIXED_HEADER_SIZE+
                      + userID.length() + groupID.length() + operationName.length() +
                     plc;
        assert(plc >= 0);
        assert(flength > 0);
        assert(flength > FIXED_HEADER_SIZE);
        return flength;
    }

    /**
     * Unmarshalls a LogEntry.
     * @param src The ByteBuffer holding the binary representation of the LogEntry.
     */
    public LogEntry(ReusableBuffer src) throws IOException {
        try {

            int leSize = src.getInt();

            if (leSize < 0)
                throw new IOException("LogEntry length cannot be negative");

            int payloadl = leSize - FIXED_HEADER_SIZE;

            viewID = src.getInt();
            sequenceID = src.getInt();
            slID = new SliceID(src);
            operationType = src.get();

            int strlen = src.get() & 0x00FF;
            assert(strlen < 256);
            assert(strlen >= 0);
            payloadl -= strlen;
            byte[] tmp = new byte[strlen];
            src.get(tmp);
            operationName = new String(tmp,HTTPUtils.ENC_ASCII);

            strlen = src.get() & 0x00FF;
            assert(strlen < 256);
            assert(strlen >= 0);
            if (strlen > 0) {
                payloadl -= strlen;
                tmp = new byte[strlen];
                src.get(tmp);
                userID = new String(tmp,HTTPUtils.ENC_ASCII);
            } else
                userID = "";

            strlen = src.get() & 0x00FF;
            assert(strlen < 256);
            assert(strlen >= 0);
            if (strlen > 0) {
                payloadl -= strlen;
                tmp = new byte[strlen];
                src.get(tmp);
                groupID = new String(tmp,HTTPUtils.ENC_ASCII);
            } else
                groupID = "";

            assert(payloadl >= 0);

            if (payloadl == 0) {
                payload = null;
            } else {
                if (src.remaining() < payloadl)
                    throw new InvalidLogEntryException("buffer does not contain enough bytes in payload. LogEntry corrupted. "+payloadl,leSize);
                byte[] arr = new byte[payloadl];
                src.get(arr);
                payload = ReusableBuffer.wrap(arr);
            }
            listener = null;
        } catch (BufferUnderflowException ex) {
            Logging.logMessage(Logging.LEVEL_ERROR,this,ex);
            throw new InvalidLogEntryException(ex.getMessage(),0);
        }
        this.refCount = new AtomicInteger(1);
    }

    /**
     * Unmarshalls a LogEntry.
     * @param src The ByteBuffer holding the binary representation of the LogEntry.
     */
    public LogEntry(FileChannel fc) throws IOException {
        ByteBuffer src2 = ByteBuffer.allocate(Integer.SIZE/8);
        fc.read(src2);
        src2.flip();
        int leSize = src2.getInt();

        ReusableBuffer src = null;
        try {

            //ReusableBuffer src = ByteBuffer.allocateDirect(leSize);
            src = BufferPool.allocate(leSize);
            fc.read(src.getBuffer());
            src.flip();

            int payloadl = leSize - FIXED_HEADER_SIZE;

            viewID = src.getInt();
            sequenceID = src.getInt();
            slID = new SliceID(src);
            operationType = src.get();

            int strlen = src.get() & 0x00FF;
            assert(strlen < 256);
            assert(strlen >= 0);
            payloadl -= strlen;
            byte[] tmp = new byte[strlen];
            src.get(tmp);
            operationName = new String(tmp,HTTPUtils.ENC_ASCII);

            strlen = src.get() & 0x00FF;
            assert(strlen < 256);
            assert(strlen >= 0);
            if (strlen > 0) {
                payloadl -= strlen;
                tmp = new byte[strlen];
                src.get(tmp);
                userID = new String(tmp,HTTPUtils.ENC_ASCII);
            } else
                userID = "";

            strlen = src.get() & 0x00FF;
            assert(strlen < 256);
            assert(strlen >= 0);
            if (strlen > 0) {
                payloadl -= strlen;
                tmp = new byte[strlen];
                src.get(tmp);
                groupID = new String(tmp,HTTPUtils.ENC_ASCII);
            } else
                groupID = "";



            if (payloadl == 0) {
                payload = null;
            } else {
                if (src.remaining() < payloadl)
                    throw new InvalidLogEntryException("buffer does not contain enough bytes in payload. LogEntry corrupted. "+payloadl,leSize);
                byte[] arr = new byte[payloadl];
                src.get(arr);
                payload = ReusableBuffer.wrap(arr);
            }
            listener = null;
        } catch (BufferUnderflowException ex) {
            throw new InvalidLogEntryException(ex.getMessage(),0);
        } finally {
            if(src != null)
                BufferPool.free(src);
        }
        this.refCount = new AtomicInteger(1);
    }

    @Override
    public LogEntry clone() {
        LogEntry myClone = new LogEntry(this.viewID,this.sequenceID,this.slID,
                                       this.operationType,this.operationName,
                                       this.userID,this.groupID,this.payload.createViewBuffer(),
                                       this.attachment);
        return myClone;
    }

    /**
     * Marshalls a log entry.
     * @return the binary representation of the LogEntry.
     */
    public ReusableBuffer marshall() {
        int leSize = this.getLengthField();
        //entry size + integer for length field itself
        //ByteBuffer m = ByteBuffer.allocateDirect(Integer.SIZE/8 + leSize);
        ReusableBuffer m = BufferPool.allocate(Integer.SIZE/8 + leSize);

        m.putInt(leSize);
        m.putInt(viewID);
        m.putInt(sequenceID);
        slID.write(m);
        m.put(operationType);

        assert(operationName.length() < 256);
        m.put((byte)operationName.length());
        if (operationName.length() > 0)
            m.put(operationName.getBytes(HTTPUtils.ENC_ASCII));

        assert(userID.length() < 256);
        m.put((byte)userID.length());
        if (userID.length() > 0)
            m.put(userID.getBytes(HTTPUtils.ENC_ASCII));

        assert(groupID.length() < 256);
        m.put((byte)groupID.length());
        if (groupID.length() > 0)
            m.put(groupID.getBytes(HTTPUtils.ENC_ASCII));

        if (payload != null) {
            payload.position(0);
            m.put(payload.getBuffer());
            payload.position(0);
        }
        m.position(0);
        return m;

    }

    /**
     * Marshalls a log entry.
     * @return the binary representation of the LogEntry.
     */
    public void marshall(ReusableBuffer m) {

        // int plc = (payload == null) ? 0 : payload.capacity();
        int leSize = this.getLengthField();
        //entry size + integer for length field itself

        m.putInt(leSize);
        m.putInt(viewID);
        m.putInt(sequenceID);
        slID.write(m);
        m.put(operationType);

        assert(operationName.length() < 256);
        m.put((byte)operationName.length());
        if (operationName.length() > 0)
            m.put(operationName.getBytes(HTTPUtils.ENC_ASCII));

        assert(userID.length() < 256);
        m.put((byte)userID.length());
        if (userID.length() > 0)
            m.put(userID.getBytes(HTTPUtils.ENC_ASCII));

        assert(groupID.length() < 256);
        m.put((byte)groupID.length());
        if (groupID.length() > 0)
            m.put(groupID.getBytes(HTTPUtils.ENC_ASCII));

        if (payload != null) {
            payload.position(0);
            m.put(payload.getBuffer());
            payload.position(0);
        }

    }

    public int binarySize() {
        return Integer.SIZE/8 + this.getLengthField();
    }


    /**
     * Registers a listener called when the LogEntry was synced or sent.
     * @param listener Listener to be called.
     */
    public void registerListener(SyncListener listener) {
        this.listener = listener;
    }

    public boolean equals(LogEntry obj) {
        //payload is not compared. Who cares?!
        //viewID+sequenceID should be unique and enough
        if (obj == null)
            throw new RuntimeException("other object must not be null!");
        return ( ( obj.viewID == this.viewID ) &&
                (obj.sequenceID == this.sequenceID) &&
                (obj.operationType == this.operationType) &&
                (obj.operationName.equals(this.operationName)) &&
                (obj.slID.equals(this.slID) ));

    }

    public int hashCode() {
        throw new UnsupportedOperationException("there is no hash function defined for LogEntry");
    }

    public String toString() {
        return "LogEntry(viewID=" + viewID + ", sequenceId=" + sequenceID
                + ", sliceId=" + slID + ", operationType=" + operationType
                + ", operationName=" + operationName + ", uid=" + userID
                + ", gid=" + groupID + ", args=" + new String(payload.array()) + ")";
    }

    public void free() {
        if (refCount.getAndDecrement() == 1) {
            //free everything
            BufferPool.free(this.payload);
        }
    }

    public void newReference() {
        refCount.incrementAndGet();
    }

}
