/*
 * Copyright (c) 2009-2010, Konrad-Zuse-Zentrum fuer Informationstechnik Berlin
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this 
 * list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, 
 * this list of conditions and the following disclaimer in the documentation 
 * and/or other materials provided with the distribution.
 * Neither the name of the Konrad-Zuse-Zentrum fuer Informationstechnik Berlin 
 * nor the names of its contributors may be used to endorse or promote products 
 * derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */
/*
 * AUTHORS: NEC HPC Europe, Bjoern Kolbeck (ZIB)
 */

package org.xtreemfs.foundation.oncrpc.utils;

import java.nio.charset.Charset;

import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;

/**
 *
 * @author bjko
 */
public class XDRUtils {
    
    /**
     * URL schemes for ONC-RPCs
     */
    public static final String ONCRPC_SCHEME = "oncrpc";
    public static final String ONCRPCG_SCHEME = "oncrpcg";
    public static final String ONCRPCS_SCHEME = "oncrpcs";
    public static final String ONCRPCU_SCHEME = "oncrpcu";

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

    private static final Charset UTF8 = Charset.forName("UTF-8");


    public static ReusableBuffer deserializeSerializableBuffer(ReusableBuffer data) {
        final int dataSize = data.getInt();
        if (dataSize == 0)
            return BufferPool.allocate(0);
        final ReusableBuffer viewbuf = data.createViewBuffer();
        viewbuf.range(data.position(), dataSize);
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
        final String str = new String(bytes,UTF8);
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
        final byte[] bytes = str.getBytes(UTF8);
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
