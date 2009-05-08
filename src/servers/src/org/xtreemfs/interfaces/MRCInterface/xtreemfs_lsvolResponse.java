package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_lsvolResponse implements org.xtreemfs.interfaces.utils.Response
{
    public xtreemfs_lsvolResponse() { volumes = new VolumeSet(); }
    public xtreemfs_lsvolResponse( VolumeSet volumes ) { this.volumes = volumes; }
    public xtreemfs_lsvolResponse( Object from_hash_map ) { volumes = new VolumeSet(); this.deserialize( from_hash_map ); }
    public xtreemfs_lsvolResponse( Object[] from_array ) { volumes = new VolumeSet();this.deserialize( from_array ); }

    public VolumeSet getVolumes() { return volumes; }
    public void setVolumes( VolumeSet volumes ) { this.volumes = volumes; }

    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_lsvolResponse"; }    
    public long getTypeId() { return 31; }

    public String toString()
    {
        return "xtreemfs_lsvolResponse( " + volumes.toString() + " )";
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.volumes.deserialize( ( Object[] )from_hash_map.get( "volumes" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.volumes.deserialize( ( Object[] )from_array[0] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        volumes = new VolumeSet(); volumes.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "volumes", volumes.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        volumes.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += volumes.calculateSize();
        return my_size;
    }

    // Response
    public int getOperationNumber() { return 31; }


    private VolumeSet volumes;

}

