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


         

public class statfsResponse implements Response
{
    public statfsResponse() { statfsbuf = new org.xtreemfs.interfaces.statfs_(); }
    public statfsResponse( statfs_ statfsbuf ) { this.statfsbuf = statfsbuf; }

    public statfs_ getStatfsbuf() { return statfsbuf; }
    public void setStatfsbuf( statfs_ statfsbuf ) { this.statfsbuf = statfsbuf; }

    // Object
    public String toString()
    {
        return "statfsResponse( " + statfsbuf.toString() + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::MRCInterface::statfsResponse"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        statfsbuf.serialize( writer );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        statfsbuf = new org.xtreemfs.interfaces.statfs_(); statfsbuf.deserialize( buf );    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += statfsbuf.calculateSize();
        return my_size;
    }

    private statfs_ statfsbuf;
    

    // Response
    public int getInterfaceVersion() { return 2; }
    public int getOperationNumber() { return 19; }    

}

