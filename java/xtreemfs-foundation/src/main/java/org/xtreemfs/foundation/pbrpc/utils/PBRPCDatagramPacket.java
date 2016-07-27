/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.pbrpc.utils;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.Message;
import java.io.IOException;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader;

/**
 *
 * @author bjko
 */
public class PBRPCDatagramPacket {
    
    private final RPCHeader   header;
    private final Message message;

    public PBRPCDatagramPacket(ReusableBuffer datagramToParse, Message msgPrototype) throws IOException {
        RecordMarker rm = new RecordMarker(datagramToParse.getBuffer());
        ReusableBufferInputStream rbis = new ReusableBufferInputStream(datagramToParse);

        final int origLimit = datagramToParse.limit();
        assert(origLimit == rm.HDR_SIZE+rm.getRpcHeaderLength()+rm.getMessageLength());
        datagramToParse.limit(rm.HDR_SIZE+rm.getRpcHeaderLength());

        header = RPCHeader.newBuilder().mergeFrom(rbis).build();

        datagramToParse.limit(origLimit);
        message = msgPrototype.newBuilderForType().mergeFrom(rbis).build();
    }

    public PBRPCDatagramPacket(RPCHeader header, Message message) {
        this.header = header;
        this.message = message;
    }

    public ReusableBuffer assembleDatagramPacket() throws IOException {

        ReusableBufferOutputStream out = new ReusableBufferOutputStream(RecordMarker.HDR_SIZE+getHeader().getSerializedSize()+getMessage().getSerializedSize());
        RecordMarker rm = new RecordMarker(getHeader().getSerializedSize(), getMessage().getSerializedSize(), 0);
        rm.writeFragmentHeader(out);
        getHeader().writeTo(out);
        getMessage().writeTo(out);
        out.flip();
        ReusableBuffer[] bufs = out.getBuffers();
        assert(bufs.length == 1);
        return bufs[0];
    }

    /**
     * @return the header
     */
    public RPCHeader getHeader() {
        return header;
    }

    /**
     * @return the message
     */
    public Message getMessage() {
        return message;
    }

}
