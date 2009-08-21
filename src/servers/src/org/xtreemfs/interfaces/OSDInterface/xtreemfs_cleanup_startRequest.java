package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_cleanup_startRequest implements org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 1332;

    
    public xtreemfs_cleanup_startRequest() { remove_zombies = false; remove_unavail_volume = false; lost_and_found = false; }
    public xtreemfs_cleanup_startRequest( boolean remove_zombies, boolean remove_unavail_volume, boolean lost_and_found ) { this.remove_zombies = remove_zombies; this.remove_unavail_volume = remove_unavail_volume; this.lost_and_found = lost_and_found; }
    public xtreemfs_cleanup_startRequest( Object from_hash_map ) { remove_zombies = false; remove_unavail_volume = false; lost_and_found = false; this.deserialize( from_hash_map ); }
    public xtreemfs_cleanup_startRequest( Object[] from_array ) { remove_zombies = false; remove_unavail_volume = false; lost_and_found = false;this.deserialize( from_array ); }

    public boolean getRemove_zombies() { return remove_zombies; }
    public void setRemove_zombies( boolean remove_zombies ) { this.remove_zombies = remove_zombies; }
    public boolean getRemove_unavail_volume() { return remove_unavail_volume; }
    public void setRemove_unavail_volume( boolean remove_unavail_volume ) { this.remove_unavail_volume = remove_unavail_volume; }
    public boolean getLost_and_found() { return lost_and_found; }
    public void setLost_and_found( boolean lost_and_found ) { this.lost_and_found = lost_and_found; }

    // Object
    public String toString()
    {
        return "xtreemfs_cleanup_startRequest( " + Boolean.toString( remove_zombies ) + ", " + Boolean.toString( remove_unavail_volume ) + ", " + Boolean.toString( lost_and_found ) + " )";
    }

    // Serializable
    public int getTag() { return 1332; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_cleanup_startRequest"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.remove_zombies = ( ( Boolean )from_hash_map.get( "remove_zombies" ) ).booleanValue();
        this.remove_unavail_volume = ( ( Boolean )from_hash_map.get( "remove_unavail_volume" ) ).booleanValue();
        this.lost_and_found = ( ( Boolean )from_hash_map.get( "lost_and_found" ) ).booleanValue();
    }
    
    public void deserialize( Object[] from_array )
    {
        this.remove_zombies = ( ( Boolean )from_array[0] ).booleanValue();
        this.remove_unavail_volume = ( ( Boolean )from_array[1] ).booleanValue();
        this.lost_and_found = ( ( Boolean )from_array[2] ).booleanValue();        
    }

    public void deserialize( ReusableBuffer buf )
    {
        remove_zombies = buf.getInt() != 0;
        remove_unavail_volume = buf.getInt() != 0;
        lost_and_found = buf.getInt() != 0;
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "remove_zombies", new Boolean( remove_zombies ) );
        to_hash_map.put( "remove_unavail_volume", new Boolean( remove_unavail_volume ) );
        to_hash_map.put( "lost_and_found", new Boolean( lost_and_found ) );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        writer.putInt( remove_zombies ? 1 : 0 );
        writer.putInt( remove_unavail_volume ? 1 : 0 );
        writer.putInt( lost_and_found ? 1 : 0 );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += 4;
        my_size += 4;
        my_size += 4;
        return my_size;
    }

    // Request
    public Response createDefaultResponse() { return new xtreemfs_cleanup_startResponse(); }


    private boolean remove_zombies;
    private boolean remove_unavail_volume;
    private boolean lost_and_found;    

}

