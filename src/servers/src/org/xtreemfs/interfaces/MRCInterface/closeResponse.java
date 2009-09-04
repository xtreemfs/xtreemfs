package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class closeResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2009090431;
    
    public closeResponse() {  }

    // java.io.Serializable
    public static final long serialVersionUID = 2009090431;    

    // yidl.Object
    public int getTag() { return 2009090431; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::closeResponse"; }
    
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

