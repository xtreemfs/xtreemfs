/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.pbrpc.utils;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 *
 * @author bjko
 */
public class RecordMarker {

    public static final int HDR_SIZE = Integer.SIZE/8*3;

    private final int rpcHeaderLength;
    private final int messageLength;
    private final int dataLength;

    public RecordMarker(ByteBuffer buf) throws IOException {
        rpcHeaderLength = buf.getInt();
        messageLength = buf.getInt();
        dataLength = buf.getInt();
    }

    public RecordMarker(int rpcHeaderLength, int messageLength, int dataLength) {
        this.rpcHeaderLength = rpcHeaderLength;
        this.messageLength = messageLength;
        this.dataLength = dataLength;
    }

    public void writeFragmentHeader(ByteBuffer buf) {
        buf.putInt(getRpcHeaderLength());
        buf.putInt(getMessageLength());
        buf.putInt(getDataLength());
    }

    public void writeFragmentHeader(ReusableBufferOutputStream out) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(HDR_SIZE);
        buf.putInt(getRpcHeaderLength());
        buf.putInt(getMessageLength());
        buf.putInt(getDataLength());
        out.write(buf.array());
    }

    /**
     * @return the rpcHeaderLength
     */
    public int getRpcHeaderLength() {
        return rpcHeaderLength;
    }

    /**
     * @return the messageLength
     */
    public int getMessageLength() {
        return messageLength;
    }

    /**
     * @return the dataLength
     */
    public int getDataLength() {
        return dataLength;
    }

}
