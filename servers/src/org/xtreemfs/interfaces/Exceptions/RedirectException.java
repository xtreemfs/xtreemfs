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


         
public class RedirectException extends org.xtreemfs.interfaces.utils.ONCRPCException 
{
    public RedirectException() { to_uuid = ""; }
    public RedirectException( String to_uuid ) { this.to_uuid = to_uuid; }

    public String getTo_uuid() { return to_uuid; }
    public void setTo_uuid( String to_uuid ) { this.to_uuid = to_uuid; }

    // Object
    public String toString()
    {
        return "RedirectException( " + "\"" + to_uuid + "\"" + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::Exceptions::RedirectException"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        { final byte[] bytes = to_uuid.getBytes(); writer.putInt( bytes.length ); writer.put( bytes );  if (bytes.length % 4 > 0) {for (int k = 0; k < (4 - (bytes.length % 4)); k++) { writer.put((byte)0); } }}        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        { int to_uuid_new_length = buf.getInt(); byte[] to_uuid_new_bytes = new byte[to_uuid_new_length]; buf.get( to_uuid_new_bytes ); to_uuid = new String( to_uuid_new_bytes ); if (to_uuid_new_length % 4 > 0) {for (int k = 0; k < (4 - (to_uuid_new_length % 4)); k++) { buf.get(); } } }    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += 4 + ( to_uuid.length() + 4 - ( to_uuid.length() % 4 ) );
        return my_size;
    }

    private String to_uuid;
    
}

