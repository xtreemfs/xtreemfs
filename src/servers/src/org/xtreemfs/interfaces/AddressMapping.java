package org.xtreemfs.interfaces;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class AddressMapping implements Struct
{
    public static final int TAG = 2010022245;

    public AddressMapping() {  }
    public AddressMapping( String uuid, long version, String protocol, String address, int port, String match_network, int ttl_s, String uri ) { this.uuid = uuid; this.version = version; this.protocol = protocol; this.address = address; this.port = port; this.match_network = match_network; this.ttl_s = ttl_s; this.uri = uri; }

    public String getUuid() { return uuid; }
    public void setUuid( String uuid ) { this.uuid = uuid; }
    public long getVersion() { return version; }
    public void setVersion( long version ) { this.version = version; }
    public String getProtocol() { return protocol; }
    public void setProtocol( String protocol ) { this.protocol = protocol; }
    public String getAddress() { return address; }
    public void setAddress( String address ) { this.address = address; }
    public int getPort() { return port; }
    public void setPort( int port ) { this.port = port; }
    public String getMatch_network() { return match_network; }
    public void setMatch_network( String match_network ) { this.match_network = match_network; }
    public int getTtl_s() { return ttl_s; }
    public void setTtl_s( int ttl_s ) { this.ttl_s = ttl_s; }
    public String getUri() { return uri; }
    public void setUri( String uri ) { this.uri = uri; }

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
    public static final long serialVersionUID = 2010022245;

    // yidl.runtime.Object
    public int getTag() { return 2010022245; }
    public String getTypeName() { return "org::xtreemfs::interfaces::AddressMapping"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8 + ( uuid != null ? ( ( uuid.getBytes().length % 4 == 0 ) ? uuid.getBytes().length : ( uuid.getBytes().length + 4 - uuid.getBytes().length % 4 ) ) : 0 ); // uuid
        my_size += Long.SIZE / 8; // version
        my_size += Integer.SIZE / 8 + ( protocol != null ? ( ( protocol.getBytes().length % 4 == 0 ) ? protocol.getBytes().length : ( protocol.getBytes().length + 4 - protocol.getBytes().length % 4 ) ) : 0 ); // protocol
        my_size += Integer.SIZE / 8 + ( address != null ? ( ( address.getBytes().length % 4 == 0 ) ? address.getBytes().length : ( address.getBytes().length + 4 - address.getBytes().length % 4 ) ) : 0 ); // address
        my_size += Integer.SIZE / 8; // port
        my_size += Integer.SIZE / 8 + ( match_network != null ? ( ( match_network.getBytes().length % 4 == 0 ) ? match_network.getBytes().length : ( match_network.getBytes().length + 4 - match_network.getBytes().length % 4 ) ) : 0 ); // match_network
        my_size += Integer.SIZE / 8; // ttl_s
        my_size += Integer.SIZE / 8 + ( uri != null ? ( ( uri.getBytes().length % 4 == 0 ) ? uri.getBytes().length : ( uri.getBytes().length + 4 - uri.getBytes().length % 4 ) ) : 0 ); // uri
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "uuid", uuid );
        marshaller.writeUint64( "version", version );
        marshaller.writeString( "protocol", protocol );
        marshaller.writeString( "address", address );
        marshaller.writeUint16( "port", port );
        marshaller.writeString( "match_network", match_network );
        marshaller.writeUint32( "ttl_s", ttl_s );
        marshaller.writeString( "uri", uri );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        uuid = unmarshaller.readString( "uuid" );
        version = unmarshaller.readUint64( "version" );
        protocol = unmarshaller.readString( "protocol" );
        address = unmarshaller.readString( "address" );
        port = unmarshaller.readUint16( "port" );
        match_network = unmarshaller.readString( "match_network" );
        ttl_s = unmarshaller.readUint32( "ttl_s" );
        uri = unmarshaller.readString( "uri" );
    }

    

    private String uuid;
    private long version;
    private String protocol;
    private String address;
    private int port;
    private String match_network;
    private int ttl_s;
    private String uri;

}

