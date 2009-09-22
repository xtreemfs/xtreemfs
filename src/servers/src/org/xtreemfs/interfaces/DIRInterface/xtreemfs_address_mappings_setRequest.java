package org.xtreemfs.interfaces.DIRInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class xtreemfs_address_mappings_setRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009082721;
    
    public xtreemfs_address_mappings_setRequest() { address_mappings = new AddressMappingSet();  }
    public xtreemfs_address_mappings_setRequest( AddressMappingSet address_mappings ) { this.address_mappings = address_mappings; }

    public AddressMappingSet getAddress_mappings() { return address_mappings; }
    public void setAddress_mappings( AddressMappingSet address_mappings ) { this.address_mappings = address_mappings; }

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
    public Response createDefaultResponse() { return new xtreemfs_address_mappings_setResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009082721;    

    // yidl.runtime.Object
    public int getTag() { return 2009082721; }
    public String getTypeName() { return "org::xtreemfs::interfaces::DIRInterface::xtreemfs_address_mappings_setRequest"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += address_mappings.getXDRSize(); // address_mappings
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeSequence( "address_mappings", address_mappings );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        address_mappings = new AddressMappingSet(); unmarshaller.readSequence( "address_mappings", address_mappings );    
    }
        
    

    private AddressMappingSet address_mappings;    

}

