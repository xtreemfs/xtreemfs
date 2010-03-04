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




public class statvfsRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2010030531;

    public statvfsRequest() {  }
    public statvfsRequest( String volume_name, long known_etag ) { this.volume_name = volume_name; this.known_etag = known_etag; }

    public String getVolume_name() { return volume_name; }
    public void setVolume_name( String volume_name ) { this.volume_name = volume_name; }
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
    public Response createDefaultResponse() { return new statvfsResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2010030531;

    // yidl.runtime.Object
    public int getTag() { return 2010030531; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::statvfsRequest"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8 + ( volume_name != null ? ( ( volume_name.getBytes().length % 4 == 0 ) ? volume_name.getBytes().length : ( volume_name.getBytes().length + 4 - volume_name.getBytes().length % 4 ) ) : 0 ); // volume_name
        my_size += Long.SIZE / 8; // known_etag
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "volume_name", volume_name );
        marshaller.writeUint64( "known_etag", known_etag );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        volume_name = unmarshaller.readString( "volume_name" );
        known_etag = unmarshaller.readUint64( "known_etag" );
    }

    

    private String volume_name;
    private long known_etag;

}

