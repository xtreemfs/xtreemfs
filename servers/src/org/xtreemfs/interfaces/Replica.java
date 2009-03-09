package org.xtreemfs.interfaces;

import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class Replica implements org.xtreemfs.interfaces.utils.Serializable
{
    public Replica() { striping_policy = new StripingPolicy(); replication_flags = 0; osd_uuids = new StringSet(); }
    public Replica( StripingPolicy striping_policy, int replication_flags, StringSet osd_uuids ) { this.striping_policy = striping_policy; this.replication_flags = replication_flags; this.osd_uuids = osd_uuids; }
    public Replica( Object from_hash_map ) { striping_policy = new StripingPolicy(); replication_flags = 0; osd_uuids = new StringSet(); this.deserialize( from_hash_map ); }
    public Replica( Object[] from_array ) { striping_policy = new StripingPolicy(); replication_flags = 0; osd_uuids = new StringSet();this.deserialize( from_array ); }

    public StripingPolicy getStriping_policy() { return striping_policy; }
    public void setStriping_policy( StripingPolicy striping_policy ) { this.striping_policy = striping_policy; }
    public int getReplication_flags() { return replication_flags; }
    public void setReplication_flags( int replication_flags ) { this.replication_flags = replication_flags; }
    public StringSet getOsd_uuids() { return osd_uuids; }
    public void setOsd_uuids( StringSet osd_uuids ) { this.osd_uuids = osd_uuids; }

    // Serializable
    public String getTypeName() { return "org::xtreemfs::interfaces::Replica"; }    
    public long getTypeId() { return 0; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.striping_policy.deserialize( from_hash_map.get( "striping_policy" ) );
        this.replication_flags = ( ( Integer )from_hash_map.get( "replication_flags" ) ).intValue();
        this.osd_uuids.deserialize( ( Object[] )from_hash_map.get( "osd_uuids" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.striping_policy.deserialize( from_array[0] );
        this.replication_flags = ( ( Integer )from_array[1] ).intValue();
        this.osd_uuids.deserialize( ( Object[] )from_array[2] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        striping_policy = new StripingPolicy(); striping_policy.deserialize( buf );
        replication_flags = buf.getInt();
        osd_uuids = new StringSet(); osd_uuids.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "striping_policy", striping_policy.serialize() );
        to_hash_map.put( "replication_flags", new Integer( replication_flags ) );
        to_hash_map.put( "osd_uuids", osd_uuids.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        striping_policy.serialize( writer );
        writer.putInt( replication_flags );
        osd_uuids.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += striping_policy.calculateSize();
        my_size += ( Integer.SIZE / 8 );
        my_size += osd_uuids.calculateSize();
        return my_size;
    }


    private StripingPolicy striping_policy;
    private int replication_flags;
    private StringSet osd_uuids;

}

