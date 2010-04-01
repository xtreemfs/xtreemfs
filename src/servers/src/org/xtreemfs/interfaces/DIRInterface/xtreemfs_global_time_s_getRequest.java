package org.xtreemfs.interfaces.DIRInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.oncrpc.utils.*;
import org.xtreemfs.interfaces.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class xtreemfs_global_time_s_getRequest extends org.xtreemfs.foundation.oncrpc.utils.Request
{
    public static final int TAG = 2010031022;

    public xtreemfs_global_time_s_getRequest() {  }

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
    public Response createDefaultResponse() { return new xtreemfs_global_time_s_getResponse(); }

    // java.io.Serializable
    public static final long serialVersionUID = 2010031022;

    // yidl.runtime.Object
    public int getTag() { return 2010031022; }
    public String getTypeName() { return "org::xtreemfs::interfaces::DIRInterface::xtreemfs_global_time_s_getRequest"; }

    public int getXDRSize()
    {
        int my_size = 0;

        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {

    }

    public void unmarshal( Unmarshaller unmarshaller )
    {

    }
}
