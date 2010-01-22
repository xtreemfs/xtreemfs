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




public class xtreemfs_service_get_by_nameRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2010012223;
    
    public xtreemfs_service_get_by_nameRequest() {  }
    public xtreemfs_service_get_by_nameRequest( String name ) { this.name = name; }

    public String getName() { return name; }
    public void setName( String name ) { this.name = name; }

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
    public Response createDefaultResponse() { return new xtreemfs_service_get_by_nameResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2010012223;    

    // yidl.runtime.Object
    public int getTag() { return 2010012223; }
    public String getTypeName() { return "org::xtreemfs::interfaces::DIRInterface::xtreemfs_service_get_by_nameRequest"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8 + ( name != null ? ( ( name.getBytes().length % 4 == 0 ) ? name.getBytes().length : ( name.getBytes().length + 4 - name.getBytes().length % 4 ) ) : 0 ); // name
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "name", name );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        name = unmarshaller.readString( "name" );    
    }
        
    

    private String name;    

}

