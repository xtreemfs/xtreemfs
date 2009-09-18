package org.xtreemfs.interfaces.MRCInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.PrettyPrinter;
import yidl.Struct;
import yidl.Unmarshaller;




public class xtreemfs_replica_listResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2009090449;
    
    public xtreemfs_replica_listResponse() { replicas = new ReplicaSet();  }
    public xtreemfs_replica_listResponse( ReplicaSet replicas ) { this.replicas = replicas; }

    public ReplicaSet getReplicas() { return replicas; }
    public void setReplicas( ReplicaSet replicas ) { this.replicas = replicas; }

    // java.lang.Object
    public String toString() 
    { 
        StringWriter string_writer = new StringWriter();
        PrettyPrinter pretty_printer = new PrettyPrinter( string_writer );
        pretty_printer.writeStruct( "", this );
        return string_writer.toString();
    }


    // java.io.Serializable
    public static final long serialVersionUID = 2009090449;    

    // yidl.Object
    public int getTag() { return 2009090449; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_replica_listResponse"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += replicas.getXDRSize(); // replicas
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeSequence( "replicas", replicas );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        replicas = new ReplicaSet(); unmarshaller.readSequence( "replicas", replicas );    
    }
        
    

    private ReplicaSet replicas;    

}

