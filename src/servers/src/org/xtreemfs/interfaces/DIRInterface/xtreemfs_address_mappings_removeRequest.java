package org.xtreemfs.interfaces.DIRInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class xtreemfs_address_mappings_removeRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009082720;
    
    public xtreemfs_address_mappings_removeRequest() {  }
    public xtreemfs_address_mappings_removeRequest( String uuid ) { this.uuid = uuid; }

    public String getUuid() { return uuid; }
    public void setUuid( String uuid ) { this.uuid = uuid; }

    // Request
    public Response createDefaultResponse() { return new xtreemfs_address_mappings_removeResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009082720;    

    // yidl.Object
    public int getTag() { return 2009082720; }
    public String getTypeName() { return "org::xtreemfs::interfaces::DIRInterface::xtreemfs_address_mappings_removeRequest"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE/8 + ( uuid != null ? ( ( uuid.getBytes().length % 4 == 0 ) ? uuid.getBytes().length : ( uuid.getBytes().length + 4 - uuid.getBytes().length % 4 ) ) : 0 );
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

