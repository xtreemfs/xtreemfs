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


         

public class service_get_by_typeRequest implements Request
{
    public service_get_by_typeRequest() { type = 0; }
    public service_get_by_typeRequest( int type ) { this.type = type; }

    public int getType() { return type; }
    public void setType( int type ) { this.type = type; }

    // Object
    public String toString()
    {
        return "service_get_by_typeRequest( " + Integer.toString( type ) + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::DIRInterface::service_get_by_typeRequest"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        writer.putInt( type );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        type = buf.getInt();    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += ( Integer.SIZE / 8 );
        return my_size;
    }

    private int type;
    

    // Request
    public int getInterfaceVersion() { return 1; }    
    public int getOperationNumber() { return 6; }
    public Response createDefaultResponse() { return new service_get_by_typeResponse(); }

}

