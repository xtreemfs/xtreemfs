package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_pingResponse implements org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2009082978;

    
    public xtreemfs_pingResponse() { remote_coordinates = new VivaldiCoordinates(); }
    public xtreemfs_pingResponse( VivaldiCoordinates remote_coordinates ) { this.remote_coordinates = remote_coordinates; }
    public xtreemfs_pingResponse( Object from_hash_map ) { remote_coordinates = new VivaldiCoordinates(); this.deserialize( from_hash_map ); }
    public xtreemfs_pingResponse( Object[] from_array ) { remote_coordinates = new VivaldiCoordinates();this.deserialize( from_array ); }

    public VivaldiCoordinates getRemote_coordinates() { return remote_coordinates; }
    public void setRemote_coordinates( VivaldiCoordinates remote_coordinates ) { this.remote_coordinates = remote_coordinates; }

    // Object
    public String toString()
    {
        return "xtreemfs_pingResponse( " + remote_coordinates.toString() + " )";
    }

    // Serializable
    public int getTag() { return 2009082978; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_pingResponse"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.remote_coordinates.deserialize( from_hash_map.get( "remote_coordinates" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.remote_coordinates.deserialize( from_array[0] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        remote_coordinates = new VivaldiCoordinates(); remote_coordinates.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "remote_coordinates", remote_coordinates.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        remote_coordinates.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += remote_coordinates.calculateSize();
        return my_size;
    }


    private VivaldiCoordinates remote_coordinates;    

}

