package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.OSDInterface.*;

import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         

public class internal_get_gmaxResponse implements Response
{
    public internal_get_gmaxResponse() { returnValue = new org.xtreemfs.interfaces.InternalGmax(); }
    public internal_get_gmaxResponse( InternalGmax returnValue ) { this.returnValue = returnValue; }

    public InternalGmax getReturnValue() { return returnValue; }
    public void setReturnValue( InternalGmax returnValue ) { this.returnValue = returnValue; }

    // Object
    public String toString()
    {
        return "internal_get_gmaxResponse( " + returnValue.toString() + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::OSDInterface::internal_get_gmaxResponse"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        returnValue.serialize( writer );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        returnValue = new org.xtreemfs.interfaces.InternalGmax(); returnValue.deserialize( buf );    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += returnValue.calculateSize();
        return my_size;
    }

    private InternalGmax returnValue;
    

    // Response
    public int getInterfaceVersion() { return 3; }
    public int getOperationNumber() { return 100; }    

}

