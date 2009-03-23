package org.xtreemfs.interfaces;

import java.util.HashMap;
import java.util.Iterator;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class ServiceDataMap extends HashMap<String, String>
{
    public ServiceDataMap()
    { }

    public ServiceDataMap( Object from_hash_map )
    {
        this.deserialize( from_hash_map );
    }
        
    public String toString()
    {
        String to_string = new String();
        to_string += "{ ";
        for ( Iterator<String> key_i = keySet().iterator(); key_i.hasNext(); )
        {
             String key = key_i.next();
             String value = get( key );
             to_string += "\"" + key + "\"" + ": " + "\"" + value + "\"" + ", ";             
        }            
        to_string += "}";
        return to_string;        
    }    
    
    public Object serialize()
    {
        HashMap<String, String> to_hash_map = new HashMap<String, String>();
        for ( Iterator<String> key_i = keySet().iterator(); key_i.hasNext(); )
        {
             String key = key_i.next();
             String value = get( key );
             to_hash_map.put( key, value );
        }
        return to_hash_map;
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {       
        writer.putInt( keySet().size() );
        for ( Iterator<String> key_i = keySet().iterator(); key_i.hasNext(); ) 
        {
            String key = key_i.next();
            org.xtreemfs.interfaces.utils.XDRUtils.serializeString( key, writer );
            String value = get( key );
            org.xtreemfs.interfaces.utils.XDRUtils.serializeString( value, writer );
        }
    }        

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, String> )from_hash_map );
    }
    
    public void deserialize( HashMap<String, String> from_hash_map )
    {    
        for ( Iterator<String> from_hash_map_key_i = from_hash_map.keySet().iterator(); from_hash_map_key_i.hasNext(); )
        {
            String from_hash_map_key = from_hash_map_key_i.next();
            Object from_hash_map_value = from_hash_map.get( from_hash_map_key );
            this.put( from_hash_map_key, ( String )from_hash_map_value );
        }    
    }        

    public void deserialize( ReusableBuffer buf ) 
    {
        int new_size = buf.getInt();
        for ( int i = 0; i < new_size; i++ )
        {
            String new_key; 
            new_key = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
            String new_value; 
            new_value = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
            this.put( new_key, new_value );
        }
    } 

    public int calculateSize() 
    {
        int my_size = Integer.SIZE / 8;
        for ( Iterator<String> key_i = keySet().iterator(); key_i.hasNext(); ) 
        {
            String key = key_i.next();
            String value = get( key );
            my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(value);	    my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(key);
	    my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(key);
        }
        return my_size;
    }


}
    
