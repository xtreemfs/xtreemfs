package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class setxattrRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009082835;
    
    public setxattrRequest() {  }
    public setxattrRequest( String path, String name, String value, int flags ) { this.path = path; this.name = name; this.value = value; this.flags = flags; }

    public String getPath() { return path; }
    public void setPath( String path ) { this.path = path; }
    public String getName() { return name; }
    public void setName( String name ) { this.name = name; }
    public String getValue() { return value; }
    public void setValue( String value ) { this.value = value; }
    public int getFlags() { return flags; }
    public void setFlags( int flags ) { this.flags = flags; }

    // Request
    public Response createDefaultResponse() { return new setxattrResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009082835;    

    // yidl.Object
    public int getTag() { return 2009082835; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::setxattrRequest"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += ( ( path.getBytes().length + Integer.SIZE/8 ) % 4 == 0 ) ? ( path.getBytes().length + Integer.SIZE/8 ) : ( path.getBytes().length + Integer.SIZE/8 + 4 - ( path.getBytes().length + Integer.SIZE/8 ) % 4 );
        my_size += ( ( name.getBytes().length + Integer.SIZE/8 ) % 4 == 0 ) ? ( name.getBytes().length + Integer.SIZE/8 ) : ( name.getBytes().length + Integer.SIZE/8 + 4 - ( name.getBytes().length + Integer.SIZE/8 ) % 4 );
        my_size += ( ( value.getBytes().length + Integer.SIZE/8 ) % 4 == 0 ) ? ( value.getBytes().length + Integer.SIZE/8 ) : ( value.getBytes().length + Integer.SIZE/8 + 4 - ( value.getBytes().length + Integer.SIZE/8 ) % 4 );
        my_size += ( Integer.SIZE / 8 );
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "path", path );
        marshaller.writeString( "name", name );
        marshaller.writeString( "value", value );
        marshaller.writeInt32( "flags", flags );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        path = unmarshaller.readString( "path" );
        name = unmarshaller.readString( "name" );
        value = unmarshaller.readString( "value" );
        flags = unmarshaller.readInt32( "flags" );    
    }
        
    

    private String path;
    private String name;
    private String value;
    private int flags;    

}

