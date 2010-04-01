package org.xtreemfs.interfaces.MRCInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.oncrpc.utils.*;
import org.xtreemfs.interfaces.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class renameRequest extends org.xtreemfs.foundation.oncrpc.utils.Request
{
    public static final int TAG = 2010031129;

    public renameRequest() {  }
    public renameRequest( String volume_name, String source_path, String target_path ) { this.volume_name = volume_name; this.source_path = source_path; this.target_path = target_path; }

    public String getVolume_name() { return volume_name; }
    public String getSource_path() { return source_path; }
    public String getTarget_path() { return target_path; }
    public void setVolume_name( String volume_name ) { this.volume_name = volume_name; }
    public void setSource_path( String source_path ) { this.source_path = source_path; }
    public void setTarget_path( String target_path ) { this.target_path = target_path; }

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
    public Response createDefaultResponse() { return new renameResponse(); }

    // java.io.Serializable
    public static final long serialVersionUID = 2010031129;

    // yidl.runtime.Object
    public int getTag() { return 2010031129; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::renameRequest"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8 + ( volume_name != null ? ( ( volume_name.getBytes().length % 4 == 0 ) ? volume_name.getBytes().length : ( volume_name.getBytes().length + 4 - volume_name.getBytes().length % 4 ) ) : 0 ); // volume_name
        my_size += Integer.SIZE / 8 + ( source_path != null ? ( ( source_path.getBytes().length % 4 == 0 ) ? source_path.getBytes().length : ( source_path.getBytes().length + 4 - source_path.getBytes().length % 4 ) ) : 0 ); // source_path
        my_size += Integer.SIZE / 8 + ( target_path != null ? ( ( target_path.getBytes().length % 4 == 0 ) ? target_path.getBytes().length : ( target_path.getBytes().length + 4 - target_path.getBytes().length % 4 ) ) : 0 ); // target_path
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "volume_name", volume_name );
        marshaller.writeString( "source_path", source_path );
        marshaller.writeString( "target_path", target_path );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        volume_name = unmarshaller.readString( "volume_name" );
        source_path = unmarshaller.readString( "source_path" );
        target_path = unmarshaller.readString( "target_path" );
    }

    private String volume_name;
    private String source_path;
    private String target_path;
}
