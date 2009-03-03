package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.MRCInterface.*;
import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         

public class admin_restore_databaseResponse implements Response
{
    public admin_restore_databaseResponse() {  }



    // Object
    public String toString()
    {
        return "admin_restore_databaseResponse()";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::MRCInterface::admin_restore_databaseResponse"; }    
    
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


    

    // Response
    public int getInterfaceVersion() { return 2; }
    public int getOperationNumber() { return 53; }    

}

