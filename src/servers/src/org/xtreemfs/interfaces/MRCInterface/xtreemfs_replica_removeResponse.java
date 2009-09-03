package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class xtreemfs_replica_removeResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2009090450;
    
    public xtreemfs_replica_removeResponse() { delete_xcap = new XCap();  }
    public xtreemfs_replica_removeResponse( XCap delete_xcap ) { this.delete_xcap = delete_xcap; }

    public XCap getDelete_xcap() { return delete_xcap; }
    public void setDelete_xcap( XCap delete_xcap ) { this.delete_xcap = delete_xcap; }

    // java.io.Serializable
    public static final long serialVersionUID = 2009090450;    

    // yidl.Object
    public int getTag() { return 2009090450; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_replica_removeResponse"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += delete_xcap.getXDRSize();
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeStruct( "delete_xcap", delete_xcap );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        delete_xcap = new XCap(); unmarshaller.readStruct( "delete_xcap", delete_xcap );    
    }
        
    

    private XCap delete_xcap;    

}

