package org.xtreemfs.interfaces.MRCInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class getxattrResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2010031121;

    public getxattrResponse() {  }
    public getxattrResponse( String value ) { this.value = value; }

    public String getValue() { return value; }
    public void setValue( String value ) { this.value = value; }

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
    public static final long serialVersionUID = 2010031121;

    // yidl.runtime.Object
    public int getTag() { return 2010031121; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::getxattrResponse"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8 + ( value != null ? ( ( value.getBytes().length % 4 == 0 ) ? value.getBytes().length : ( value.getBytes().length + 4 - value.getBytes().length % 4 ) ) : 0 ); // value
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "value", value );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        value = unmarshaller.readString( "value" );
    }

    private String value;
}
