package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class xtreemfs_get_suitable_osdsRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009082851;
    
    public xtreemfs_get_suitable_osdsRequest() {  }
    public xtreemfs_get_suitable_osdsRequest( String file_id ) { this.file_id = file_id; }

    public String getFile_id() { return file_id; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }

    // Request
    public Response createDefaultResponse() { return new xtreemfs_get_suitable_osdsResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009082851;    

    // yidl.Object
    public int getTag() { return 2009082851; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_get_suitable_osdsRequest"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += ( ( file_id.getBytes().length + Integer.SIZE/8 ) % 4 == 0 ) ? ( file_id.getBytes().length + Integer.SIZE/8 ) : ( file_id.getBytes().length + Integer.SIZE/8 + 4 - ( file_id.getBytes().length + Integer.SIZE/8 ) % 4 );
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "file_id", file_id );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        file_id = unmarshaller.readString( "file_id" );    
    }
        
    

    private String file_id;    

}

