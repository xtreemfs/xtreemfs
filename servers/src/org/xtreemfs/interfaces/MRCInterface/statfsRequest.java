package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class statfsRequest implements org.xtreemfs.interfaces.utils.Request
{
    public statfsRequest() { context = new Context(); volume_name = ""; }
    public statfsRequest( Context context, String volume_name ) { this.context = context; this.volume_name = volume_name; }
    public statfsRequest( Object from_hash_map ) { context = new Context(); volume_name = ""; this.deserialize( from_hash_map ); }
    public statfsRequest( Object[] from_array ) { context = new Context(); volume_name = "";this.deserialize( from_array ); }

    public Context getContext() { return context; }
    public void setContext( Context context ) { this.context = context; }
    public String getVolume_name() { return volume_name; }
    public void setVolume_name( String volume_name ) { this.volume_name = volume_name; }

    // Serializable
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::statfsRequest"; }    
    public long getTypeId() { return 19; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.context.deserialize( from_hash_map.get( "context" ) );
        this.volume_name = ( String )from_hash_map.get( "volume_name" );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.context.deserialize( from_array[0] );
        this.volume_name = ( String )from_array[1];        
    }

    public void deserialize( ReusableBuffer buf )
    {
        context = new Context(); context.deserialize( buf );
        volume_name = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "context", context.serialize() );
        to_hash_map.put( "volume_name", volume_name );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        context.serialize( writer );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( volume_name, writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += context.calculateSize();
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(volume_name);
        return my_size;
    }

    // Request
    public int getOperationNumber() { return 19; }
    public Response createDefaultResponse() { return new statfsResponse(); }


    private Context context;
    private String volume_name;

}

