package org.xtreemfs.interfaces;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class Replica extends Struct
{
    public static final int TAG = 2009090229;
    
    public Replica() { striping_policy = new StripingPolicy(); osd_uuids = new StringSet();  }
    public Replica( StripingPolicy striping_policy, int replication_flags, StringSet osd_uuids ) { this.striping_policy = striping_policy; this.replication_flags = replication_flags; this.osd_uuids = osd_uuids; }

    public StripingPolicy getStriping_policy() { return striping_policy; }
    public void setStriping_policy( StripingPolicy striping_policy ) { this.striping_policy = striping_policy; }
    public int getReplication_flags() { return replication_flags; }
    public void setReplication_flags( int replication_flags ) { this.replication_flags = replication_flags; }
    public StringSet getOsd_uuids() { return osd_uuids; }
    public void setOsd_uuids( StringSet osd_uuids ) { this.osd_uuids = osd_uuids; }

    // java.io.Serializable
    public static final long serialVersionUID = 2009090229;    

    // yidl.Object
    public int getTag() { return 2009090229; }
    public String getTypeName() { return "org::xtreemfs::interfaces::Replica"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += striping_policy.getXDRSize();
        my_size += ( Integer.SIZE / 8 );
        my_size += osd_uuids.getXDRSize();
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeStruct( "striping_policy", striping_policy );
        marshaller.writeUint32( "replication_flags", replication_flags );
        marshaller.writeSequence( "osd_uuids", osd_uuids );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        striping_policy = new StripingPolicy(); unmarshaller.readStruct( "striping_policy", striping_policy );
        replication_flags = unmarshaller.readUint32( "replication_flags" );
        osd_uuids = new StringSet(); unmarshaller.readSequence( "osd_uuids", osd_uuids );    
    }
        
    

    private StripingPolicy striping_policy;
    private int replication_flags;
    private StringSet osd_uuids;    

}

