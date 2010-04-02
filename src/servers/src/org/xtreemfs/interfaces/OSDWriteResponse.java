package org.xtreemfs.interfaces;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.oncrpc.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class OSDWriteResponse implements Struct
{
    public static final int TAG = 2010030933;

    public OSDWriteResponse() { new_file_size = new NewFileSizeSet();  }
    public OSDWriteResponse( NewFileSizeSet new_file_size ) { this.new_file_size = new_file_size; }

    public NewFileSizeSet getNew_file_size() { return new_file_size; }
    public void setNew_file_size( NewFileSizeSet new_file_size ) { this.new_file_size = new_file_size; }

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
    public static final long serialVersionUID = 2010030933;

    // yidl.runtime.Object
    public int getTag() { return 2010030933; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDWriteResponse"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += new_file_size.getXDRSize(); // new_file_size
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeSequence( "new_file_size", new_file_size );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        new_file_size = new NewFileSizeSet(); unmarshaller.readSequence( "new_file_size", new_file_size );
    }

    private NewFileSizeSet new_file_size;
}
