package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_replica_listResponse implements org.xtreemfs.interfaces.utils.Response
{
    public xtreemfs_replica_listResponse() { replicas = new ReplicaSet(); }
    public xtreemfs_replica_listResponse( ReplicaSet replicas ) { this.replicas = replicas; }
    public xtreemfs_replica_listResponse( Object from_hash_map ) { replicas = new ReplicaSet(); this.deserialize( from_hash_map ); }
    public xtreemfs_replica_listResponse( Object[] from_array ) { replicas = new ReplicaSet();this.deserialize( from_array ); }

    public ReplicaSet getReplicas() { return replicas; }
    public void setReplicas( ReplicaSet replicas ) { this.replicas = replicas; }

    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_replica_listResponse"; }    
    public long getTypeId() { return 32; }

    public String toString()
    {
        return "xtreemfs_replica_listResponse( " + replicas.toString() + " )";
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.replicas.deserialize( ( Object[] )from_hash_map.get( "replicas" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.replicas.deserialize( ( Object[] )from_array[0] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        replicas = new ReplicaSet(); replicas.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "replicas", replicas.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        replicas.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += replicas.calculateSize();
        return my_size;
    }

    // Response
    public int getOperationNumber() { return 32; }


    private ReplicaSet replicas;

}

