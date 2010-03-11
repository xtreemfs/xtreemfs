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




public class openRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2010031125;

    public openRequest() { client_vivaldi_coordinates = new VivaldiCoordinates();  }
    public openRequest( String volume_name, String path, int flags, int mode, int attributes, VivaldiCoordinates client_vivaldi_coordinates ) { this.volume_name = volume_name; this.path = path; this.flags = flags; this.mode = mode; this.attributes = attributes; this.client_vivaldi_coordinates = client_vivaldi_coordinates; }

    public String getVolume_name() { return volume_name; }
    public String getPath() { return path; }
    public int getFlags() { return flags; }
    public int getMode() { return mode; }
    public int getAttributes() { return attributes; }
    public VivaldiCoordinates getClient_vivaldi_coordinates() { return client_vivaldi_coordinates; }
    public void setVolume_name( String volume_name ) { this.volume_name = volume_name; }
    public void setPath( String path ) { this.path = path; }
    public void setFlags( int flags ) { this.flags = flags; }
    public void setMode( int mode ) { this.mode = mode; }
    public void setAttributes( int attributes ) { this.attributes = attributes; }
    public void setClient_vivaldi_coordinates( VivaldiCoordinates client_vivaldi_coordinates ) { this.client_vivaldi_coordinates = client_vivaldi_coordinates; }

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
    public Response createDefaultResponse() { return new openResponse(); }

    // java.io.Serializable
    public static final long serialVersionUID = 2010031125;

    // yidl.runtime.Object
    public int getTag() { return 2010031125; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::openRequest"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8 + ( volume_name != null ? ( ( volume_name.getBytes().length % 4 == 0 ) ? volume_name.getBytes().length : ( volume_name.getBytes().length + 4 - volume_name.getBytes().length % 4 ) ) : 0 ); // volume_name
        my_size += Integer.SIZE / 8 + ( path != null ? ( ( path.getBytes().length % 4 == 0 ) ? path.getBytes().length : ( path.getBytes().length + 4 - path.getBytes().length % 4 ) ) : 0 ); // path
        my_size += Integer.SIZE / 8; // flags
        my_size += Integer.SIZE / 8; // mode
        my_size += Integer.SIZE / 8; // attributes
        my_size += client_vivaldi_coordinates.getXDRSize(); // client_vivaldi_coordinates
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "volume_name", volume_name );
        marshaller.writeString( "path", path );
        marshaller.writeUint32( "flags", flags );
        marshaller.writeUint32( "mode", mode );
        marshaller.writeUint32( "attributes", attributes );
        marshaller.writeStruct( "client_vivaldi_coordinates", client_vivaldi_coordinates );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        volume_name = unmarshaller.readString( "volume_name" );
        path = unmarshaller.readString( "path" );
        flags = unmarshaller.readUint32( "flags" );
        mode = unmarshaller.readUint32( "mode" );
        attributes = unmarshaller.readUint32( "attributes" );
        client_vivaldi_coordinates = new VivaldiCoordinates(); unmarshaller.readStruct( "client_vivaldi_coordinates", client_vivaldi_coordinates );
    }

    private String volume_name;
    private String path;
    private int flags;
    private int mode;
    private int attributes;
    private VivaldiCoordinates client_vivaldi_coordinates;
}
