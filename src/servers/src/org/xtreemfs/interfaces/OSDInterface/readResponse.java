package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class readResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2009082928;
    
    public readResponse() { object_data = new ObjectData();  }
    public readResponse( ObjectData object_data ) { this.object_data = object_data; }

    public ObjectData getObject_data() { return object_data; }
    public void setObject_data( ObjectData object_data ) { this.object_data = object_data; }

    // java.io.Serializable
    public static final long serialVersionUID = 2009082928;    

    // yidl.Object
    public int getTag() { return 2009082928; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::readResponse"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += object_data.getXDRSize();
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeStruct( "object_data", object_data );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        object_data = new ObjectData(); unmarshaller.readStruct( "object_data", object_data );    
    }
        
    

    private ObjectData object_data;    

}

