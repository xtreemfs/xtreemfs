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
        { final byte[] bytes = error_message.getBytes(); writer.putInt( bytes.length ); writer.put( bytes );  if (bytes.length % 4 > 0) {for (int k = 0; k < (4 - (bytes.length % 4)); k++) { writer.put((byte)0); } }}        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        { int error_message_new_length = buf.getInt(); byte[] error_message_new_bytes = new byte[error_message_new_length]; buf.get( error_message_new_bytes ); error_message = new String( error_message_new_bytes ); if (error_message_new_length % 4 > 0) {for (int k = 0; k < (4 - (error_message_new_length % 4)); k++) { buf.get(); } } }    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += 4 + ( error_message.length() + 4 - ( error_message.length() % 4 ) );
        return my_size;
    }

    private String error_message;
    
}

