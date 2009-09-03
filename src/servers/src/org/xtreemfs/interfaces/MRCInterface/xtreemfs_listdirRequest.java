package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class xtreemfs_listdirRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009082854;
    
    public xtreemfs_listdirRequest() {  }
    public xtreemfs_listdirRequest( String path ) { this.path = path; }

    public String getPath() { return path; }
    public void setPath( String path ) { this.path = path; }

    // Request
    public Response createDefaultResponse() { return new xtreemfs_listdirResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009082854;    

    // yidl.Object
    public int getTag() { return 2009082854; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_listdirRequest"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += ( ( path.getBytes().length + Integer.SIZE/8 ) % 4 == 0 ) ? ( path.getBytes().length + Integer.SIZE/8 ) : ( path.getBytes().length + Integer.SIZE/8 + 4 - ( path.getBytes().length + Integer.SIZE/8 ) % 4 );
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "path", path );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        path = unmarshaller.readString( "path" );    
    }
        
    

    private String path;    

}

