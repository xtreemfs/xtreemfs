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


         

public class xtreemfs_get_suitable_osdsRequest implements Request
{
    public xtreemfs_get_suitable_osdsRequest() { file_id = ""; }
    public xtreemfs_get_suitable_osdsRequest( String file_id ) { this.file_id = file_id; }

    public String getFile_id() { return file_id; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }

    // Object
    public String toString()
    {
        return "xtreemfs_get_suitable_osdsRequest( " + "\"" + file_id + "\"" + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::MRCInterface::xtreemfs_get_suitable_osdsRequest"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(file_id,writer); }        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        { file_id = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(file_id);
        return my_size;
    }

    private String file_id;
    

    // Request
    public int getInterfaceVersion() { return 2; }    
    public int getOperationNumber() { return 24; }
    public Response createDefaultResponse() { return new xtreemfs_get_suitable_osdsResponse(); }

}

