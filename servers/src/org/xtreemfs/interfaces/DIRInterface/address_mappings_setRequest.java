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


         

public class address_mappings_setRequest implements Request
{
    public address_mappings_setRequest() { address_mappings = new org.xtreemfs.interfaces.AddressMappingSet(); }
    public address_mappings_setRequest( AddressMappingSet address_mappings ) { this.address_mappings = address_mappings; }

    public AddressMappingSet getAddress_mappings() { return address_mappings; }
    public void setAddress_mappings( AddressMappingSet address_mappings ) { this.address_mappings = address_mappings; }

    // Object
    public String toString()
    {
        return "address_mappings_setRequest( " + address_mappings.toString() + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::DIRInterface::address_mappings_setRequest"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        address_mappings.serialize( writer );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        address_mappings = new org.xtreemfs.interfaces.AddressMappingSet(); address_mappings.deserialize( buf );    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += address_mappings.calculateSize();
        return my_size;
    }

    private AddressMappingSet address_mappings;
    

    // Request
    public int getInterfaceVersion() { return 1; }    
    public int getOperationNumber() { return 2; }
    public Response createDefaultResponse() { return new address_mappings_setResponse(); }

}

