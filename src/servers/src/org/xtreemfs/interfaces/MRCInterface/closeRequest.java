package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class closeRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009082840;
    
    public closeRequest() { write_xcap = new XCap();  }
    public closeRequest( XCap write_xcap ) { this.write_xcap = write_xcap; }

    public XCap getWrite_xcap() { return write_xcap; }
    public void setWrite_xcap( XCap write_xcap ) { this.write_xcap = write_xcap; }

    // Request
    public Response createDefaultResponse() { return new closeResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009082840;    

    // yidl.Object
    public int getTag() { return 2009082840; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::closeRequest"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += write_xcap.getXDRSize();
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

