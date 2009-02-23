package org.xtreemfs.interfaces;

import java.util.List;
import org.xtreemfs.common.buffer.ReusableBuffer;


public interface Serializable {

    /**
     * Serializes the message into one or more buffers and appends
     * them to the responseBuffers list. The buffers must be ready
     * for being written (i.e. position = 0 and limit set correctly).
     * @param responseBuffers list
     */
    public void serialize(List<ReusableBuffer> responseBuffers);

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
    public int getSize();
};   
