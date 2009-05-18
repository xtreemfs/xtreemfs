package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_get_suitable_osdsResponse implements org.xtreemfs.interfaces.utils.Response
{
    public xtreemfs_get_suitable_osdsResponse() { osd_uuids = new StringSet(); }
    public xtreemfs_get_suitable_osdsResponse( StringSet osd_uuids ) { this.osd_uuids = osd_uuids; }
    public xtreemfs_get_suitable_osdsResponse( Object from_hash_map ) { osd_uuids = new StringSet(); this.deserialize( from_hash_map ); }
    public xtreemfs_get_suitable_osdsResponse( Object[] from_array ) { osd_uuids = new StringSet();this.deserialize( from_array ); }

    public StringSet getOsd_uuids() { return osd_uuids; }
    public void setOsd_uuids( StringSet osd_uuids ) { this.osd_uuids = osd_uuids; }

    public long getTag() { return 1224; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_get_suitable_osdsResponse"; }

    public String toString()
    {
        return "xtreemfs_get_suitable_osdsResponse( " + osd_uuids.toString() + " )";
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.osd_uuids.deserialize( ( Object[] )from_hash_map.get( "osd_uuids" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.osd_uuids.deserialize( ( Object[] )from_array[0] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        osd_uuids = new StringSet(); osd_uuids.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "osd_uuids", osd_uuids.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        osd_uuids.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += osd_uuids.calculateSize();
        return my_size;
    }

    // Response
    public int getOperationNumber() { return 1224; }


    private StringSet osd_uuids;    

}

