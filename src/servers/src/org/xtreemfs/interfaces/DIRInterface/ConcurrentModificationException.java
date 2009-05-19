package org.xtreemfs.interfaces.DIRInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class ConcurrentModificationException extends org.xtreemfs.interfaces.utils.ONCRPCException
{
    public static final int TAG = 1106;

    
    public ConcurrentModificationException() { stack_trace = ""; }
    public ConcurrentModificationException( String stack_trace ) { this.stack_trace = stack_trace; }
    public ConcurrentModificationException( Object from_hash_map ) { stack_trace = ""; this.deserialize( from_hash_map ); }
    public ConcurrentModificationException( Object[] from_array ) { stack_trace = "";this.deserialize( from_array ); }

    public String getStack_trace() { return stack_trace; }
    public void setStack_trace( String stack_trace ) { this.stack_trace = stack_trace; }

    // Object
    public String toString()
    {
        return "ConcurrentModificationException( " + "\"" + stack_trace + "\"" + " )";
    }

    // Serializable
    public int getTag() { return 1106; }
    public String getTypeName() { return "org::xtreemfs::interfaces::DIRInterface::ConcurrentModificationException"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.stack_trace = ( String )from_hash_map.get( "stack_trace" );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.stack_trace = ( String )from_array[0];        
    }

    public void deserialize( ReusableBuffer buf )
    {
        stack_trace = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "stack_trace", stack_trace );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( stack_trace, writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(stack_trace);
        return my_size;
    }


    private String stack_trace;    

}

