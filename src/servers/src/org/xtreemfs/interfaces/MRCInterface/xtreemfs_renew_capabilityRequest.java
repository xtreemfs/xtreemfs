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




public class xtreemfs_renew_capabilityRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2010012149;
    
    public xtreemfs_renew_capabilityRequest() { old_xcap = new XCap();  }
    public xtreemfs_renew_capabilityRequest( XCap old_xcap ) { this.old_xcap = old_xcap; }

    public XCap getOld_xcap() { return old_xcap; }
    public void setOld_xcap( XCap old_xcap ) { this.old_xcap = old_xcap; }

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
    public Response createDefaultResponse() { return new xtreemfs_renew_capabilityResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2010012149;    

    // yidl.runtime.Object
    public int getTag() { return 2010012149; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_renew_capabilityRequest"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += old_xcap.getXDRSize(); // old_xcap
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeStruct( "old_xcap", old_xcap );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        old_xcap = new XCap(); unmarshaller.readStruct( "old_xcap", old_xcap );    
    }
        
    

    private XCap old_xcap;    

}

