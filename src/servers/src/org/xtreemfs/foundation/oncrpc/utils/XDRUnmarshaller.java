/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.foundation.oncrpc.utils;

import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.XDRUtils;
import yidl.Map;
import yidl.Sequence;
import yidl.Struct;
import yidl.Unmarshaller;

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
        return buffer.getBoolean();
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

    @Override
    public Map readMap(Object key, Map value) {
        int numItems = buffer.getInt();
        for (int i = 0; i < numItems; i++) {
            value.unmarshal(this);
        }
        return value;
    }

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
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public java.lang.Object readBuffer() {
        return XDRUtils.deserializeSerializableBuffer(buffer);
    }


}
