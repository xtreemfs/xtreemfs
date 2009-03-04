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


         

public class xtreemfs_check_file_existsRequest implements Request
{
    public xtreemfs_check_file_existsRequest() { volume_id = ""; file_ids = new org.xtreemfs.interfaces.StringSet(); }
    public xtreemfs_check_file_existsRequest( String volume_id, StringSet file_ids ) { this.volume_id = volume_id; this.file_ids = file_ids; }

    public String getVolume_id() { return volume_id; }
    public void setVolume_id( String volume_id ) { this.volume_id = volume_id; }
    public StringSet getFile_ids() { return file_ids; }
    public void setFile_ids( StringSet file_ids ) { this.file_ids = file_ids; }

    // Object
    public String toString()
    {
        return "xtreemfs_check_file_existsRequest( " + "\"" + volume_id + "\"" + ", " + file_ids.toString() + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::MRCInterface::xtreemfs_check_file_existsRequest"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(volume_id,writer); }
        file_ids.serialize( writer );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        { volume_id = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        file_ids = new org.xtreemfs.interfaces.StringSet(); file_ids.deserialize( buf );    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += 4 + ( volume_id.length() + 4 - ( volume_id.length() % 4 ) );
        my_size += file_ids.calculateSize();
        return my_size;
    }

    private String volume_id;
    private StringSet file_ids;
    

    // Request
    public int getInterfaceVersion() { return 2; }    
    public int getOperationNumber() { return 23; }
    public Response createDefaultResponse() { return new xtreemfs_check_file_existsResponse(); }

}

