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




public class setattrRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2010022431;

    public setattrRequest() { stbuf = new Stat();  }
    public setattrRequest( String path, Stat stbuf, int to_set ) { this.path = path; this.stbuf = stbuf; this.to_set = to_set; }

    public String getPath() { return path; }
    public void setPath( String path ) { this.path = path; }
    public Stat getStbuf() { return stbuf; }
    public void setStbuf( Stat stbuf ) { this.stbuf = stbuf; }
    public int getTo_set() { return to_set; }
    public void setTo_set( int to_set ) { this.to_set = to_set; }

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
    public Response createDefaultResponse() { return new setattrResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2010022431;

    // yidl.runtime.Object
    public int getTag() { return 2010022431; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::setattrRequest"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8 + ( path != null ? ( ( path.getBytes().length % 4 == 0 ) ? path.getBytes().length : ( path.getBytes().length + 4 - path.getBytes().length % 4 ) ) : 0 ); // path
        my_size += stbuf.getXDRSize(); // stbuf
        my_size += Integer.SIZE / 8; // to_set
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "path", path );
        marshaller.writeStruct( "stbuf", stbuf );
        marshaller.writeUint32( "to_set", to_set );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        path = unmarshaller.readString( "path" );
        stbuf = new Stat(); unmarshaller.readStruct( "stbuf", stbuf );
        to_set = unmarshaller.readUint32( "to_set" );
    }

    

    private String path;
    private Stat stbuf;
    private int to_set;

}

