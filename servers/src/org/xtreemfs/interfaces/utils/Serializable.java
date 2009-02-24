package org.xtreemfs.interfaces.utils;

import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;


public interface Serializable {
    /**
     * Gets the IDL module[::interface]::type_name for this Serializable type
     * @return
     */
    public String getTypeName();
    
    /**
     * Serializes the message into one or more buffers and appends
     * them to the responseBuffers list. The buffers must be ready
     * for being written (i.e. position = 0 and limit set correctly).
     * @param responseBuffers list
     */
    public void serialize(ONCRPCBufferWriter writer);

    /**
     * Deserialize the message from the buffer. The buffer's
     * position should be set to the start of the message.
     * @param buf buffer containing the message
     */
    public void deserialize(ReusableBuffer buf);

    /**
     * Returns the size of this message in bytes.
     * @return
     */
    public int calculateSize();
};   
