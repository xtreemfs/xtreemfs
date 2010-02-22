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




public class xtreemfs_service_get_by_nameResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2010022325;

    public xtreemfs_service_get_by_nameResponse() { services = new ServiceSet();  }
    public xtreemfs_service_get_by_nameResponse( ServiceSet services ) { this.services = services; }

    public ServiceSet getServices() { return services; }
    public void setServices( ServiceSet services ) { this.services = services; }

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
    public static final long serialVersionUID = 2010022325;

    // yidl.runtime.Object
    public int getTag() { return 2010022325; }
    public String getTypeName() { return "org::xtreemfs::interfaces::DIRInterface::xtreemfs_service_get_by_nameResponse"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += services.getXDRSize(); // services
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeSequence( "services", services );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        services = new ServiceSet(); unmarshaller.readSequence( "services", services );
    }

    

    private ServiceSet services;

}

