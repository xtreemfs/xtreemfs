package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_mkvolRequest implements org.xtreemfs.interfaces.utils.Request
{
    public xtreemfs_mkvolRequest() { volume = new Volume(); }
    public xtreemfs_mkvolRequest( Volume volume ) { this.volume = volume; }
    public xtreemfs_mkvolRequest( Object from_hash_map ) { volume = new Volume(); this.deserialize( from_hash_map ); }
    public xtreemfs_mkvolRequest( Object[] from_array ) { volume = new Volume();this.deserialize( from_array ); }

    public Volume getVolume() { return volume; }
    public void setVolume( Volume volume ) { this.volume = volume; }

    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_mkvolRequest"; }    
    public long getTypeId() { return 10; }

    public String toString()
    {
        return "xtreemfs_mkvolRequest( " + volume.toString() + " )";
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.volume.deserialize( from_hash_map.get( "volume" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.volume.deserialize( from_array[0] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        volume = new Volume(); volume.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "volume", volume.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        volume.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += volume.calculateSize();
        return my_size;
    }

    // Request
    public int getOperationNumber() { return 10; }
    public Response createDefaultResponse() { return new xtreemfs_mkvolResponse(); }


    private Volume volume;

}

