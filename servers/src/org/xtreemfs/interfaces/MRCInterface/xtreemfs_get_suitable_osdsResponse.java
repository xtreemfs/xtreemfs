package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.MRCInterface.*;
import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         

public class xtreemfs_get_suitable_osdsResponse implements Response
{
    public xtreemfs_get_suitable_osdsResponse() { osd_uuids = new org.xtreemfs.interfaces.StringSet(); }
    public xtreemfs_get_suitable_osdsResponse( StringSet osd_uuids ) { this.osd_uuids = osd_uuids; }

    public StringSet getOsd_uuids() { return osd_uuids; }
    public void setOsd_uuids( StringSet osd_uuids ) { this.osd_uuids = osd_uuids; }

    // Object
    public String toString()
    {
        return "xtreemfs_get_suitable_osdsResponse( " + osd_uuids.toString() + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::MRCInterface::xtreemfs_get_suitable_osdsResponse"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        osd_uuids.serialize( writer );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        osd_uuids = new org.xtreemfs.interfaces.StringSet(); osd_uuids.deserialize( buf );    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += osd_uuids.calculateSize();
        return my_size;
    }

    private StringSet osd_uuids;
    

    // Response
    public int getInterfaceVersion() { return 2; }
    public int getOperationNumber() { return 24; }    

}

