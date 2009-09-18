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




public class xtreemfs_discover_dirRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009082723;
    
    public xtreemfs_discover_dirRequest() {  }

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

    // Request
    public Response createDefaultResponse() { return new xtreemfs_discover_dirResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009082723;    

    // yidl.Object
    public int getTag() { return 2009082723; }
    public String getTypeName() { return "org::xtreemfs::interfaces::DIRInterface::xtreemfs_discover_dirRequest"; }
    
    public int getXDRSize()
    {
        int my_size = 0;

        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {

    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
    
    }
        
        

}

