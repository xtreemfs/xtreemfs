package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class removexattrRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009090422;
    
    public removexattrRequest() {  }
    public removexattrRequest( String path, String name ) { this.path = path; this.name = name; }

    public String getPath() { return path; }
    public void setPath( String path ) { this.path = path; }
    public String getName() { return name; }
    public void setName( String name ) { this.name = name; }

    // Request
    public Response createDefaultResponse() { return new removexattrResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009090422;    

    // yidl.Object
    public int getTag() { return 2009090422; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::removexattrRequest"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE/8 + ( path != null ? ( ( path.getBytes().length % 4 == 0 ) ? path.getBytes().length : ( path.getBytes().length + 4 - path.getBytes().length % 4 ) ) : 0 );
        my_size += Integer.SIZE/8 + ( name != null ? ( ( name.getBytes().length % 4 == 0 ) ? name.getBytes().length : ( name.getBytes().length + 4 - name.getBytes().length % 4 ) ) : 0 );
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "path", path );
        marshaller.writeString( "name", name );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        path = unmarshaller.readString( "path" );
        name = unmarshaller.readString( "name" );    
    }
        
    

    private String path;
    private String name;    

}

