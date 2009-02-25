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


         

public class registerServiceRequest implements Request
{
    public registerServiceRequest() { service = new org.xtreemfs.interfaces.ServiceRegistry(); }
    public registerServiceRequest( ServiceRegistry service ) { this.service = service; }

    public ServiceRegistry getService() { return service; }
    public void setService( ServiceRegistry service ) { this.service = service; }

    // Object
    public String toString()
    {
        return "registerServiceRequest( " + service.toString() + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::DIRInterface::registerServiceRequest"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        service.serialize( writer );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        service = new org.xtreemfs.interfaces.ServiceRegistry(); service.deserialize( buf );    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += service.calculateSize();
        return my_size;
    }

    private ServiceRegistry service;
    

    // Request
    public int getInterfaceVersion() { return 1; }    
    public int getOperationNumber() { return 4; }
    public Response createDefaultResponse() { return new registerServiceResponse(); }

}

