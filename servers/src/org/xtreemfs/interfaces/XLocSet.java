package org.xtreemfs.interfaces;

import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class XLocSet implements org.xtreemfs.interfaces.utils.Serializable
{
    public XLocSet() { replicas = new ReplicaSet(); version = 0; repUpdatePolicy = ""; read_only_file_size = 0; }
    public XLocSet( ReplicaSet replicas, int version, String repUpdatePolicy, long read_only_file_size ) { this.replicas = replicas; this.version = version; this.repUpdatePolicy = repUpdatePolicy; this.read_only_file_size = read_only_file_size; }
    public XLocSet( Object from_hash_map ) { replicas = new ReplicaSet(); version = 0; repUpdatePolicy = ""; read_only_file_size = 0; this.deserialize( from_hash_map ); }
    public XLocSet( Object[] from_array ) { replicas = new ReplicaSet(); version = 0; repUpdatePolicy = ""; read_only_file_size = 0;this.deserialize( from_array ); }

    public ReplicaSet getReplicas() { return replicas; }
    public void setReplicas( ReplicaSet replicas ) { this.replicas = replicas; }
    public int getVersion() { return version; }
    public void setVersion( int version ) { this.version = version; }
    public String getRepUpdatePolicy() { return repUpdatePolicy; }
    public void setRepUpdatePolicy( String repUpdatePolicy ) { this.repUpdatePolicy = repUpdatePolicy; }
    public long getRead_only_file_size() { return read_only_file_size; }
    public void setRead_only_file_size( long read_only_file_size ) { this.read_only_file_size = read_only_file_size; }

    public String getTypeName() { return "org::xtreemfs::interfaces::XLocSet"; }    
    public long getTypeId() { return 0; }

    public String toString()
    {
        return "XLocSet( " + replicas.toString() + ", " + Integer.toString( version ) + ", " + "\"" + repUpdatePolicy + "\"" + ", " + Long.toString( read_only_file_size ) + " )"; 
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.replicas.deserialize( from_hash_map.get( "replicas" ) );
        this.version = ( ( Integer )from_hash_map.get( "version" ) ).intValue();
        this.repUpdatePolicy = ( String )from_hash_map.get( "repUpdatePolicy" );
        this.read_only_file_size = ( ( Long )from_hash_map.get( "read_only_file_size" ) ).longValue();
    }
    
    public void deserialize( Object[] from_array )
    {
        this.replicas.deserialize( from_array[0] );
        this.version = ( ( Integer )from_array[1] ).intValue();
        this.repUpdatePolicy = ( String )from_array[2];
        this.read_only_file_size = ( ( Long )from_array[3] ).longValue();        
    }

    public void deserialize( ReusableBuffer buf )
    {
        replicas = new ReplicaSet(); replicas.deserialize( buf );
        version = buf.getInt();
        repUpdatePolicy = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        read_only_file_size = buf.getLong();
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "replicas", replicas.serialize() );
        to_hash_map.put( "version", new Integer( version ) );
        to_hash_map.put( "repUpdatePolicy", repUpdatePolicy );
        to_hash_map.put( "read_only_file_size", new Long( read_only_file_size ) );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        replicas.serialize( writer );
        writer.putInt( version );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( repUpdatePolicy, writer );
        writer.putLong( read_only_file_size );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += replicas.calculateSize();
        my_size += ( Integer.SIZE / 8 );
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(repUpdatePolicy);
        my_size += ( Long.SIZE / 8 );
        return my_size;
    }


    private ReplicaSet replicas;
    private int version;
    private String repUpdatePolicy;
    private long read_only_file_size;

}

