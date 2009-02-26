package org.xtreemfs.interfaces.Exceptions;

import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.Exceptions.*;

import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         
public class InvalidArgumentException extends org.xtreemfs.interfaces.utils.ONCRPCException 
{
    public InvalidArgumentException() { error_message = ""; }
    public InvalidArgumentException( String error_message ) { this.error_message = error_message; }

    public String getError_message() { return error_message; }
    public void setError_message( String error_message ) { this.error_message = error_message; }

    // Object
    public String toString()
    {
        return "InvalidArgumentException( " + "\"" + error_message + "\"" + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::Exceptions::InvalidArgumentException"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(error_message,writer); }        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        { error_message = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += 4 + ( error_message.length() + 4 - ( error_message.length() % 4 ) );
        return my_size;
    }

    private String error_message;
    
}

