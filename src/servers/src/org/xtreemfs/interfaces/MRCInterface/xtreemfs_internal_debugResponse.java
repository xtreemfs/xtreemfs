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




public class xtreemfs_internal_debugResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2010022449;

    public xtreemfs_internal_debugResponse() {  }
    public xtreemfs_internal_debugResponse( String result ) { this.result = result; }

    public String getResult() { return result; }
    public void setResult( String result ) { this.result = result; }

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
    public static final long serialVersionUID = 2010022449;

    // yidl.runtime.Object
    public int getTag() { return 2010022449; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_internal_debugResponse"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8 + ( result != null ? ( ( result.getBytes().length % 4 == 0 ) ? result.getBytes().length : ( result.getBytes().length + 4 - result.getBytes().length % 4 ) ) : 0 ); // result
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "result", result );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        result = unmarshaller.readString( "result" );
    }

    

    private String result;

}

