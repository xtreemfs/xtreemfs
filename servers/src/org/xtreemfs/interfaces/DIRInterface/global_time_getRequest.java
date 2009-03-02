package org.xtreemfs.interfaces.DIRInterface;

import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.DIRInterface.*;

import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         

public class global_time_getRequest implements Request
{
    public global_time_getRequest() {  }



    // Object
    public String toString()
    {
        return "global_time_getRequest()";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::DIRInterface::global_time_getRequest"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
    
    }
    
    public int calculateSize()
    {
        int my_size = 0;

        return my_size;
    }


    

    // Request
    public int getInterfaceVersion() { return 1; }    
    public int getOperationNumber() { return 8; }
    public Response createDefaultResponse() { return new global_time_getResponse(); }

}

