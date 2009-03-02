package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.OSDInterface.*;

import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         

public class truncateResponse implements Response
{
    public truncateResponse() { osd_response = new org.xtreemfs.interfaces.OSDWriteResponse(); }
    public truncateResponse( OSDWriteResponse osd_response ) { this.osd_response = osd_response; }

    public OSDWriteResponse getOsd_response() { return osd_response; }
    public void setOsd_response( OSDWriteResponse osd_response ) { this.osd_response = osd_response; }

    // Object
    public String toString()
    {
        return "truncateResponse( " + osd_response.toString() + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::OSDInterface::truncateResponse"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        osd_response.serialize( writer );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        osd_response = new org.xtreemfs.interfaces.OSDWriteResponse(); osd_response.deserialize( buf );    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += osd_response.calculateSize();
        return my_size;
    }

    private OSDWriteResponse osd_response;
    

    // Response
    public int getInterfaceVersion() { return 3; }
    public int getOperationNumber() { return 2; }    

}

