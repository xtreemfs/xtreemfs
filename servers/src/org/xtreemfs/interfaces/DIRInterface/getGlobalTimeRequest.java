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


         

public class getGlobalTimeRequest implements Request
{
    public getGlobalTimeRequest() {  }



    // Object
    public String toString()
    {
        return "getGlobalTimeRequest()";
    }    

    // Serializable
    public void serialize(ONCRPCBufferWriter writer) {
        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
    
    }
    
    public int getSize()
    {
        int my_size = 0;

        return my_size;
    }


    

    // Request
    public int getInterfaceVersion() { return 1; }    
    public int getOperationNumber() { return 2; }
    public Response createDefaultResponse() { return new getGlobalTimeResponse(); }

}

