package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_replica_addRequest implements org.xtreemfs.interfaces.utils.Request
{
    public xtreemfs_replica_addRequest() { context = new Context(); file_id = ""; new_replica = new Replica(); }
    public xtreemfs_replica_addRequest( Context context, String file_id, Replica new_replica ) { this.context = context; this.file_id = file_id; this.new_replica = new_replica; }
    public xtreemfs_replica_addRequest( Object from_hash_map ) { context = new Context(); file_id = ""; new_replica = new Replica(); this.deserialize( from_hash_map ); }
    public xtreemfs_replica_addRequest( Object[] from_array ) { context = new Context(); file_id = ""; new_replica = new Replica();this.deserialize( from_array ); }

    public Context getContext() { return context; }
    public void setContext( Context context ) { this.context = context; }
    public String getFile_id() { return file_id; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }
    public Replica getNew_replica() { return new_replica; }
    public void setNew_replica( Replica new_replica ) { this.new_replica = new_replica; }

    // Serializable
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_replica_addRequest"; }    
    public long getTypeId() { return 26; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.context.deserialize( from_hash_map.get( "context" ) );
        this.file_id = ( String )from_hash_map.get( "file_id" );
        this.new_replica.deserialize( from_hash_map.get( "new_replica" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.context.deserialize( from_array[0] );
        this.file_id = ( String )from_array[1];
        this.new_replica.deserialize( from_array[2] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        context = new Context(); context.deserialize( buf );
        file_id = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        new_replica = new Replica(); new_replica.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "context", context.serialize() );
        to_hash_map.put( "file_id", file_id );
        to_hash_map.put( "new_replica", new_replica.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        context.serialize( writer );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( file_id, writer );
        new_replica.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += context.calculateSize();
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(file_id);
        my_size += new_replica.calculateSize();
        return my_size;
    }

    // Request
    public int getOperationNumber() { return 26; }
    public Response createDefaultResponse() { return new xtreemfs_replica_addResponse(); }


    private Context context;
    private String file_id;
    private Replica new_replica;

}

