package org.xtreemfs.interfaces.DIRInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class xtreemfs_service_offlineRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009082730;
    
    public xtreemfs_service_offlineRequest() {  }
    public xtreemfs_service_offlineRequest( String uuid ) { this.uuid = uuid; }

    public String getUuid() { return uuid; }
    public void setUuid( String uuid ) { this.uuid = uuid; }

    // Request
    public Response createDefaultResponse() { return new xtreemfs_service_offlineResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009082730;    

    // yidl.Object
    public int getTag() { return 2009082730; }
    public String getTypeName() { return "org::xtreemfs::interfaces::DIRInterface::xtreemfs_service_offlineRequest"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += ( ( uuid.getBytes().length + Integer.SIZE/8 ) % 4 == 0 ) ? ( uuid.getBytes().length + Integer.SIZE/8 ) : ( uuid.getBytes().length + Integer.SIZE/8 + 4 - ( uuid.getBytes().length + Integer.SIZE/8 ) % 4 );
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "uuid", uuid );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        uuid = unmarshaller.readString( "uuid" );    
    }
        
    

    private String uuid;    

}

