/*
 * Copyright (c) 2016 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.ec;

import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.client.RPCResponseAvailableListener;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyResponse;

public class ECHelper {
    public static int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }

    public final static RPCResponseAvailableListener<emptyResponse> emptyResponseListener = new RPCResponseAvailableListener<emptyResponse>() {
        @Override
        public void responseAvailable(RPCResponse<emptyResponse> r) {
            try {
                r.get();
            } catch (Exception ex) {
                Logging.logUserError(Logging.LEVEL_DEBUG, Category.ec, this, ex);
            } finally {
                // Free the allocated buffers
                r.freeBuffers();
            }
        }
    };


    @SuppressWarnings("rawtypes")
    public final static RPCResponseAvailableListener ignoringResponseListener = new RPCResponseAvailableListener() {
         @Override
         public void responseAvailable(RPCResponse r) {
             try {
                 r.get();
             } catch (Exception ex) {
                 Logging.logUserError(Logging.LEVEL_DEBUG, Category.ec, this, ex);
             } finally {
                 // Free the allocated buffers
                 r.freeBuffers();
             }
         }
     };

    /*
     * public static void xor(byte[] target, byte[] a, byte[] b) { // xor(ReusableBuffer.wrap(target),
     * ReusableBuffer.wrap(a), ReusableBuffer.wrap(b)); assert (target.length == a.length); assert (target.length ==
     * b.length);
     * 
     * for (int i = 0; i < target.length; i++) { target[i] = (byte) (a[i] ^ b[i]); } }
     * 
     * public static void xor(byte[] target, ReusableBuffer a, ReusableBuffer b) { xor(ReusableBuffer.wrap(target), a,
     * b); }
     */

    /**
     * Computes the xor between the src and cur buffers and stores the result in the dst buffer. <br>
     * The src buffers position will be at its limit afterwards, <br>
     * and the dst buffers position will be forwarded by the number of xor'd bytes.
     */
    public static void xor(ReusableBuffer dst, ReusableBuffer src, ReusableBuffer cur) {
        if (src.remaining() > dst.remaining() || src.remaining() > cur.remaining()) {
            throw new IndexOutOfBoundsException(
                    "cur.remaining() and dst.remaining() have to be at least src.remaining()");
        }

        while (src.remaining() >= 8) {
            // FIXME (jdillmann): Check endianess!
            dst.putLong(src.getLong() ^ cur.getLong());
        }

        while (src.hasRemaining()) {
            dst.put((byte) (src.get() ^ cur.get()));
        }
    }


    /**
     * Computes the xor between the dst and src buffers and stores the result in the dst buffer. <br>
     * The src buffers position will be at its limit afterwards, <br>
     * and the dst buffers position will be forwarded by the number of xor'd bytes.
     */
    public static void xor(ReusableBuffer dst, ReusableBuffer src) {
        int dstPos = dst.position();
        ReusableBuffer dstViewBuffer = dst.createViewBuffer();
        dst.position(dstPos);
        dstViewBuffer.position(dstPos);

        xor(dst, src, dstViewBuffer);
        BufferPool.free(dstViewBuffer);
    }


    /**
     * Returns a new (maybe view) buffer with a capacity of length, which contains the data from the src buffer followed
     * by zeros if the src capacity is less then length. <br>
     * If the src capacity was larger then length, a view buffer from 0:length will be returned.<br>
     * Note: the caller is responsible for freeing the returned buffer.
     */
    public static ReusableBuffer zeroPad(ReusableBuffer src, int length) {
        if (src != null && src.capacity() >= length) {
            int srcPos = src.position();
            src.position(0);
            ReusableBuffer viewBuffer = src.createViewBuffer();
            viewBuffer.range(0, length);
            src.position(srcPos);
            return viewBuffer;
        }

        ReusableBuffer padded = BufferPool.allocate(length);
        if (src != null) {
            int srcPos = src.position();
            src.position(0);
            padded.put(src);
            src.position(srcPos);
        }

        zeroFill(padded);
        padded.flip();
        return padded;
    }

    /**
     * Fills the buffer from position to limit with zeros. <br>
     * The buffers position will be limit afterwards.
     */
    public static void zeroFill(ReusableBuffer buffer) {
        while (buffer.remaining() >= 8) {
            buffer.putLong(0L);
        }

        while (buffer.hasRemaining()) {
            buffer.put((byte) 0);
        }
    }

}
