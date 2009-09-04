package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class ftruncateResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2009090414;
    
    public ftruncateResponse() { truncate_xcap = new XCap();  }
    public ftruncateResponse( XCap truncate_xcap ) { this.truncate_xcap = truncate_xcap; }

    public XCap getTruncate_xcap() { return truncate_xcap; }
    public void setTruncate_xcap( XCap truncate_xcap ) { this.truncate_xcap = truncate_xcap; }

    // java.io.Serializable
    public static final long serialVersionUID = 2009090414;    

    // yidl.Object
    public int getTag() { return 2009090414; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::ftruncateResponse"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += truncate_xcap.getXDRSize(); // truncate_xcap
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeStruct( "truncate_xcap", truncate_xcap );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        truncate_xcap = new XCap(); unmarshaller.readStruct( "truncate_xcap", truncate_xcap );    
    }
        
    

    private XCap truncate_xcap;    

}

