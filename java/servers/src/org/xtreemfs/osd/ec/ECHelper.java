/*
 * Copyright (c) 2016 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.ec;

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
                Logging.logUserError(Logging.LEVEL_NOTICE, Category.ec, this, ex);
            } finally {
                // Free the allocated buffers
                r.freeBuffers();
            }
        }
    };

    public static void xor(byte[] target, byte[] a, byte[] b) {
        // xor(ReusableBuffer.wrap(target), ReusableBuffer.wrap(a), ReusableBuffer.wrap(b));
        assert (target.length == a.length);
        assert (target.length == b.length);

        for (int i = 0; i < target.length; i++) {
            target[i] = (byte) (a[i] ^ b[i]);
        }
    }

    public static void xor(byte[] target, ReusableBuffer a, ReusableBuffer b) {
        xor(ReusableBuffer.wrap(target), a, b);
    }

    public static void xor(ReusableBuffer dst, ReusableBuffer src, ReusableBuffer cur) {
        assert (src.remaining() == dst.remaining());
        assert (src.remaining() <= cur.remaining());

        while (src.remaining() >= 8) {
            dst.putLong(src.getLong() ^ cur.getLong());
        }

        while (src.hasRemaining()) {
            dst.put((byte) (src.get() ^ cur.get()));
        }
    }

}
