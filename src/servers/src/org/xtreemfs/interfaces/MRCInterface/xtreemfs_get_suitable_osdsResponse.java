package org.xtreemfs.interfaces.MRCInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class xtreemfs_get_suitable_osdsResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2010031149;

    public xtreemfs_get_suitable_osdsResponse() { osd_uuids = new StringSet();  }
    public xtreemfs_get_suitable_osdsResponse( StringSet osd_uuids ) { this.osd_uuids = osd_uuids; }

    public StringSet getOsd_uuids() { return osd_uuids; }
    public void setOsd_uuids( StringSet osd_uuids ) { this.osd_uuids = osd_uuids; }

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
    public static final long serialVersionUID = 2010031149;

    // yidl.runtime.Object
    public int getTag() { return 2010031149; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_get_suitable_osdsResponse"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += osd_uuids.getXDRSize(); // osd_uuids
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeSequence( "osd_uuids", osd_uuids );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        osd_uuids = new StringSet(); unmarshaller.readSequence( "osd_uuids", osd_uuids );
    }

    private StringSet osd_uuids;
}
