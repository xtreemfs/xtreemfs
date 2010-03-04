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




public class xtreemfs_renew_capabilityResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2010030551;

    public xtreemfs_renew_capabilityResponse() { renewed_xcap = new XCap();  }
    public xtreemfs_renew_capabilityResponse( XCap renewed_xcap ) { this.renewed_xcap = renewed_xcap; }

    public XCap getRenewed_xcap() { return renewed_xcap; }
    public void setRenewed_xcap( XCap renewed_xcap ) { this.renewed_xcap = renewed_xcap; }

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
    public static final long serialVersionUID = 2010030551;

    // yidl.runtime.Object
    public int getTag() { return 2010030551; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_renew_capabilityResponse"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += renewed_xcap.getXDRSize(); // renewed_xcap
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeStruct( "renewed_xcap", renewed_xcap );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        renewed_xcap = new XCap(); unmarshaller.readStruct( "renewed_xcap", renewed_xcap );
    }

    

    private XCap renewed_xcap;

}

