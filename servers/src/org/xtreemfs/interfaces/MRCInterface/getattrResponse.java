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


         

public class getattrResponse implements Response
{
    public getattrResponse() { stbuf = new org.xtreemfs.interfaces.stat_(); }
    public getattrResponse( stat_ stbuf ) { this.stbuf = stbuf; }

    public stat_ getStbuf() { return stbuf; }
    public void setStbuf( stat_ stbuf ) { this.stbuf = stbuf; }

    // Object
    public String toString()
    {
        return "getattrResponse( " + stbuf.toString() + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::MRCInterface::getattrResponse"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        stbuf.serialize( writer );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        stbuf = new org.xtreemfs.interfaces.stat_(); stbuf.deserialize( buf );    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += stbuf.calculateSize();
        return my_size;
    }

    private stat_ stbuf;
    

    // Response
    public int getInterfaceVersion() { return 2; }
    public int getOperationNumber() { return 5; }    

}

