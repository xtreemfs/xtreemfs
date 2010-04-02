package org.xtreemfs.interfaces;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.oncrpc.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class ObjectVersion implements Struct
{
    public static final int TAG = 2010030973;

    public ObjectVersion() {  }
    public ObjectVersion( long object_number, long object_version ) { this.object_number = object_number; this.object_version = object_version; }

    public long getObject_number() { return object_number; }
    public long getObject_version() { return object_version; }
    public void setObject_number( long object_number ) { this.object_number = object_number; }
    public void setObject_version( long object_version ) { this.object_version = object_version; }

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
    public static final long serialVersionUID = 2010030973;

    // yidl.runtime.Object
    public int getTag() { return 2010030973; }
    public String getTypeName() { return "org::xtreemfs::interfaces::ObjectVersion"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Long.SIZE / 8; // object_number
        my_size += Long.SIZE / 8; // object_version
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeUint64( "object_number", object_number );
        marshaller.writeUint64( "object_version", object_version );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        object_number = unmarshaller.readUint64( "object_number" );
        object_version = unmarshaller.readUint64( "object_version" );
    }

    private long object_number;
    private long object_version;
}
