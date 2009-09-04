package org.xtreemfs.interfaces.DIRInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class xtreemfs_global_time_s_getResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2009082724;
    
    public xtreemfs_global_time_s_getResponse() {  }
    public xtreemfs_global_time_s_getResponse( long returnValue ) { this.returnValue = returnValue; }

    public long getReturnValue() { return returnValue; }
    public void setReturnValue( long returnValue ) { this.returnValue = returnValue; }

    // java.io.Serializable
    public static final long serialVersionUID = 2009082724;    

    // yidl.Object
    public int getTag() { return 2009082724; }
    public String getTypeName() { return "org::xtreemfs::interfaces::DIRInterface::xtreemfs_global_time_s_getResponse"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Long.SIZE / 8; // returnValue
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeUint64( "returnValue", returnValue );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        returnValue = unmarshaller.readUint64( "returnValue" );    
    }
        
    

    private long returnValue;    

}

