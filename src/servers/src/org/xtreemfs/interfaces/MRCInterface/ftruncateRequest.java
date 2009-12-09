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




public class ftruncateRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009121115;
    
    public ftruncateRequest() { write_xcap = new XCap();  }
    public ftruncateRequest( XCap write_xcap ) { this.write_xcap = write_xcap; }

    public XCap getWrite_xcap() { return write_xcap; }
    public void setWrite_xcap( XCap write_xcap ) { this.write_xcap = write_xcap; }

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
    public Response createDefaultResponse() { return new ftruncateResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009121115;    

    // yidl.runtime.Object
    public int getTag() { return 2009121115; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::ftruncateRequest"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += write_xcap.getXDRSize(); // write_xcap
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeStruct( "write_xcap", write_xcap );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        write_xcap = new XCap(); unmarshaller.readStruct( "write_xcap", write_xcap );    
    }
        
    

    private XCap write_xcap;    

}

