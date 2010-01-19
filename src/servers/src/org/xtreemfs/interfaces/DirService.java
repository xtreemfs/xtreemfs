package org.xtreemfs.interfaces;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class DirService implements Struct
{
    public static final int TAG = 2010011946;
    
    public DirService() {  }
    public DirService( String address, int port, String protocol, int interface_version ) { this.address = address; this.port = port; this.protocol = protocol; this.interface_version = interface_version; }

    public String getAddress() { return address; }
    public void setAddress( String address ) { this.address = address; }
    public int getPort() { return port; }
    public void setPort( int port ) { this.port = port; }
    public String getProtocol() { return protocol; }
    public void setProtocol( String protocol ) { this.protocol = protocol; }
    public int getInterface_version() { return interface_version; }
    public void setInterface_version( int interface_version ) { this.interface_version = interface_version; }

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
    public static final long serialVersionUID = 2010011946;    

    // yidl.runtime.Object
    public int getTag() { return 2010011946; }
    public String getTypeName() { return "org::xtreemfs::interfaces::DirService"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8 + ( address != null ? ( ( address.getBytes().length % 4 == 0 ) ? address.getBytes().length : ( address.getBytes().length + 4 - address.getBytes().length % 4 ) ) : 0 ); // address
        my_size += Integer.SIZE / 8; // port
        my_size += Integer.SIZE / 8 + ( protocol != null ? ( ( protocol.getBytes().length % 4 == 0 ) ? protocol.getBytes().length : ( protocol.getBytes().length + 4 - protocol.getBytes().length % 4 ) ) : 0 ); // protocol
        my_size += Integer.SIZE / 8; // interface_version
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "address", address );
        marshaller.writeUint16( "port", port );
        marshaller.writeString( "protocol", protocol );
        marshaller.writeUint32( "interface_version", interface_version );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        address = unmarshaller.readString( "address" );
        port = unmarshaller.readUint16( "port" );
        protocol = unmarshaller.readString( "protocol" );
        interface_version = unmarshaller.readUint32( "interface_version" );    
    }
        
    

    private String address;
    private int port;
    private String protocol;
    private int interface_version;    

}

