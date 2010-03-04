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




public class RedirectException extends org.xtreemfs.interfaces.utils.ONCRPCException
{
    public static final int TAG = 2010030569;

    public RedirectException() {  }
    public RedirectException( String address, int port ) { this.address = address; this.port = port; }

    public String getAddress() { return address; }
    public void setAddress( String address ) { this.address = address; }
    public int getPort() { return port; }
    public void setPort( int port ) { this.port = port; }

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
    public static final long serialVersionUID = 2010030569;

    // yidl.runtime.Object
    public int getTag() { return 2010030569; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::RedirectException"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8 + ( address != null ? ( ( address.getBytes().length % 4 == 0 ) ? address.getBytes().length : ( address.getBytes().length + 4 - address.getBytes().length % 4 ) ) : 0 ); // address
        my_size += Integer.SIZE / 8; // port
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "address", address );
        marshaller.writeUint16( "port", port );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        address = unmarshaller.readString( "address" );
        port = unmarshaller.readUint16( "port" );
    }

    

    private String address;
    private int port;

}

