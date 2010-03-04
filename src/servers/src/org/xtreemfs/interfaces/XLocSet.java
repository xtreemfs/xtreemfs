package org.xtreemfs.interfaces;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class XLocSet implements Struct
{
    public static final int TAG = 2010030338;

    public XLocSet() { replicas = new ReplicaSet();  }
    public XLocSet( long read_only_file_size, ReplicaSet replicas, String replica_update_policy, int version ) { this.read_only_file_size = read_only_file_size; this.replicas = replicas; this.replica_update_policy = replica_update_policy; this.version = version; }

    public long getRead_only_file_size() { return read_only_file_size; }
    public void setRead_only_file_size( long read_only_file_size ) { this.read_only_file_size = read_only_file_size; }
    public ReplicaSet getReplicas() { return replicas; }
    public void setReplicas( ReplicaSet replicas ) { this.replicas = replicas; }
    public String getReplica_update_policy() { return replica_update_policy; }
    public void setReplica_update_policy( String replica_update_policy ) { this.replica_update_policy = replica_update_policy; }
    public int getVersion() { return version; }
    public void setVersion( int version ) { this.version = version; }

    // java.lang.Object
    public String toString() 
    { 
        StringWriter string_writer = new StringWriter();
        string_writer.append(this.getClass().getCanonicalName());
        string_writer.append(" ");
        PrettyPrinter pretty_printer = new PrettyPrinter( string_writer );
        pretty_printer.writeStruct( "", this );
        return string_writer.toString();
    }


    // java.io.Serializable
    public static final long serialVersionUID = 2010030338;

    // yidl.runtime.Object
    public int getTag() { return 2010030338; }
    public String getTypeName() { return "org::xtreemfs::interfaces::XLocSet"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Long.SIZE / 8; // read_only_file_size
        my_size += replicas.getXDRSize(); // replicas
        my_size += Integer.SIZE / 8 + ( replica_update_policy != null ? ( ( replica_update_policy.getBytes().length % 4 == 0 ) ? replica_update_policy.getBytes().length : ( replica_update_policy.getBytes().length + 4 - replica_update_policy.getBytes().length % 4 ) ) : 0 ); // replica_update_policy
        my_size += Integer.SIZE / 8; // version
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeUint64( "read_only_file_size", read_only_file_size );
        marshaller.writeSequence( "replicas", replicas );
        marshaller.writeString( "replica_update_policy", replica_update_policy );
        marshaller.writeUint32( "version", version );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        read_only_file_size = unmarshaller.readUint64( "read_only_file_size" );
        replicas = new ReplicaSet(); unmarshaller.readSequence( "replicas", replicas );
        replica_update_policy = unmarshaller.readString( "replica_update_policy" );
        version = unmarshaller.readUint32( "version" );
    }

    

    private long read_only_file_size;
    private ReplicaSet replicas;
    private String replica_update_policy;
    private int version;

}

