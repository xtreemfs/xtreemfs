package org.xtreemfs.interfaces.OSDInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.oncrpc.utils.*;
import org.xtreemfs.interfaces.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class xtreemfs_cleanup_is_runningRequest extends org.xtreemfs.foundation.oncrpc.utils.Request
{
    public static final int TAG = 2010031247;

    public xtreemfs_cleanup_is_runningRequest() {  }

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
    public Response createDefaultResponse() { return new xtreemfs_cleanup_is_runningResponse(); }

    // java.io.Serializable
    public static final long serialVersionUID = 2010031247;

    // yidl.runtime.Object
    public int getTag() { return 2010031247; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_cleanup_is_runningRequest"; }

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
