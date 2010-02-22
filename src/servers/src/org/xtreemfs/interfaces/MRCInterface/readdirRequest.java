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




public class readdirRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2010022426;

    public readdirRequest() {  }
    public readdirRequest( long known_etag, int limit_more_directory_entries_count, boolean names_only, long seen_directory_entries_count ) { this.known_etag = known_etag; this.limit_more_directory_entries_count = limit_more_directory_entries_count; this.names_only = names_only; this.seen_directory_entries_count = seen_directory_entries_count; }

    public long getKnown_etag() { return known_etag; }
    public void setKnown_etag( long known_etag ) { this.known_etag = known_etag; }
    public int getLimit_more_directory_entries_count() { return limit_more_directory_entries_count; }
    public void setLimit_more_directory_entries_count( int limit_more_directory_entries_count ) { this.limit_more_directory_entries_count = limit_more_directory_entries_count; }
    public boolean getNames_only() { return names_only; }
    public void setNames_only( boolean names_only ) { this.names_only = names_only; }
    public long getSeen_directory_entries_count() { return seen_directory_entries_count; }
    public void setSeen_directory_entries_count( long seen_directory_entries_count ) { this.seen_directory_entries_count = seen_directory_entries_count; }

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
    public Response createDefaultResponse() { return new readdirResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2010022426;

    // yidl.runtime.Object
    public int getTag() { return 2010022426; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::readdirRequest"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Long.SIZE / 8; // known_etag
        my_size += Integer.SIZE / 8; // limit_more_directory_entries_count
        my_size += Integer.SIZE / 8; // names_only
        my_size += Long.SIZE / 8; // seen_directory_entries_count
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeUint64( "known_etag", known_etag );
        marshaller.writeUint16( "limit_more_directory_entries_count", limit_more_directory_entries_count );
        marshaller.writeBoolean( "names_only", names_only );
        marshaller.writeUint64( "seen_directory_entries_count", seen_directory_entries_count );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        known_etag = unmarshaller.readUint64( "known_etag" );
        limit_more_directory_entries_count = unmarshaller.readUint16( "limit_more_directory_entries_count" );
        names_only = unmarshaller.readBoolean( "names_only" );
        seen_directory_entries_count = unmarshaller.readUint64( "seen_directory_entries_count" );
    }

    

    private long known_etag;
    private int limit_more_directory_entries_count;
    private boolean names_only;
    private long seen_directory_entries_count;

}

