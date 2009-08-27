package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class getxattrResponse implements org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2009082825;

    
    public getxattrResponse() { value = ""; }
    public getxattrResponse( String value ) { this.value = value; }
    public getxattrResponse( Object from_hash_map ) { value = ""; this.deserialize( from_hash_map ); }
    public getxattrResponse( Object[] from_array ) { value = "";this.deserialize( from_array ); }

    public String getValue() { return value; }
    public void setValue( String value ) { this.value = value; }

    // Object
    public String toString()
    {
        return "getxattrResponse( " + "\"" + value + "\"" + " )";
    }

    // Serializable
    public int getTag() { return 2009082825; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::getxattrResponse"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.value = ( String )from_hash_map.get( "value" );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.value = ( String )from_array[0];        
    }

    public void deserialize( ReusableBuffer buf )
    {
        value = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "value", value );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( value, writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(value);
        return my_size;
    }


    private String value;    

}

