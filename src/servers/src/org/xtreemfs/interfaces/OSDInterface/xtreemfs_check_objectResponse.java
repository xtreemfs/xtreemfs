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




public class xtreemfs_check_objectResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2009082939;
    
    public xtreemfs_check_objectResponse() { returnValue = new ObjectData();  }
    public xtreemfs_check_objectResponse( ObjectData returnValue ) { this.returnValue = returnValue; }

    public ObjectData getReturnValue() { return returnValue; }
    public void setReturnValue( ObjectData returnValue ) { this.returnValue = returnValue; }

    // java.lang.Object
    public String toString() 
    { 
        StringWriter string_writer = new StringWriter();
        PrettyPrinter pretty_printer = new PrettyPrinter( string_writer );
        pretty_printer.writeStruct( "", this );
        return string_writer.toString();
    }


    // java.io.Serializable
    public static final long serialVersionUID = 2009082939;    

    // yidl.Object
    public int getTag() { return 2009082939; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_check_objectResponse"; }
    
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
        returnValue = new ObjectData(); unmarshaller.readStruct( "returnValue", returnValue );    
    }
        
    

    private ObjectData returnValue;    

}

