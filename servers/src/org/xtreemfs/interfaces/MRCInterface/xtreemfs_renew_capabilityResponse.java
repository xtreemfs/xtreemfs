package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.MRCInterface.*;

import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         

public class xtreemfs_renew_capabilityResponse implements Response
{
    public xtreemfs_renew_capabilityResponse() { xcap = new org.xtreemfs.interfaces.XCap(); }
    public xtreemfs_renew_capabilityResponse( XCap xcap ) { this.xcap = xcap; }

    public XCap getXcap() { return xcap; }
    public void setXcap( XCap xcap ) { this.xcap = xcap; }

    // Object
    public String toString()
    {
        return "xtreemfs_renew_capabilityResponse( " + xcap.toString() + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::MRCInterface::xtreemfs_renew_capabilityResponse"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        xcap.serialize( writer );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        xcap = new org.xtreemfs.interfaces.XCap(); xcap.deserialize( buf );    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += xcap.calculateSize();
        return my_size;
    }

    private XCap xcap;
    

    // Response
    public int getInterfaceVersion() { return 2; }
    public int getOperationNumber() { return 25; }    

}

