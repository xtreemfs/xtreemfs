package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class creatRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009090413;
    
    public creatRequest() {  }
    public creatRequest( String path, int mode ) { this.path = path; this.mode = mode; }

    public String getPath() { return path; }
    public void setPath( String path ) { this.path = path; }
    public int getMode() { return mode; }
    public void setMode( int mode ) { this.mode = mode; }

    // Request
    public Response createDefaultResponse() { return new creatResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009090413;    

    // yidl.Object
    public int getTag() { return 2009090413; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::creatRequest"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE/8 + ( path != null ? ( ( path.getBytes().length % 4 == 0 ) ? path.getBytes().length : ( path.getBytes().length + 4 - path.getBytes().length % 4 ) ) : 0 );
        my_size += ( Integer.SIZE / 8 );
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "path", path );
        marshaller.writeUint32( "mode", mode );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        path = unmarshaller.readString( "path" );
        mode = unmarshaller.readUint32( "mode" );    
    }
        
    

    private String path;
    private int mode;    

}

