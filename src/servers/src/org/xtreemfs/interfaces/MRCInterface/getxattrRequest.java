package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class getxattrRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009090416;
    
    public getxattrRequest() {  }
    public getxattrRequest( String path, String name ) { this.path = path; this.name = name; }

    public String getPath() { return path; }
    public void setPath( String path ) { this.path = path; }
    public String getName() { return name; }
    public void setName( String name ) { this.name = name; }

    // Request
    public Response createDefaultResponse() { return new getxattrResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009090416;    

    // yidl.Object
    public int getTag() { return 2009090416; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::getxattrRequest"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += path != null ? ( ( path.getBytes().length + Integer.SIZE/8 ) % 4 == 0 ) ? ( path.getBytes().length + Integer.SIZE/8 ) : ( path.getBytes().length + Integer.SIZE/8 + 4 - ( path.getBytes().length + Integer.SIZE/8 ) % 4 ) : 0;
        my_size += name != null ? ( ( name.getBytes().length + Integer.SIZE/8 ) % 4 == 0 ) ? ( name.getBytes().length + Integer.SIZE/8 ) : ( name.getBytes().length + Integer.SIZE/8 + 4 - ( name.getBytes().length + Integer.SIZE/8 ) % 4 ) : 0;
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

