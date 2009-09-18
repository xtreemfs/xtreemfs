package org.xtreemfs.interfaces.OSDInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.PrettyPrinter;
import yidl.Struct;
import yidl.Unmarshaller;




public class xtreemfs_internal_get_gmaxResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2009082958;
    
    public xtreemfs_internal_get_gmaxResponse() { returnValue = new InternalGmax();  }
    public xtreemfs_internal_get_gmaxResponse( InternalGmax returnValue ) { this.returnValue = returnValue; }

    public InternalGmax getReturnValue() { return returnValue; }
    public void setReturnValue( InternalGmax returnValue ) { this.returnValue = returnValue; }

    // java.lang.Object
    public String toString() 
    { 
        StringWriter string_writer = new StringWriter();
        PrettyPrinter pretty_printer = new PrettyPrinter( string_writer );
        pretty_printer.writeStruct( "", this );
        return string_writer.toString();
    }


    // java.io.Serializable
    public static final long serialVersionUID = 2009082958;    

    // yidl.Object
    public int getTag() { return 2009082958; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_internal_get_gmaxResponse"; }
    
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
        returnValue = new InternalGmax(); unmarshaller.readStruct( "returnValue", returnValue );    
    }
        
    

    private InternalGmax returnValue;    

}

