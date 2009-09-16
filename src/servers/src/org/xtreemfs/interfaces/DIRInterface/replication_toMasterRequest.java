package org.xtreemfs.interfaces.DIRInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class replication_toMasterRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009082732;
    
    public replication_toMasterRequest() {  }

    // Request
    public Response createDefaultResponse() { return new replication_toMasterResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009082732;    

    // yidl.Object
    public int getTag() { return 2009082732; }
    public String getTypeName() { return "org::xtreemfs::interfaces::DIRInterface::replication_toMasterRequest"; }
    
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

