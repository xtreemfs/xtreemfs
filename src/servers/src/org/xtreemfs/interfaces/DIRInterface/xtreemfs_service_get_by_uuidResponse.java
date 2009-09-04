package org.xtreemfs.interfaces.DIRInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class xtreemfs_service_get_by_uuidResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2009082726;
    
    public xtreemfs_service_get_by_uuidResponse() { services = new ServiceSet();  }
    public xtreemfs_service_get_by_uuidResponse( ServiceSet services ) { this.services = services; }

    public ServiceSet getServices() { return services; }
    public void setServices( ServiceSet services ) { this.services = services; }

    // java.io.Serializable
    public static final long serialVersionUID = 2009082726;    

    // yidl.Object
    public int getTag() { return 2009082726; }
    public String getTypeName() { return "org::xtreemfs::interfaces::DIRInterface::xtreemfs_service_get_by_uuidResponse"; }
    
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

