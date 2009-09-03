package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class InvalidArgumentException extends Struct
{
    public static final int TAG = 2009082921;
    
    public InvalidArgumentException() {  }
    public InvalidArgumentException( String error_message ) { this.error_message = error_message; }

    public String getError_message() { return error_message; }
    public void setError_message( String error_message ) { this.error_message = error_message; }

    // java.io.Serializable
    public static final long serialVersionUID = 2009082921;    

    // yidl.Object
    public int getTag() { return 2009082921; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::InvalidArgumentException"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += ( ( error_message.getBytes().length + Integer.SIZE/8 ) % 4 == 0 ) ? ( error_message.getBytes().length + Integer.SIZE/8 ) : ( error_message.getBytes().length + Integer.SIZE/8 + 4 - ( error_message.getBytes().length + Integer.SIZE/8 ) % 4 );
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "error_message", error_message );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        error_message = unmarshaller.readString( "error_message" );    
    }
        
    

    private String error_message;    

}

