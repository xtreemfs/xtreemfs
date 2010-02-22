package org.xtreemfs.interfaces.MRCInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class xtreemfs_listdirRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2010022450;

    public xtreemfs_listdirRequest() {  }
    public xtreemfs_listdirRequest( long known_etag, int limit_names_count, long seen_names_count ) { this.known_etag = known_etag; this.limit_names_count = limit_names_count; this.seen_names_count = seen_names_count; }

    public long getKnown_etag() { return known_etag; }
    public void setKnown_etag( long known_etag ) { this.known_etag = known_etag; }
    public int getLimit_names_count() { return limit_names_count; }
    public void setLimit_names_count( int limit_names_count ) { this.limit_names_count = limit_names_count; }
    public long getSeen_names_count() { return seen_names_count; }
    public void setSeen_names_count( long seen_names_count ) { this.seen_names_count = seen_names_count; }

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

    // Request
    public Response createDefaultResponse() { return new xtreemfs_listdirResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2010022450;

    // yidl.runtime.Object
    public int getTag() { return 2010022450; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_listdirRequest"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Long.SIZE / 8; // known_etag
        my_size += Integer.SIZE / 8; // limit_names_count
        my_size += Long.SIZE / 8; // seen_names_count
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeUint64( "known_etag", known_etag );
        marshaller.writeUint16( "limit_names_count", limit_names_count );
        marshaller.writeUint64( "seen_names_count", seen_names_count );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        known_etag = unmarshaller.readUint64( "known_etag" );
        limit_names_count = unmarshaller.readUint16( "limit_names_count" );
        seen_names_count = unmarshaller.readUint64( "seen_names_count" );
    }

    

    private long known_etag;
    private int limit_names_count;
    private long seen_names_count;

}

