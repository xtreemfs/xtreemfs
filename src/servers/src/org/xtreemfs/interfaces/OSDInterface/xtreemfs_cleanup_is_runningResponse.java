package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_cleanup_is_runningResponse implements org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 1408;

    
    public xtreemfs_cleanup_is_runningResponse() { is_running = false; }
    public xtreemfs_cleanup_is_runningResponse( boolean is_running ) { this.is_running = is_running; }
    public xtreemfs_cleanup_is_runningResponse( Object from_hash_map ) { is_running = false; this.deserialize( from_hash_map ); }
    public xtreemfs_cleanup_is_runningResponse( Object[] from_array ) { is_running = false;this.deserialize( from_array ); }

    public boolean getIs_running() { return is_running; }
    public void setIs_running( boolean is_running ) { this.is_running = is_running; }

    // Object
    public String toString()
    {
        return "xtreemfs_cleanup_is_runningResponse( " + Boolean.toString( is_running ) + " )";
    }

    // Serializable
    public int getTag() { return 1408; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_cleanup_is_runningResponse"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.is_running = ( ( Boolean )from_hash_map.get( "is_running" ) ).booleanValue();
    }
    
    public void deserialize( Object[] from_array )
    {
        this.is_running = ( ( Boolean )from_array[0] ).booleanValue();        
    }

    public void deserialize( ReusableBuffer buf )
    {
        is_running = buf.getInt() != 0;
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "is_running", new Boolean( is_running ) );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        writer.putInt( is_running ? 1 : 0 );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += 4;
        return my_size;
    }


    private boolean is_running;    

}

