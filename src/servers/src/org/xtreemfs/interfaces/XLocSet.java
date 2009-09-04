package org.xtreemfs.interfaces;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class XLocSet extends Struct
{
    public static final int TAG = 2009090232;
    
    public XLocSet() { replicas = new ReplicaSet();  }
    public XLocSet( ReplicaSet replicas, int version, String repUpdatePolicy, long read_only_file_size ) { this.replicas = replicas; this.version = version; this.repUpdatePolicy = repUpdatePolicy; this.read_only_file_size = read_only_file_size; }

    public ReplicaSet getReplicas() { return replicas; }
    public void setReplicas( ReplicaSet replicas ) { this.replicas = replicas; }
    public int getVersion() { return version; }
    public void setVersion( int version ) { this.version = version; }
    public String getRepUpdatePolicy() { return repUpdatePolicy; }
    public void setRepUpdatePolicy( String repUpdatePolicy ) { this.repUpdatePolicy = repUpdatePolicy; }
    public long getRead_only_file_size() { return read_only_file_size; }
    public void setRead_only_file_size( long read_only_file_size ) { this.read_only_file_size = read_only_file_size; }

    // java.io.Serializable
    public static final long serialVersionUID = 2009090232;    

    // yidl.Object
    public int getTag() { return 2009090232; }
    public String getTypeName() { return "org::xtreemfs::interfaces::XLocSet"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += replicas.getXDRSize(); // replicas
        my_size += Integer.SIZE / 8; // version
        my_size += Integer.SIZE / 8 + ( repUpdatePolicy != null ? ( ( repUpdatePolicy.getBytes().length % 4 == 0 ) ? repUpdatePolicy.getBytes().length : ( repUpdatePolicy.getBytes().length + 4 - repUpdatePolicy.getBytes().length % 4 ) ) : 0 ); // repUpdatePolicy
        my_size += Long.SIZE / 8; // read_only_file_size
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeSequence( "replicas", replicas );
        marshaller.writeUint32( "version", version );
        marshaller.writeString( "repUpdatePolicy", repUpdatePolicy );
        marshaller.writeUint64( "read_only_file_size", read_only_file_size );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        replicas = new ReplicaSet(); unmarshaller.readSequence( "replicas", replicas );
        version = unmarshaller.readUint32( "version" );
        repUpdatePolicy = unmarshaller.readString( "repUpdatePolicy" );
        read_only_file_size = unmarshaller.readUint64( "read_only_file_size" );    
    }
        
    

    private ReplicaSet replicas;
    private int version;
    private String repUpdatePolicy;
    private long read_only_file_size;    

}

