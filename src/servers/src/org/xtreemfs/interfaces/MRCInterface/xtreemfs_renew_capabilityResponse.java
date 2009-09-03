package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class xtreemfs_renew_capabilityResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2009090447;
    
    public xtreemfs_renew_capabilityResponse() { renewed_xcap = new XCap();  }
    public xtreemfs_renew_capabilityResponse( XCap renewed_xcap ) { this.renewed_xcap = renewed_xcap; }

    public XCap getRenewed_xcap() { return renewed_xcap; }
    public void setRenewed_xcap( XCap renewed_xcap ) { this.renewed_xcap = renewed_xcap; }

    // java.io.Serializable
    public static final long serialVersionUID = 2009090447;    

    // yidl.Object
    public int getTag() { return 2009090447; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_renew_capabilityResponse"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += renewed_xcap.getXDRSize();
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

