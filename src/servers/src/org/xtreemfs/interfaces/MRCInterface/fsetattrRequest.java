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




public class fsetattrRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2010031118;

    public fsetattrRequest() { stbuf = new Stat(); xcap = new XCap();  }
    public fsetattrRequest( Stat stbuf, int to_set, XCap xcap ) { this.stbuf = stbuf; this.to_set = to_set; this.xcap = xcap; }

    public Stat getStbuf() { return stbuf; }
    public int getTo_set() { return to_set; }
    public XCap getXcap() { return xcap; }
    public void setStbuf( Stat stbuf ) { this.stbuf = stbuf; }
    public void setTo_set( int to_set ) { this.to_set = to_set; }
    public void setXcap( XCap xcap ) { this.xcap = xcap; }

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
    public Response createDefaultResponse() { return new fsetattrResponse(); }

    // java.io.Serializable
    public static final long serialVersionUID = 2010031118;

    // yidl.runtime.Object
    public int getTag() { return 2010031118; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::fsetattrRequest"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += stbuf.getXDRSize(); // stbuf
        my_size += Integer.SIZE / 8; // to_set
        my_size += xcap.getXDRSize(); // xcap
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeStruct( "stbuf", stbuf );
        marshaller.writeUint32( "to_set", to_set );
        marshaller.writeStruct( "xcap", xcap );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        stbuf = new Stat(); unmarshaller.readStruct( "stbuf", stbuf );
        to_set = unmarshaller.readUint32( "to_set" );
        xcap = new XCap(); unmarshaller.readStruct( "xcap", xcap );
    }

    private Stat stbuf;
    private int to_set;
    private XCap xcap;
}
