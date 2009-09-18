package org.xtreemfs.interfaces.DIRInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.PrettyPrinter;
import yidl.Struct;
import yidl.Unmarshaller;




public class ConcurrentModificationException extends org.xtreemfs.interfaces.utils.ONCRPCException
{
    public static final int TAG = 2009082738;
    
    public ConcurrentModificationException() {  }
    public ConcurrentModificationException( String stack_trace ) { this.stack_trace = stack_trace; }

    public String getStack_trace() { return stack_trace; }
    public void setStack_trace( String stack_trace ) { this.stack_trace = stack_trace; }

    // java.lang.Object
    public String toString() 
    { 
        StringWriter string_writer = new StringWriter();
        PrettyPrinter pretty_printer = new PrettyPrinter( string_writer );
        pretty_printer.writeStruct( "", this );
        return string_writer.toString();
    }


    // java.io.Serializable
    public static final long serialVersionUID = 2009082738;    

    // yidl.Object
    public int getTag() { return 2009082738; }
    public String getTypeName() { return "org::xtreemfs::interfaces::DIRInterface::ConcurrentModificationException"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8 + ( stack_trace != null ? ( ( stack_trace.getBytes().length % 4 == 0 ) ? stack_trace.getBytes().length : ( stack_trace.getBytes().length + 4 - stack_trace.getBytes().length % 4 ) ) : 0 ); // stack_trace
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

