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
    public xtreemfs_listdirRequest( long known_etag ) { this.known_etag = known_etag; }

    public long getKnown_etag() { return known_etag; }
    public void setKnown_etag( long known_etag ) { this.known_etag = known_etag; }

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
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeUint64( "known_etag", known_etag );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        known_etag = unmarshaller.readUint64( "known_etag" );
    }

    

    private long known_etag;

}

