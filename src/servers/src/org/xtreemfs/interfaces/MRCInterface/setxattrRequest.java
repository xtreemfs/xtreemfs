package org.xtreemfs.interfaces.MRCInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class setxattrRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2010031132;

    public setxattrRequest() {  }
    public setxattrRequest( String volume_name, String path, String name, String value, int flags ) { this.volume_name = volume_name; this.path = path; this.name = name; this.value = value; this.flags = flags; }

    public String getVolume_name() { return volume_name; }
    public void setVolume_name( String volume_name ) { this.volume_name = volume_name; }
    public String getPath() { return path; }
    public void setPath( String path ) { this.path = path; }
    public String getName() { return name; }
    public void setName( String name ) { this.name = name; }
    public String getValue() { return value; }
    public void setValue( String value ) { this.value = value; }
    public int getFlags() { return flags; }
    public void setFlags( int flags ) { this.flags = flags; }

    // java.lang.Object
    public String toString() 
    { 
        StringWriter string_writer = new StringWriter();
        string_writer.append(this.getClass().getCanonicalName());
        string_writer.append(" ");
        PrettyPrinter pretty_printer = new PrettyPrinter( string_writer );
        pretty_printer.writeStruct( "", this );
        return string_writer.toString();
    }

    // Request
    public Response createDefaultResponse() { return new setxattrResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2010031132;

    // yidl.runtime.Object
    public int getTag() { return 2010031132; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::setxattrRequest"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8 + ( volume_name != null ? ( ( volume_name.getBytes().length % 4 == 0 ) ? volume_name.getBytes().length : ( volume_name.getBytes().length + 4 - volume_name.getBytes().length % 4 ) ) : 0 ); // volume_name
        my_size += Integer.SIZE / 8 + ( path != null ? ( ( path.getBytes().length % 4 == 0 ) ? path.getBytes().length : ( path.getBytes().length + 4 - path.getBytes().length % 4 ) ) : 0 ); // path
        my_size += Integer.SIZE / 8 + ( name != null ? ( ( name.getBytes().length % 4 == 0 ) ? name.getBytes().length : ( name.getBytes().length + 4 - name.getBytes().length % 4 ) ) : 0 ); // name
        my_size += Integer.SIZE / 8 + ( value != null ? ( ( value.getBytes().length % 4 == 0 ) ? value.getBytes().length : ( value.getBytes().length + 4 - value.getBytes().length % 4 ) ) : 0 ); // value
        my_size += Integer.SIZE / 8; // flags
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "volume_name", volume_name );
        marshaller.writeString( "path", path );
        marshaller.writeString( "name", name );
        marshaller.writeString( "value", value );
        marshaller.writeInt32( "flags", flags );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        volume_name = unmarshaller.readString( "volume_name" );
        path = unmarshaller.readString( "path" );
        name = unmarshaller.readString( "name" );
        value = unmarshaller.readString( "value" );
        flags = unmarshaller.readInt32( "flags" );
    }

    

    private String volume_name;
    private String path;
    private String name;
    private String value;
    private int flags;

}

