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




public class xtreemfs_update_file_sizeRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2010012158;
    
    public xtreemfs_update_file_sizeRequest() { xcap = new XCap(); osd_write_response = new OSDWriteResponse();  }
    public xtreemfs_update_file_sizeRequest( XCap xcap, OSDWriteResponse osd_write_response ) { this.xcap = xcap; this.osd_write_response = osd_write_response; }

    public XCap getXcap() { return xcap; }
    public void setXcap( XCap xcap ) { this.xcap = xcap; }
    public OSDWriteResponse getOsd_write_response() { return osd_write_response; }
    public void setOsd_write_response( OSDWriteResponse osd_write_response ) { this.osd_write_response = osd_write_response; }

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
    public Response createDefaultResponse() { return new xtreemfs_update_file_sizeResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2010012158;    

    // yidl.runtime.Object
    public int getTag() { return 2010012158; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_update_file_sizeRequest"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += xcap.getXDRSize(); // xcap
        my_size += osd_write_response.getXDRSize(); // osd_write_response
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeStruct( "xcap", xcap );
        marshaller.writeStruct( "osd_write_response", osd_write_response );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        xcap = new XCap(); unmarshaller.readStruct( "xcap", xcap );
        osd_write_response = new OSDWriteResponse(); unmarshaller.readStruct( "osd_write_response", osd_write_response );    
    }
        
    

    private XCap xcap;
    private OSDWriteResponse osd_write_response;    

}

