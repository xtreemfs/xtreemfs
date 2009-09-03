package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class xtreemfs_cleanup_statusRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009082951;
    
    public xtreemfs_cleanup_statusRequest() {  }

    // Request
    public Response createDefaultResponse() { return new xtreemfs_cleanup_statusResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009082951;    

    // yidl.Object
    public int getTag() { return 2009082951; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_cleanup_statusRequest"; }
    
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

