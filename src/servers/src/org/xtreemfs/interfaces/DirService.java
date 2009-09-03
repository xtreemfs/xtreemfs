package org.xtreemfs.interfaces;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class DirService extends Struct
{
    public static final int TAG = 2009082653;
    
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

    // java.io.Serializable
    public static final long serialVersionUID = 2009082653;    

    // yidl.Object
    public int getTag() { return 2009082653; }
    public String getTypeName() { return "org::xtreemfs::interfaces::DirService"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += ( ( address.getBytes().length + Integer.SIZE/8 ) % 4 == 0 ) ? ( address.getBytes().length + Integer.SIZE/8 ) : ( address.getBytes().length + Integer.SIZE/8 + 4 - ( address.getBytes().length + Integer.SIZE/8 ) % 4 );
        my_size += ( Integer.SIZE / 8 );
        my_size += ( ( protocol.getBytes().length + Integer.SIZE/8 ) % 4 == 0 ) ? ( protocol.getBytes().length + Integer.SIZE/8 ) : ( protocol.getBytes().length + Integer.SIZE/8 + 4 - ( protocol.getBytes().length + Integer.SIZE/8 ) % 4 );
        my_size += ( Integer.SIZE / 8 );
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

