package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_pingRequest implements org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009082978;

    
    public xtreemfs_pingRequest() { coordinates = new VivaldiCoordinates(); }
    public xtreemfs_pingRequest( VivaldiCoordinates coordinates ) { this.coordinates = coordinates; }
    public xtreemfs_pingRequest( Object from_hash_map ) { coordinates = new VivaldiCoordinates(); this.deserialize( from_hash_map ); }
    public xtreemfs_pingRequest( Object[] from_array ) { coordinates = new VivaldiCoordinates();this.deserialize( from_array ); }

    public VivaldiCoordinates getCoordinates() { return coordinates; }
    public void setCoordinates( VivaldiCoordinates coordinates ) { this.coordinates = coordinates; }

    // Object
    public String toString()
    {
        return "xtreemfs_pingRequest( " + coordinates.toString() + " )";
    }

    // Serializable
    public int getTag() { return 2009082978; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_pingRequest"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.coordinates.deserialize( from_hash_map.get( "coordinates" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.coordinates.deserialize( from_array[0] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        coordinates = new VivaldiCoordinates(); coordinates.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "coordinates", coordinates.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        coordinates.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += coordinates.calculateSize();
        return my_size;
    }

    // Request
    public Response createDefaultResponse() { return new xtreemfs_pingResponse(); }


    private VivaldiCoordinates coordinates;    

}

