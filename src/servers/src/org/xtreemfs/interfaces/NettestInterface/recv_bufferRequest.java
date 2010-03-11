package org.xtreemfs.interfaces.NettestInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class recv_bufferRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2010031319;

    public recv_bufferRequest() {  }
    public recv_bufferRequest( int size ) { this.size = size; }

    public int getSize() { return size; }
    public void setSize( int size ) { this.size = size; }

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
    public Response createDefaultResponse() { return new recv_bufferResponse(); }

    // java.io.Serializable
    public static final long serialVersionUID = 2010031319;

    // yidl.runtime.Object
    public int getTag() { return 2010031319; }
    public String getTypeName() { return "org::xtreemfs::interfaces::NettestInterface::recv_bufferRequest"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8; // size
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeUint32( "size", size );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        size = unmarshaller.readUint32( "size" );
    }

    private int size;
}
