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




public class xtreemfs_service_get_by_typeRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009082725;
    
    public xtreemfs_service_get_by_typeRequest() { type = ServiceType.SERVICE_TYPE_MIXED;  }
    public xtreemfs_service_get_by_typeRequest( ServiceType type ) { this.type = type; }

    public ServiceType getType() { return type; }
    public void setType( ServiceType type ) { this.type = type; }

    // java.lang.Object
    public String toString() 
    { 
        StringWriter string_writer = new StringWriter();
        PrettyPrinter pretty_printer = new PrettyPrinter( string_writer );
        pretty_printer.writeStruct( "", this );
        return string_writer.toString();
    }

    // Request
    public Response createDefaultResponse() { return new xtreemfs_service_get_by_typeResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009082725;    

    // yidl.Object
    public int getTag() { return 2009082725; }
    public String getTypeName() { return "org::xtreemfs::interfaces::DIRInterface::xtreemfs_service_get_by_typeRequest"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8; // type
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeInt32( type, type.intValue() );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        type = ServiceType.parseInt( unmarshaller.readInt32( "type" ) );    
    }
        
    

    private ServiceType type;    

}

