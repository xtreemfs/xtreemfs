package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class xtreemfs_internal_get_object_setResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2009082962;
    
    public xtreemfs_internal_get_object_setResponse() { returnValue = new ObjectList();  }
    public xtreemfs_internal_get_object_setResponse( ObjectList returnValue ) { this.returnValue = returnValue; }

    public ObjectList getReturnValue() { return returnValue; }
    public void setReturnValue( ObjectList returnValue ) { this.returnValue = returnValue; }

    // java.io.Serializable
    public static final long serialVersionUID = 2009082962;    

    // yidl.Object
    public int getTag() { return 2009082962; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_internal_get_object_setResponse"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += returnValue.getXDRSize();
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeStruct( "returnValue", returnValue );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        returnValue = new ObjectList(); unmarshaller.readStruct( "returnValue", returnValue );    
    }
        
    

    private ObjectList returnValue;    

}

