package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class chownRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009090412;
    
    public chownRequest() {  }
    public chownRequest( String path, String user_id, String group_id ) { this.path = path; this.user_id = user_id; this.group_id = group_id; }

    public String getPath() { return path; }
    public void setPath( String path ) { this.path = path; }
    public String getUser_id() { return user_id; }
    public void setUser_id( String user_id ) { this.user_id = user_id; }
    public String getGroup_id() { return group_id; }
    public void setGroup_id( String group_id ) { this.group_id = group_id; }

    // Request
    public Response createDefaultResponse() { return new chownResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009090412;    

    // yidl.Object
    public int getTag() { return 2009090412; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::chownRequest"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += path != null ? ( ( path.getBytes().length + Integer.SIZE/8 ) % 4 == 0 ) ? ( path.getBytes().length + Integer.SIZE/8 ) : ( path.getBytes().length + Integer.SIZE/8 + 4 - ( path.getBytes().length + Integer.SIZE/8 ) % 4 ) : 0;
        my_size += user_id != null ? ( ( user_id.getBytes().length + Integer.SIZE/8 ) % 4 == 0 ) ? ( user_id.getBytes().length + Integer.SIZE/8 ) : ( user_id.getBytes().length + Integer.SIZE/8 + 4 - ( user_id.getBytes().length + Integer.SIZE/8 ) % 4 ) : 0;
        my_size += group_id != null ? ( ( group_id.getBytes().length + Integer.SIZE/8 ) % 4 == 0 ) ? ( group_id.getBytes().length + Integer.SIZE/8 ) : ( group_id.getBytes().length + Integer.SIZE/8 + 4 - ( group_id.getBytes().length + Integer.SIZE/8 ) % 4 ) : 0;
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "path", path );
        marshaller.writeString( "user_id", user_id );
        marshaller.writeString( "group_id", group_id );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        path = unmarshaller.readString( "path" );
        user_id = unmarshaller.readString( "user_id" );
        group_id = unmarshaller.readString( "group_id" );    
    }
        
    

    private String path;
    private String user_id;
    private String group_id;    

}

