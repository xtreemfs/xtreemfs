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
 * AUTHORS: Minor Gordon (NEC), Björn Kolbeck (ZIB)
 */

package org.xtreemfs.interfaces.utils;

import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.BufferPool;

/**
 *
 * @author bjko
 */
public class XDRUtils {

    /**
     * Maximum length of a string that the parser accepts
     */
    public static final int MAX_STRLEN = 32*1024;

    /**
     * Maximum number of elements in an array that the parser
     * accepts
     */
    //public static final int MAX_ARRAY_ELEMS = 8*1024;
    public static final int MAX_ARRAY_ELEMS = 1024*1024;

    public static final int TYPE_CALL = 0;

    public static final int TYPE_RESPONSE = 1;


    public static ReusableBuffer deserializeSerializableBuffer(ReusableBuffer data) {
        final int dataSize = data.getInt();
        if (dataSize == 0)
            return BufferPool.allocate(0);
        final ReusableBuffer viewbuf = data.createViewBuffer();
        viewbuf.range(data.position(), dataSize);
        int bytesToSkip = dataSize;
        data.position(data.position()+dataSize);
        if (dataSize% 4 > 0) {
            for (int k = 0; k < (4 - (dataSize % 4)); k++) {
                data.get();
            }
        }
        return viewbuf;
    }

    public static void serializeSerializableBuffer(ReusableBuffer data, ONCRPCBufferWriter writer) {
        if (data == null) {
            writer.writeInt32(null,0);
        } else {
            final int len = data.remaining();
            writer.writeInt32(null,len);
            writer.put(data);
            if (len % 4 > 0) {
                for (int k = 0; k < (4 - (len % 4)); k++) {
                    writer.put((byte)0);
                }
            }
        }
    }

    public static int serializableBufferLength(ReusableBuffer data) {
        if (data == null)
            return Integer.SIZE/8;
        int len = data.remaining()+Integer.SIZE/8;
        if (len % 4 > 0)
            len += 4 - (len % 4);
        return len;
    }

    public static int stringLengthPadded(String str) {
        if (str == null)
            return Integer.SIZE/8;
        int len = str.getBytes().length+Integer.SIZE/8;
        if (len % 4 > 0)
            len += 4 - (len % 4);
        return len;
    }

    public static int stringLengthPadded(byte[] str) {
        int len = str.length+Integer.SIZE/8;
        if (len % 4 > 0)
            len += 4 - (len % 4);
        return len;
    }

    public static String deserializeString(ReusableBuffer buf) {
        final int strlen = buf.getInt();
        if (strlen > MAX_STRLEN)
            throw new IllegalArgumentException("string is too large ("+strlen+"), maximum allowed is "+MAX_STRLEN+" bytes");
        byte[] bytes = new byte[strlen];
        buf.get(bytes);
        final String str = new String(bytes);
        if (strlen% 4 > 0) {
            for (int k = 0; k < (4 - (strlen % 4)); k++) {
                buf.get();
            }
        }
        return str;
    }

    public static void serializeString(String str, ONCRPCBufferWriter writer) {
        if (str == null) {
            writer.writeInt32(null,0);
            return;
        }
        final byte[] bytes = str.getBytes();
        serializeString(bytes, writer);
    }

    public static void serializeString(byte[] bytes, ONCRPCBufferWriter writer) {
        final int strlen = bytes.length;
        if (strlen > MAX_STRLEN)
            throw new IllegalArgumentException("string is too large ("+strlen+"), maximum allowed is "+MAX_STRLEN+" bytes");
        writer.writeInt32(null,strlen);
        writer.put(bytes);
        if (strlen% 4 > 0) {
            for (int k = 0; k < (4 - (strlen % 4)); k++) {
                writer.put((byte)0);
            }
        }
    }



}
