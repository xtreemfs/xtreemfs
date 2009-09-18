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




public class xtreemfs_service_registerRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009082728;
    
    public xtreemfs_service_registerRequest() { service = new Service();  }
    public xtreemfs_service_registerRequest( Service service ) { this.service = service; }

    public Service getService() { return service; }
    public void setService( Service service ) { this.service = service; }

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
    public Response createDefaultResponse() { return new xtreemfs_service_registerResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009082728;    

    // yidl.Object
    public int getTag() { return 2009082728; }
    public String getTypeName() { return "org::xtreemfs::interfaces::DIRInterface::xtreemfs_service_registerRequest"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += service.getXDRSize(); // service
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeStruct( "service", service );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        service = new Service(); unmarshaller.readStruct( "service", service );    
    }
        
    

    private Service service;    

}

