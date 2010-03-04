package org.xtreemfs.interfaces;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class Replica implements Struct
{
    public static final int TAG = 2010030334;

    public Replica() { osd_uuids = new StringSet(); striping_policy = new StripingPolicy();  }
    public Replica( StringSet osd_uuids, int replication_flags, StripingPolicy striping_policy ) { this.osd_uuids = osd_uuids; this.replication_flags = replication_flags; this.striping_policy = striping_policy; }

    public StringSet getOsd_uuids() { return osd_uuids; }
    public void setOsd_uuids( StringSet osd_uuids ) { this.osd_uuids = osd_uuids; }
    public int getReplication_flags() { return replication_flags; }
    public void setReplication_flags( int replication_flags ) { this.replication_flags = replication_flags; }
    public StripingPolicy getStriping_policy() { return striping_policy; }
    public void setStriping_policy( StripingPolicy striping_policy ) { this.striping_policy = striping_policy; }

    // java.lang.Object
    public String toString() 
    { 
        StringWriter string_writer = new StringWriter();
        string_writer.append(this.getClass().getCanonicalName());
        string_writer.append(" ");
        PrettyPrinter pretty_printer = new PrettyPrinter( string_writer );
        pretty_printer.writeStruct( "", this );
        return string_writer.toString();
    }


    // java.io.Serializable
    public static final long serialVersionUID = 2010030334;

    // yidl.runtime.Object
    public int getTag() { return 2010030334; }
    public String getTypeName() { return "org::xtreemfs::interfaces::Replica"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += osd_uuids.getXDRSize(); // osd_uuids
        my_size += Integer.SIZE / 8; // replication_flags
        my_size += striping_policy.getXDRSize(); // striping_policy
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeSequence( "osd_uuids", osd_uuids );
        marshaller.writeUint32( "replication_flags", replication_flags );
        marshaller.writeStruct( "striping_policy", striping_policy );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        osd_uuids = new StringSet(); unmarshaller.readSequence( "osd_uuids", osd_uuids );
        replication_flags = unmarshaller.readUint32( "replication_flags" );
        striping_policy = new StripingPolicy(); unmarshaller.readStruct( "striping_policy", striping_policy );
    }

    

    private StringSet osd_uuids;
    private int replication_flags;
    private StripingPolicy striping_policy;

}

