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


         

public class internal_read_localResponse implements Response
{
    public internal_read_localResponse() { returnValue = new org.xtreemfs.interfaces.InternalReadLocalResponse(); }
    public internal_read_localResponse( InternalReadLocalResponse returnValue ) { this.returnValue = returnValue; }

    public InternalReadLocalResponse getReturnValue() { return returnValue; }
    public void setReturnValue( InternalReadLocalResponse returnValue ) { this.returnValue = returnValue; }

    // Object
    public String toString()
    {
        return "internal_read_localResponse( " + returnValue.toString() + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::OSDInterface::internal_read_localResponse"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        returnValue.serialize( writer );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        returnValue = new org.xtreemfs.interfaces.InternalReadLocalResponse(); returnValue.deserialize( buf );    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += returnValue.calculateSize();
        return my_size;
    }

    private InternalReadLocalResponse returnValue;
    

    // Response
    public int getInterfaceVersion() { return 3; }
    public int getOperationNumber() { return 102; }    

}

