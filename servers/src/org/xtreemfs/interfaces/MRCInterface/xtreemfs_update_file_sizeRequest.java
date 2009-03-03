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


         

public class xtreemfs_update_file_sizeRequest implements Request
{
    public xtreemfs_update_file_sizeRequest() { xcap = new org.xtreemfs.interfaces.XCap(); new_file_size = new org.xtreemfs.interfaces.OSDWriteResponse(); }
    public xtreemfs_update_file_sizeRequest( XCap xcap, OSDWriteResponse new_file_size ) { this.xcap = xcap; this.new_file_size = new_file_size; }

    public XCap getXcap() { return xcap; }
    public void setXcap( XCap xcap ) { this.xcap = xcap; }
    public OSDWriteResponse getNew_file_size() { return new_file_size; }
    public void setNew_file_size( OSDWriteResponse new_file_size ) { this.new_file_size = new_file_size; }

    // Object
    public String toString()
    {
        return "xtreemfs_update_file_sizeRequest( " + xcap.toString() + ", " + new_file_size.toString() + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::MRCInterface::xtreemfs_update_file_sizeRequest"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        xcap.serialize( writer );
        new_file_size.serialize( writer );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        xcap = new org.xtreemfs.interfaces.XCap(); xcap.deserialize( buf );
        new_file_size = new org.xtreemfs.interfaces.OSDWriteResponse(); new_file_size.deserialize( buf );    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += xcap.calculateSize();
        my_size += new_file_size.calculateSize();
        return my_size;
    }

    private XCap xcap;
    private OSDWriteResponse new_file_size;
    

    // Request
    public int getInterfaceVersion() { return 2; }    
    public int getOperationNumber() { return 29; }
    public Response createDefaultResponse() { return new xtreemfs_update_file_sizeResponse(); }

}

