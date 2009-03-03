package org.xtreemfs.interfaces.DIRInterface;

import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.DIRInterface.*;
import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         

public class service_get_by_typeResponse implements Response
{
    public service_get_by_typeResponse() { services = new org.xtreemfs.interfaces.ServiceRegistrySet(); }
    public service_get_by_typeResponse( ServiceRegistrySet services ) { this.services = services; }

    public ServiceRegistrySet getServices() { return services; }
    public void setServices( ServiceRegistrySet services ) { this.services = services; }

    // Object
    public String toString()
    {
        return "service_get_by_typeResponse( " + services.toString() + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::DIRInterface::service_get_by_typeResponse"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        services.serialize( writer );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        services = new org.xtreemfs.interfaces.ServiceRegistrySet(); services.deserialize( buf );    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += services.calculateSize();
        return my_size;
    }

    private ServiceRegistrySet services;
    

    // Response
    public int getInterfaceVersion() { return 1; }
    public int getOperationNumber() { return 6; }    

}

