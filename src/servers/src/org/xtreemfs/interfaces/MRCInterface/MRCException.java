package org.xtreemfs.interfaces.MRCInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class MRCException extends org.xtreemfs.interfaces.utils.ONCRPCException
{
    public static final int TAG = 2010031169;

    public MRCException() {  }
    public MRCException( int error_code, String error_message, String stack_trace ) { this.error_code = error_code; this.error_message = error_message; this.stack_trace = stack_trace; }

    public int getError_code() { return error_code; }
    public void setError_code( int error_code ) { this.error_code = error_code; }
    public String getError_message() { return error_message; }
    public void setError_message( String error_message ) { this.error_message = error_message; }
    public String getStack_trace() { return stack_trace; }
    public void setStack_trace( String stack_trace ) { this.stack_trace = stack_trace; }

    // java.lang.Object
    public String toString() 
    { 
        StringWriter string_writer = new StringWriter();
        string_writer.append(this.getClass().getCanonicalName());
        string_writer.append(" ");
        PrettyPrinter pretty_printer = new PrettyPrinter( string_writer );
        pretty_printer.writeStruct( "", this );
        return string_writer.toString();
    }


    // java.io.Serializable
    public static final long serialVersionUID = 2010031169;

    // yidl.runtime.Object
    public int getTag() { return 2010031169; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::MRCException"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8; // error_code
        my_size += Integer.SIZE / 8 + ( error_message != null ? ( ( error_message.getBytes().length % 4 == 0 ) ? error_message.getBytes().length : ( error_message.getBytes().length + 4 - error_message.getBytes().length % 4 ) ) : 0 ); // error_message
        my_size += Integer.SIZE / 8 + ( stack_trace != null ? ( ( stack_trace.getBytes().length % 4 == 0 ) ? stack_trace.getBytes().length : ( stack_trace.getBytes().length + 4 - stack_trace.getBytes().length % 4 ) ) : 0 ); // stack_trace
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeUint32( "error_code", error_code );
        marshaller.writeString( "error_message", error_message );
        marshaller.writeString( "stack_trace", stack_trace );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        error_code = unmarshaller.readUint32( "error_code" );
        error_message = unmarshaller.readString( "error_message" );
        stack_trace = unmarshaller.readString( "stack_trace" );
    }

    

    private int error_code;
    private String error_message;
    private String stack_trace;

}

