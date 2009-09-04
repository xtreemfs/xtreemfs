package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class xtreemfs_renew_capabilityRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009082856;
    
    public xtreemfs_renew_capabilityRequest() { old_xcap = new XCap();  }
    public xtreemfs_renew_capabilityRequest( XCap old_xcap ) { this.old_xcap = old_xcap; }

    public XCap getOld_xcap() { return old_xcap; }
    public void setOld_xcap( XCap old_xcap ) { this.old_xcap = old_xcap; }

    // Request
    public Response createDefaultResponse() { return new xtreemfs_renew_capabilityResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009082856;    

    // yidl.Object
    public int getTag() { return 2009082856; }
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

