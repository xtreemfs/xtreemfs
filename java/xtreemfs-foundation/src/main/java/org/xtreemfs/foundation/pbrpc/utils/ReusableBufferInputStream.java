/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.pbrpc.utils;

import java.io.IOException;
import java.io.InputStream;
import org.xtreemfs.foundation.buffer.ReusableBuffer;

/**
 *
 * @author bjko
 */
public class ReusableBufferInputStream extends InputStream {

    private final ReusableBuffer data;

    public ReusableBufferInputStream(ReusableBuffer data) {
        assert(data != null);
        this.data = data;
    }

    @Override
    public int read() throws IOException {
        if (data.hasRemaining())
            return data.get();
        else
            return -1;
    }

    @Override
    public int read(byte[] buf, int offset, int length) throws IOException {
        final int bytesRemaining = data.remaining();
        if (bytesRemaining == 0)
            return -1;
        final int bytesToRead = (bytesRemaining >= length) ? length : bytesRemaining;
        data.get(buf, offset, bytesToRead);
        return bytesToRead;
    }

}
