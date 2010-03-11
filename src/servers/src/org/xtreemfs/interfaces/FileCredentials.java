package org.xtreemfs.interfaces;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class FileCredentials implements Struct
{
    public static final int TAG = 2010030941;

    public FileCredentials() { xcap = new XCap(); xlocs = new XLocSet();  }
    public FileCredentials( XCap xcap, XLocSet xlocs ) { this.xcap = xcap; this.xlocs = xlocs; }

    public XCap getXcap() { return xcap; }
    public XLocSet getXlocs() { return xlocs; }
    public void setXcap( XCap xcap ) { this.xcap = xcap; }
    public void setXlocs( XLocSet xlocs ) { this.xlocs = xlocs; }

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
    public static final long serialVersionUID = 2010030941;

    // yidl.runtime.Object
    public int getTag() { return 2010030941; }
    public String getTypeName() { return "org::xtreemfs::interfaces::FileCredentials"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += xcap.getXDRSize(); // xcap
        my_size += xlocs.getXDRSize(); // xlocs
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeStruct( "xcap", xcap );
        marshaller.writeStruct( "xlocs", xlocs );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        xcap = new XCap(); unmarshaller.readStruct( "xcap", xcap );
        xlocs = new XLocSet(); unmarshaller.readStruct( "xlocs", xlocs );
    }

    private XCap xcap;
    private XLocSet xlocs;
}
