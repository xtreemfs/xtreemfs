package org.xtreemfs.interfaces.DIRInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class xtreemfs_global_time_s_getRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009082724;
    
    public xtreemfs_global_time_s_getRequest() {  }

    // Request
    public Response createDefaultResponse() { return new xtreemfs_global_time_s_getResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009082724;    

    // yidl.Object
    public int getTag() { return 2009082724; }
    public String getTypeName() { return "org::xtreemfs::interfaces::DIRInterface::xtreemfs_global_time_s_getRequest"; }
    
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

