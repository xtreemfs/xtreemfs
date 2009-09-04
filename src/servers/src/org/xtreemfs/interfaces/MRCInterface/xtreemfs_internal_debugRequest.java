package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class xtreemfs_internal_debugRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009082852;
    
    public xtreemfs_internal_debugRequest() {  }
    public xtreemfs_internal_debugRequest( String operation ) { this.operation = operation; }

    public String getOperation() { return operation; }
    public void setOperation( String operation ) { this.operation = operation; }

    // Request
    public Response createDefaultResponse() { return new xtreemfs_internal_debugResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009082852;    

    // yidl.Object
    public int getTag() { return 2009082852; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_internal_debugRequest"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8 + ( operation != null ? ( ( operation.getBytes().length % 4 == 0 ) ? operation.getBytes().length : ( operation.getBytes().length + 4 - operation.getBytes().length % 4 ) ) : 0 ); // operation
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "operation", operation );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        operation = unmarshaller.readString( "operation" );    
    }
        
    

    private String operation;    

}

