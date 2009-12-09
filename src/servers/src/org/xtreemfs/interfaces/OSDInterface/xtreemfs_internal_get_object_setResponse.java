package org.xtreemfs.interfaces.OSDInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class xtreemfs_internal_get_object_setResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2009121254;
    
    public xtreemfs_internal_get_object_setResponse() { returnValue = new ObjectList();  }
    public xtreemfs_internal_get_object_setResponse( ObjectList returnValue ) { this.returnValue = returnValue; }

    public ObjectList getReturnValue() { return returnValue; }
    public void setReturnValue( ObjectList returnValue ) { this.returnValue = returnValue; }

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
    public static final long serialVersionUID = 2009121254;    

    // yidl.runtime.Object
    public int getTag() { return 2009121254; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_internal_get_object_setResponse"; }
    
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
        returnValue = new ObjectList(); unmarshaller.readStruct( "returnValue", returnValue );    
    }
        
    

    private ObjectList returnValue;    

}

