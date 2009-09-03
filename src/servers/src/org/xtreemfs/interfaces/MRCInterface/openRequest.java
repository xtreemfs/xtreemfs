package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class openRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009090420;
    
    public openRequest() {  }
    public openRequest( String path, int flags, int mode, int attributes ) { this.path = path; this.flags = flags; this.mode = mode; this.attributes = attributes; }

    public String getPath() { return path; }
    public void setPath( String path ) { this.path = path; }
    public int getFlags() { return flags; }
    public void setFlags( int flags ) { this.flags = flags; }
    public int getMode() { return mode; }
    public void setMode( int mode ) { this.mode = mode; }
    public int getAttributes() { return attributes; }
    public void setAttributes( int attributes ) { this.attributes = attributes; }

    // Request
    public Response createDefaultResponse() { return new openResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009090420;    

    // yidl.Object
    public int getTag() { return 2009090420; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::openRequest"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += path != null ? ( ( path.getBytes().length + Integer.SIZE/8 ) % 4 == 0 ) ? ( path.getBytes().length + Integer.SIZE/8 ) : ( path.getBytes().length + Integer.SIZE/8 + 4 - ( path.getBytes().length + Integer.SIZE/8 ) % 4 ) : 0;
        my_size += ( Integer.SIZE / 8 );
        my_size += ( Integer.SIZE / 8 );
        my_size += ( Integer.SIZE / 8 );
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "path", path );
        marshaller.writeUint32( "flags", flags );
        marshaller.writeUint32( "mode", mode );
        marshaller.writeUint32( "attributes", attributes );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        path = unmarshaller.readString( "path" );
        flags = unmarshaller.readUint32( "flags" );
        mode = unmarshaller.readUint32( "mode" );
        attributes = unmarshaller.readUint32( "attributes" );    
    }
        
    

    private String path;
    private int flags;
    private int mode;
    private int attributes;    

}

