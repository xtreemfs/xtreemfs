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
    public static final int TAG = 2010022417;

    public fsetattrRequest() { xcap = new XCap(); stbuf = new Stat();  }
    public fsetattrRequest( XCap xcap, Stat stbuf, int to_set ) { this.xcap = xcap; this.stbuf = stbuf; this.to_set = to_set; }

    public XCap getXcap() { return xcap; }
    public void setXcap( XCap xcap ) { this.xcap = xcap; }
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
    public Response createDefaultResponse() { return new fsetattrResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2010022417;

    // yidl.runtime.Object
    public int getTag() { return 2010022417; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::fsetattrRequest"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += xcap.getXDRSize(); // xcap
        my_size += stbuf.getXDRSize(); // stbuf
        my_size += Integer.SIZE / 8; // to_set
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeStruct( "xcap", xcap );
        marshaller.writeStruct( "stbuf", stbuf );
        marshaller.writeUint32( "to_set", to_set );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        xcap = new XCap(); unmarshaller.readStruct( "xcap", xcap );
        stbuf = new Stat(); unmarshaller.readStruct( "stbuf", stbuf );
        to_set = unmarshaller.readUint32( "to_set" );
    }

    

    private XCap xcap;
    private Stat stbuf;
    private int to_set;

}

