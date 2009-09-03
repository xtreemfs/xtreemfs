package org.xtreemfs.interfaces.DIRInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class xtreemfs_service_get_by_nameRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009082727;
    
    public xtreemfs_service_get_by_nameRequest() {  }
    public xtreemfs_service_get_by_nameRequest( String name ) { this.name = name; }

    public String getName() { return name; }
    public void setName( String name ) { this.name = name; }

    // Request
    public Response createDefaultResponse() { return new xtreemfs_service_get_by_nameResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009082727;    

    // yidl.Object
    public int getTag() { return 2009082727; }
    public String getTypeName() { return "org::xtreemfs::interfaces::DIRInterface::xtreemfs_service_get_by_nameRequest"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE/8 + ( name != null ? ( ( name.getBytes().length % 4 == 0 ) ? name.getBytes().length : ( name.getBytes().length + 4 - name.getBytes().length % 4 ) ) : 0 );
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "name", name );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        name = unmarshaller.readString( "name" );    
    }
        
    

    private String name;    

}

