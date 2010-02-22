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




public class InvalidArgumentException extends org.xtreemfs.interfaces.utils.ONCRPCException
{
    public static final int TAG = 2010022467;

    public InvalidArgumentException() {  }
    public InvalidArgumentException( String error_message ) { this.error_message = error_message; }

    public String getError_message() { return error_message; }
    public void setError_message( String error_message ) { this.error_message = error_message; }

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
    public static final long serialVersionUID = 2010022467;

    // yidl.runtime.Object
    public int getTag() { return 2010022467; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::InvalidArgumentException"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8 + ( error_message != null ? ( ( error_message.getBytes().length % 4 == 0 ) ? error_message.getBytes().length : ( error_message.getBytes().length + 4 - error_message.getBytes().length % 4 ) ) : 0 ); // error_message
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

