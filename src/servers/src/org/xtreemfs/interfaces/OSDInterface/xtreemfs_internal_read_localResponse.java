package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class xtreemfs_internal_read_localResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2009082961;
    
    public xtreemfs_internal_read_localResponse() { returnValue = new InternalReadLocalResponse();  }
    public xtreemfs_internal_read_localResponse( InternalReadLocalResponse returnValue ) { this.returnValue = returnValue; }

    public InternalReadLocalResponse getReturnValue() { return returnValue; }
    public void setReturnValue( InternalReadLocalResponse returnValue ) { this.returnValue = returnValue; }

    // java.io.Serializable
    public static final long serialVersionUID = 2009082961;    

    // yidl.Object
    public int getTag() { return 2009082961; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_internal_read_localResponse"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += returnValue.getXDRSize(); // returnValue
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeStruct( "returnValue", returnValue );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        returnValue = new InternalReadLocalResponse(); unmarshaller.readStruct( "returnValue", returnValue );    
    }
        
    

    private InternalReadLocalResponse returnValue;    

}

