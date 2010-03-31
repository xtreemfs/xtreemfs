package org.xtreemfs.interfaces.MRCInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class linkRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2010031122;

    public linkRequest() {  }
    public linkRequest( String volume_name, String target_path, String link_path ) { this.volume_name = volume_name; this.target_path = target_path; this.link_path = link_path; }

    public String getVolume_name() { return volume_name; }
    public String getTarget_path() { return target_path; }
    public String getLink_path() { return link_path; }
    public void setVolume_name( String volume_name ) { this.volume_name = volume_name; }
    public void setTarget_path( String target_path ) { this.target_path = target_path; }
    public void setLink_path( String link_path ) { this.link_path = link_path; }

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
    public Response createDefaultResponse() { return new linkResponse(); }

    // java.io.Serializable
    public static final long serialVersionUID = 2010031122;

    // yidl.runtime.Object
    public int getTag() { return 2010031122; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::linkRequest"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8 + ( volume_name != null ? ( ( volume_name.getBytes().length % 4 == 0 ) ? volume_name.getBytes().length : ( volume_name.getBytes().length + 4 - volume_name.getBytes().length % 4 ) ) : 0 ); // volume_name
        my_size += Integer.SIZE / 8 + ( target_path != null ? ( ( target_path.getBytes().length % 4 == 0 ) ? target_path.getBytes().length : ( target_path.getBytes().length + 4 - target_path.getBytes().length % 4 ) ) : 0 ); // target_path
        my_size += Integer.SIZE / 8 + ( link_path != null ? ( ( link_path.getBytes().length % 4 == 0 ) ? link_path.getBytes().length : ( link_path.getBytes().length + 4 - link_path.getBytes().length % 4 ) ) : 0 ); // link_path
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "volume_name", volume_name );
        marshaller.writeString( "target_path", target_path );
        marshaller.writeString( "link_path", link_path );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        volume_name = unmarshaller.readString( "volume_name" );
        target_path = unmarshaller.readString( "target_path" );
        link_path = unmarshaller.readString( "link_path" );
    }

    private String volume_name;
    private String target_path;
    private String link_path;
}
