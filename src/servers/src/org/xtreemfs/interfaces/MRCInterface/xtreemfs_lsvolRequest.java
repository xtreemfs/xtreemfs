package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class xtreemfs_lsvolRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009090444;
    
    public xtreemfs_lsvolRequest() {  }

    // Request
    public Response createDefaultResponse() { return new xtreemfs_lsvolResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009090444;    

    // yidl.Object
    public int getTag() { return 2009090444; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_lsvolRequest"; }
    
    public int getXDRSize()
    {
        int my_size = 0;

        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {

    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
    
    }
        
        

}

