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


         

public class accessResponse implements Response
{
    public accessResponse() { returnValue = false; }
    public accessResponse( boolean returnValue ) { this.returnValue = returnValue; }

    public boolean getReturnValue() { return returnValue; }
    public void setReturnValue( boolean returnValue ) { this.returnValue = returnValue; }

    // Object
    public String toString()
    {
        return "accessResponse( " + Boolean.toString( returnValue ) + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::MRCInterface::accessResponse"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        writer.putInt( returnValue ? 1 : 0 );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        returnValue = buf.getInt() != 0;    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += 4;
        return my_size;
    }

    private boolean returnValue;
    

    // Response
    public int getInterfaceVersion() { return 2; }
    public int getOperationNumber() { return 1; }    

}

