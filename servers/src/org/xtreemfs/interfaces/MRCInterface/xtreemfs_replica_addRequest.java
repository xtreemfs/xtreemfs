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


         

public class xtreemfs_replica_addRequest implements Request
{
    public xtreemfs_replica_addRequest() { context = new org.xtreemfs.interfaces.Context(); file_id = ""; new_replica = new org.xtreemfs.interfaces.Replica(); }
    public xtreemfs_replica_addRequest( Context context, String file_id, Replica new_replica ) { this.context = context; this.file_id = file_id; this.new_replica = new_replica; }

    public Context getContext() { return context; }
    public void setContext( Context context ) { this.context = context; }
    public String getFile_id() { return file_id; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }
    public Replica getNew_replica() { return new_replica; }
    public void setNew_replica( Replica new_replica ) { this.new_replica = new_replica; }

    // Object
    public String toString()
    {
        return "xtreemfs_replica_addRequest( " + context.toString() + ", " + "\"" + file_id + "\"" + ", " + new_replica.toString() + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::MRCInterface::xtreemfs_replica_addRequest"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        context.serialize( writer );
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(file_id,writer); }
        new_replica.serialize( writer );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        context = new org.xtreemfs.interfaces.Context(); context.deserialize( buf );
        { file_id = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        new_replica = new org.xtreemfs.interfaces.Replica(); new_replica.deserialize( buf );    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += context.calculateSize();
        my_size += 4 + ( file_id.length() + 4 - ( file_id.length() % 4 ) );
        my_size += new_replica.calculateSize();
        return my_size;
    }

    private Context context;
    private String file_id;
    private Replica new_replica;
    

    // Request
    public int getInterfaceVersion() { return 2; }    
    public int getOperationNumber() { return 26; }
    public Response createDefaultResponse() { return new xtreemfs_replica_addResponse(); }

}

