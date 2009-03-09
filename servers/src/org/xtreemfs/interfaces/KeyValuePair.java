package org.xtreemfs.interfaces;

import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class KeyValuePair implements org.xtreemfs.interfaces.utils.Serializable
{
    public KeyValuePair() { key = ""; value = ""; }
    public KeyValuePair( String key, String value ) { this.key = key; this.value = value; }
    public KeyValuePair( Object from_hash_map ) { key = ""; value = ""; this.deserialize( from_hash_map ); }
    public KeyValuePair( Object[] from_array ) { key = ""; value = "";this.deserialize( from_array ); }

    public String getKey() { return key; }
    public void setKey( String key ) { this.key = key; }
    public String getValue() { return value; }
    public void setValue( String value ) { this.value = value; }

    // Serializable
    public String getTypeName() { return "org::xtreemfs::interfaces::KeyValuePair"; }    
    public long getTypeId() { return 0; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.key = ( String )from_hash_map.get( "key" );
        this.value = ( String )from_hash_map.get( "value" );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.key = ( String )from_array[0];
        this.value = ( String )from_array[1];        
    }

    public void deserialize( ReusableBuffer buf )
    {
        key = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        value = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "key", key );
        to_hash_map.put( "value", value );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( key, writer );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( value, writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(key);
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(value);
        return my_size;
    }


    private String key;
    private String value;

}

