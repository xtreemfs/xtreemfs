package org.xtreemfs.interfaces.NettestInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class recv_bufferResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2010031319;

    public recv_bufferResponse() {  }
    public recv_bufferResponse( ReusableBuffer data ) { this.data = data; }

    public ReusableBuffer getData() { return data; }
    public void setData( ReusableBuffer data ) { this.data = data; }

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
    public static final long serialVersionUID = 2010031319;

    // yidl.runtime.Object
    public int getTag() { return 2010031319; }
    public String getTypeName() { return "org::xtreemfs::interfaces::NettestInterface::recv_bufferResponse"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8 + ( data != null ? ( ( data.remaining() % 4 == 0 ) ? data.remaining() : ( data.remaining() + 4 - data.remaining() % 4 ) ) : 0 ); // data
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeBuffer( "data", data );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        data = ( ReusableBuffer )unmarshaller.readBuffer( "data" );
    }

    private ReusableBuffer data;
}
