package org.xtreemfs.interfaces.NettestInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class pingResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2010012515;
    
    public pingResponse() {  }
    public pingResponse( ReusableBuffer returnValue ) { this.returnValue = returnValue; }

    public ReusableBuffer getReturnValue() { return returnValue; }
    public void setReturnValue( ReusableBuffer returnValue ) { this.returnValue = returnValue; }

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
    public static final long serialVersionUID = 2010012515;    

    // yidl.runtime.Object
    public int getTag() { return 2010012515; }
    public String getTypeName() { return "org::xtreemfs::interfaces::NettestInterface::pingResponse"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8 + ( returnValue != null ? ( ( returnValue.remaining() % 4 == 0 ) ? returnValue.remaining() : ( returnValue.remaining() + 4 - returnValue.remaining() % 4 ) ) : 0 ); // returnValue
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeBuffer( "returnValue", returnValue );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        returnValue = ( ReusableBuffer )unmarshaller.readBuffer( "returnValue" );    
    }
        
    

    private ReusableBuffer returnValue;    

}

