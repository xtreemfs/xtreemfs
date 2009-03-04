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


         

public class xtreemfs_replica_removeRequest implements Request
{
    public xtreemfs_replica_removeRequest() { context = new org.xtreemfs.interfaces.Context(); file_id = ""; osd_uuid = ""; }
    public xtreemfs_replica_removeRequest( Context context, String file_id, String osd_uuid ) { this.context = context; this.file_id = file_id; this.osd_uuid = osd_uuid; }

    public Context getContext() { return context; }
    public void setContext( Context context ) { this.context = context; }
    public String getFile_id() { return file_id; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }
    public String getOsd_uuid() { return osd_uuid; }
    public void setOsd_uuid( String osd_uuid ) { this.osd_uuid = osd_uuid; }

    // Object
    public String toString()
    {
        return "xtreemfs_replica_removeRequest( " + context.toString() + ", " + "\"" + file_id + "\"" + ", " + "\"" + osd_uuid + "\"" + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::MRCInterface::xtreemfs_replica_removeRequest"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        context.serialize( writer );
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(file_id,writer); }
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(osd_uuid,writer); }        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        context = new org.xtreemfs.interfaces.Context(); context.deserialize( buf );
        { file_id = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        { osd_uuid = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += context.calculateSize();
        my_size += 4 + ( file_id.length() + 4 - ( file_id.length() % 4 ) );
        my_size += 4 + ( osd_uuid.length() + 4 - ( osd_uuid.length() % 4 ) );
        return my_size;
    }

    private Context context;
    private String file_id;
    private String osd_uuid;
    

    // Request
    public int getInterfaceVersion() { return 2; }    
    public int getOperationNumber() { return 27; }
    public Response createDefaultResponse() { return new xtreemfs_replica_removeResponse(); }

}

