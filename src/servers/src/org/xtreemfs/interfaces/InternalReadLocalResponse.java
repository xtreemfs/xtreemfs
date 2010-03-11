package org.xtreemfs.interfaces;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class InternalReadLocalResponse implements Struct
{
    public static final int TAG = 2010030968;

    public InternalReadLocalResponse() { data = new ObjectData(); object_set = new ObjectListSet();  }
    public InternalReadLocalResponse( ObjectData data, ObjectListSet object_set ) { this.data = data; this.object_set = object_set; }

    public ObjectData getData() { return data; }
    public ObjectListSet getObject_set() { return object_set; }
    public void setData( ObjectData data ) { this.data = data; }
    public void setObject_set( ObjectListSet object_set ) { this.object_set = object_set; }

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
    public static final long serialVersionUID = 2010030968;

    // yidl.runtime.Object
    public int getTag() { return 2010030968; }
    public String getTypeName() { return "org::xtreemfs::interfaces::InternalReadLocalResponse"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += data.getXDRSize(); // data
        my_size += object_set.getXDRSize(); // object_set
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeStruct( "data", data );
        marshaller.writeSequence( "object_set", object_set );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        data = new ObjectData(); unmarshaller.readStruct( "data", data );
        object_set = new ObjectListSet(); unmarshaller.readSequence( "object_set", object_set );
    }

    private ObjectData data;
    private ObjectListSet object_set;
}
