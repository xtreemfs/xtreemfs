package org.xtreemfs.interfaces.DIRInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class ConcurrentModificationException extends Struct
{
    public static final int TAG = 2009082738;
    
    public ConcurrentModificationException() {  }
    public ConcurrentModificationException( String stack_trace ) { this.stack_trace = stack_trace; }

    public String getStack_trace() { return stack_trace; }
    public void setStack_trace( String stack_trace ) { this.stack_trace = stack_trace; }

    // java.io.Serializable
    public static final long serialVersionUID = 2009082738;    

    // yidl.Object
    public int getTag() { return 2009082738; }
    public String getTypeName() { return "org::xtreemfs::interfaces::DIRInterface::ConcurrentModificationException"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += ( ( stack_trace.getBytes().length + Integer.SIZE/8 ) % 4 == 0 ) ? ( stack_trace.getBytes().length + Integer.SIZE/8 ) : ( stack_trace.getBytes().length + Integer.SIZE/8 + 4 - ( stack_trace.getBytes().length + Integer.SIZE/8 ) % 4 );
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "stack_trace", stack_trace );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        stack_trace = unmarshaller.readString( "stack_trace" );    
    }
        
    

    private String stack_trace;    

}

