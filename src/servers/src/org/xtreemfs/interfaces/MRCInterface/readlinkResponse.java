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




public class readlinkResponse extends org.xtreemfs.foundation.oncrpc.utils.Response
{
    public static final int TAG = 2010031127;

    public readlinkResponse() {  }
    public readlinkResponse( String link_target_path ) { this.link_target_path = link_target_path; }

    public String getLink_target_path() { return link_target_path; }
    public void setLink_target_path( String link_target_path ) { this.link_target_path = link_target_path; }

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

    // java.io.Serializable
    public static final long serialVersionUID = 2010031127;

    // yidl.runtime.Object
    public int getTag() { return 2010031127; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::readlinkResponse"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8 + ( link_target_path != null ? ( ( link_target_path.getBytes().length % 4 == 0 ) ? link_target_path.getBytes().length : ( link_target_path.getBytes().length + 4 - link_target_path.getBytes().length % 4 ) ) : 0 ); // link_target_path
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "link_target_path", link_target_path );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        link_target_path = unmarshaller.readString( "link_target_path" );
    }

    private String link_target_path;
}
