package org.xtreemfs.interfaces;

import org.xtreemfs.interfaces.*;

import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         
   
public class OSDsByStripingPolicy implements org.xtreemfs.interfaces.utils.Serializable
{
    public OSDsByStripingPolicy() { striping_policy = new org.xtreemfs.interfaces.StripingPolicy(); osd_uuids = new org.xtreemfs.interfaces.StringSet(); }
    public OSDsByStripingPolicy( StripingPolicy striping_policy, StringSet osd_uuids ) { this.striping_policy = striping_policy; this.osd_uuids = osd_uuids; }

    public StripingPolicy getStriping_policy() { return striping_policy; }
    public void setStriping_policy( StripingPolicy striping_policy ) { this.striping_policy = striping_policy; }
    public StringSet getOsd_uuids() { return osd_uuids; }
    public void setOsd_uuids( StringSet osd_uuids ) { this.osd_uuids = osd_uuids; }

    // Object
    public String toString()
    {
        return "OSDsByStripingPolicy( " + striping_policy.toString() + ", " + osd_uuids.toString() + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::OSDsByStripingPolicy"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        striping_policy.serialize( writer );
        osd_uuids.serialize( writer );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        striping_policy = new org.xtreemfs.interfaces.StripingPolicy(); striping_policy.deserialize( buf );
        osd_uuids = new org.xtreemfs.interfaces.StringSet(); osd_uuids.deserialize( buf );    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += striping_policy.calculateSize();
        my_size += osd_uuids.calculateSize();
        return my_size;
    }

    private StripingPolicy striping_policy;
    private StringSet osd_uuids;

}

