package org.xtreemfs.interfaces;

import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class OSDtoMRCData implements org.xtreemfs.interfaces.utils.Serializable
{
    public OSDtoMRCData() { caching_policy = 0; data = ""; }
    public OSDtoMRCData( int caching_policy, String data ) { this.caching_policy = caching_policy; this.data = data; }
    public OSDtoMRCData( Object from_hash_map ) { caching_policy = 0; data = ""; this.deserialize( from_hash_map ); }
    public OSDtoMRCData( Object[] from_array ) { caching_policy = 0; data = "";this.deserialize( from_array ); }

    public int getCaching_policy() { return caching_policy; }
    public void setCaching_policy( int caching_policy ) { this.caching_policy = caching_policy; }
    public String getData() { return data; }
    public void setData( String data ) { this.data = data; }

    public String getTypeName() { return "org::xtreemfs::interfaces::OSDtoMRCData"; }    
    public long getTypeId() { return 0; }

    public String toString()
    {
        return "OSDtoMRCData( " + Integer.toString( caching_policy ) + ", " + "\"" + data + "\"" + " )"; 
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.caching_policy = ( ( Integer )from_hash_map.get( "caching_policy" ) ).intValue();
        this.data = ( String )from_hash_map.get( "data" );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.caching_policy = ( ( Integer )from_array[0] ).intValue();
        this.data = ( String )from_array[1];        
    }

    public void deserialize( ReusableBuffer buf )
    {
        caching_policy = buf.getInt();
        data = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "caching_policy", new Integer( caching_policy ) );
        to_hash_map.put( "data", data );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        writer.putInt( caching_policy );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( data, writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += ( Integer.SIZE / 8 );
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(data);
        return my_size;
    }


    private int caching_policy;
    private String data;

}

