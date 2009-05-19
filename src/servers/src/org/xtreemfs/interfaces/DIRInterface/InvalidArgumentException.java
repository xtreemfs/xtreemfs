package org.xtreemfs.interfaces.DIRInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class InvalidArgumentException extends org.xtreemfs.interfaces.utils.ONCRPCException
{
    public static final int TAG = 1108;

    
    public InvalidArgumentException() { error_message = ""; }
    public InvalidArgumentException( String error_message ) { this.error_message = error_message; }
    public InvalidArgumentException( Object from_hash_map ) { error_message = ""; this.deserialize( from_hash_map ); }
    public InvalidArgumentException( Object[] from_array ) { error_message = "";this.deserialize( from_array ); }

    public String getError_message() { return error_message; }
    public void setError_message( String error_message ) { this.error_message = error_message; }

    // Object
    public String toString()
    {
        return "InvalidArgumentException( " + "\"" + error_message + "\"" + " )";
    }

    // Serializable
    public int getTag() { return 1108; }
    public String getTypeName() { return "org::xtreemfs::interfaces::DIRInterface::InvalidArgumentException"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.error_message = ( String )from_hash_map.get( "error_message" );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.error_message = ( String )from_array[0];        
    }

    public void deserialize( ReusableBuffer buf )
    {
        error_message = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "error_message", error_message );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( error_message, writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(error_message);
        return my_size;
    }


    private String error_message;    

}

