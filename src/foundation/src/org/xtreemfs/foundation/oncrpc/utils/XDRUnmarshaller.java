/*
 * Copyright (c) 2010, Konrad-Zuse-Zentrum fuer Informationstechnik Berlin
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
 * AUTHORS: Bjoern Kolbeck (ZIB)
 */

package org.xtreemfs.foundation.oncrpc.utils;

import org.xtreemfs.foundation.buffer.ReusableBuffer;

import yidl.runtime.Map;
import yidl.runtime.Sequence;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;

/**
 *
 * @author bjko
 */
public final class XDRUnmarshaller extends Unmarshaller {
    
    final ReusableBuffer buffer;
    
    public XDRUnmarshaller(ReusableBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public boolean readBoolean(Object key) {
        return buffer.getInt() == 1;
    }

    @Override
    public double readDouble(Object key) {
        return buffer.getDouble();
    }

    public int readInt32(Object key) {
        return buffer.getInt();
    }

    public long readInt64(Object key) {
        return buffer.getLong();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map readMap(Object key, Map value) {
        int numItems = buffer.getInt();
        for (int i = 0; i < numItems; i++) {
            value.unmarshal(this);
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Sequence readSequence(Object key, Sequence value) {
        int numItems = buffer.getInt();
        for (int i = 0; i < numItems; i++)
            value.unmarshal(this);
        return value;
    }

    @Override
    public String readString(Object key) {
        return XDRUtils.deserializeString(buffer);
    }

    @Override
    public Struct readStruct(Object key, Struct value) {
        value.unmarshal(this);
        return value;
    }

    @Override
    public java.lang.Object readBuffer(Object key) {
        return XDRUtils.deserializeSerializableBuffer(buffer);
    }


}
