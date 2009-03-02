package org.xtreemfs.interfaces;

import org.xtreemfs.interfaces.*;

import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         
   
public class Replica implements org.xtreemfs.interfaces.utils.Serializable
{
    public Replica() { striping_policy = new org.xtreemfs.interfaces.StripingPolicy(); replication_flags = 0; osd_uuids = new org.xtreemfs.interfaces.StringSet(); }
    public Replica( StripingPolicy striping_policy, int replication_flags, StringSet osd_uuids ) { this.striping_policy = striping_policy; this.replication_flags = replication_flags; this.osd_uuids = osd_uuids; }

    public StripingPolicy getStriping_policy() { return striping_policy; }
    public void setStriping_policy( StripingPolicy striping_policy ) { this.striping_policy = striping_policy; }
    public int getReplication_flags() { return replication_flags; }
    public void setReplication_flags( int replication_flags ) { this.replication_flags = replication_flags; }
    public StringSet getOsd_uuids() { return osd_uuids; }
    public void setOsd_uuids( StringSet osd_uuids ) { this.osd_uuids = osd_uuids; }

    // Object
    public String toString()
    {
        return "Replica( " + striping_policy.toString() + ", " + Integer.toString( replication_flags ) + ", " + osd_uuids.toString() + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::Replica"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        striping_policy.serialize( writer );
        writer.putInt( replication_flags );
        osd_uuids.serialize( writer );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        striping_policy = new org.xtreemfs.interfaces.StripingPolicy(); striping_policy.deserialize( buf );
        replication_flags = buf.getInt();
        osd_uuids = new org.xtreemfs.interfaces.StringSet(); osd_uuids.deserialize( buf );    
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

