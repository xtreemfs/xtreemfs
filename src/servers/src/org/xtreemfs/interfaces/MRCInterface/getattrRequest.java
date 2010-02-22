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




public class getattrRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2010022419;

    public getattrRequest() {  }
    public getattrRequest( String path, long known_etag ) { this.path = path; this.known_etag = known_etag; }

    public String getPath() { return path; }
    public void setPath( String path ) { this.path = path; }
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
    public Response createDefaultResponse() { return new getattrResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2010022419;

    // yidl.runtime.Object
    public int getTag() { return 2010022419; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::getattrRequest"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8 + ( path != null ? ( ( path.getBytes().length % 4 == 0 ) ? path.getBytes().length : ( path.getBytes().length + 4 - path.getBytes().length % 4 ) ) : 0 ); // path
        my_size += Long.SIZE / 8; // known_etag
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "path", path );
        marshaller.writeUint64( "known_etag", known_etag );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        path = unmarshaller.readString( "path" );
        known_etag = unmarshaller.readUint64( "known_etag" );
    }

    

    private String path;
    private long known_etag;

}

