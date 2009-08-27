package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_internal_debugRequest implements org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009082852;

    
    public xtreemfs_internal_debugRequest() { operation = ""; }
    public xtreemfs_internal_debugRequest( String operation ) { this.operation = operation; }
    public xtreemfs_internal_debugRequest( Object from_hash_map ) { operation = ""; this.deserialize( from_hash_map ); }
    public xtreemfs_internal_debugRequest( Object[] from_array ) { operation = "";this.deserialize( from_array ); }

    public String getOperation() { return operation; }
    public void setOperation( String operation ) { this.operation = operation; }

    // Object
    public String toString()
    {
        return "xtreemfs_internal_debugRequest( " + "\"" + operation + "\"" + " )";
    }

    // Serializable
    public int getTag() { return 2009082852; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_internal_debugRequest"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.operation = ( String )from_hash_map.get( "operation" );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.operation = ( String )from_array[0];        
    }

    public void deserialize( ReusableBuffer buf )
    {
        operation = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "operation", operation );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( operation, writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(operation);
        return my_size;
    }

    // Request
    public Response createDefaultResponse() { return new xtreemfs_internal_debugResponse(); }


    private String operation;    

}

