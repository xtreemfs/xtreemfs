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




public class xtreemfs_service_registerResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2009082728;
    
    public xtreemfs_service_registerResponse() {  }
    public xtreemfs_service_registerResponse( long returnValue ) { this.returnValue = returnValue; }

    public long getReturnValue() { return returnValue; }
    public void setReturnValue( long returnValue ) { this.returnValue = returnValue; }

    // java.lang.Object
    public String toString() 
    { 
        StringWriter string_writer = new StringWriter();
        PrettyPrinter pretty_printer = new PrettyPrinter( string_writer );
        pretty_printer.writeStruct( "", this );
        return string_writer.toString();
    }


    // java.io.Serializable
    public static final long serialVersionUID = 2009082728;    

    // yidl.Object
    public int getTag() { return 2009082728; }
    public String getTypeName() { return "org::xtreemfs::interfaces::DIRInterface::xtreemfs_service_registerResponse"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Long.SIZE / 8; // returnValue
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeUint64( "returnValue", returnValue );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        returnValue = unmarshaller.readUint64( "returnValue" );    
    }
        
    

    private long returnValue;    

}

