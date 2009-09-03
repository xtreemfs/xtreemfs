package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class xtreemfs_replica_addRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009090448;
    
    public xtreemfs_replica_addRequest() { new_replica = new Replica();  }
    public xtreemfs_replica_addRequest( String file_id, Replica new_replica ) { this.file_id = file_id; this.new_replica = new_replica; }

    public String getFile_id() { return file_id; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }
    public Replica getNew_replica() { return new_replica; }
    public void setNew_replica( Replica new_replica ) { this.new_replica = new_replica; }

    // Request
    public Response createDefaultResponse() { return new xtreemfs_replica_addResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009090448;    

    // yidl.Object
    public int getTag() { return 2009090448; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_replica_addRequest"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += file_id != null ? ( ( file_id.getBytes().length + Integer.SIZE/8 ) % 4 == 0 ) ? ( file_id.getBytes().length + Integer.SIZE/8 ) : ( file_id.getBytes().length + Integer.SIZE/8 + 4 - ( file_id.getBytes().length + Integer.SIZE/8 ) % 4 ) : 0;
        my_size += new_replica.getXDRSize();
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "file_id", file_id );
        marshaller.writeStruct( "new_replica", new_replica );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        file_id = unmarshaller.readString( "file_id" );
        new_replica = new Replica(); unmarshaller.readStruct( "new_replica", new_replica );    
    }
        
    

    private String file_id;
    private Replica new_replica;    

}

