package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_internal_debugResponse implements org.xtreemfs.interfaces.utils.Response
{
    public xtreemfs_internal_debugResponse() { result = ""; }
    public xtreemfs_internal_debugResponse( String result ) { this.result = result; }
    public xtreemfs_internal_debugResponse( Object from_hash_map ) { result = ""; this.deserialize( from_hash_map ); }
    public xtreemfs_internal_debugResponse( Object[] from_array ) { result = "";this.deserialize( from_array ); }

    public String getResult() { return result; }
    public void setResult( String result ) { this.result = result; }

    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_internal_debugResponse"; }    
    public long getTypeId() { return 100; }

    public String toString()
    {
        return "xtreemfs_internal_debugResponse( " + "\"" + result + "\"" + " )";
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.result = ( String )from_hash_map.get( "result" );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.result = ( String )from_array[0];        
    }

    public void deserialize( ReusableBuffer buf )
    {
        result = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "result", result );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( result, writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(result);
        return my_size;
    }

    // Response
    public int getOperationNumber() { return 100; }


    private String result;

}

