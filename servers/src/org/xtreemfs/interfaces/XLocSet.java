package org.xtreemfs.interfaces;

import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         
   
public class XLocSet implements org.xtreemfs.interfaces.utils.Serializable
{
    public XLocSet() { replicas = new org.xtreemfs.interfaces.ReplicaSet(); version = 0; repUpdatePolicy = ""; read_only_file_size = 0; }
    public XLocSet( ReplicaSet replicas, int version, String repUpdatePolicy, long read_only_file_size ) { this.replicas = replicas; this.version = version; this.repUpdatePolicy = repUpdatePolicy; this.read_only_file_size = read_only_file_size; }

    public ReplicaSet getReplicas() { return replicas; }
    public void setReplicas( ReplicaSet replicas ) { this.replicas = replicas; }
    public int getVersion() { return version; }
    public void setVersion( int version ) { this.version = version; }
    public String getRepUpdatePolicy() { return repUpdatePolicy; }
    public void setRepUpdatePolicy( String repUpdatePolicy ) { this.repUpdatePolicy = repUpdatePolicy; }
    public long getRead_only_file_size() { return read_only_file_size; }
    public void setRead_only_file_size( long read_only_file_size ) { this.read_only_file_size = read_only_file_size; }

    // Object
    public String toString()
    {
        return "XLocSet( " + replicas.toString() + ", " + Integer.toString( version ) + ", " + "\"" + repUpdatePolicy + "\"" + ", " + Long.toString( read_only_file_size ) + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::XLocSet"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        replicas.serialize( writer );
        writer.putInt( version );
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(repUpdatePolicy,writer); }
        writer.putLong( read_only_file_size );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        replicas = new org.xtreemfs.interfaces.ReplicaSet(); replicas.deserialize( buf );
        version = buf.getInt();
        { repUpdatePolicy = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        read_only_file_size = buf.getLong();    
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

