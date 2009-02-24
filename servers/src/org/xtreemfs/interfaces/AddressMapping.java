package org.xtreemfs.interfaces;

import org.xtreemfs.interfaces.*;

import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         
   
public class AddressMapping implements org.xtreemfs.interfaces.utils.Serializable
{
    public AddressMapping() { uuid = ""; version = 0; protocol = ""; address = ""; port = 0; match_network = ""; ttl = 0; }
    public AddressMapping( String uuid, long version, String protocol, String address, int port, String match_network, int ttl ) { this.uuid = uuid; this.version = version; this.protocol = protocol; this.address = address; this.port = port; this.match_network = match_network; this.ttl = ttl; }

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
    public int getTtl() { return ttl; }
    public void setTtl( int ttl ) { this.ttl = ttl; }

    // Object
    public String toString()
    {
        return "AddressMapping( " + "\"" + uuid + "\"" + ", " + Long.toString( version ) + ", " + "\"" + protocol + "\"" + ", " + "\"" + address + "\"" + ", " + Integer.toString( port ) + ", " + "\"" + match_network + "\"" + ", " + Integer.toString( ttl ) + " )";
    }    

    // Serializable
    public void serialize(ONCRPCBufferWriter writer) {
        { final byte[] bytes = uuid.getBytes(); writer.putInt( bytes.length ); writer.put( bytes );  if (bytes.length % 4 > 0) {for (int k = 0; k < (4 - (bytes.length % 4)); k++) { writer.put((byte)0); } }}
        writer.putLong( version );
        { final byte[] bytes = protocol.getBytes(); writer.putInt( bytes.length ); writer.put( bytes );  if (bytes.length % 4 > 0) {for (int k = 0; k < (4 - (bytes.length % 4)); k++) { writer.put((byte)0); } }}
        { final byte[] bytes = address.getBytes(); writer.putInt( bytes.length ); writer.put( bytes );  if (bytes.length % 4 > 0) {for (int k = 0; k < (4 - (bytes.length % 4)); k++) { writer.put((byte)0); } }}
        writer.putInt( port );
        { final byte[] bytes = match_network.getBytes(); writer.putInt( bytes.length ); writer.put( bytes );  if (bytes.length % 4 > 0) {for (int k = 0; k < (4 - (bytes.length % 4)); k++) { writer.put((byte)0); } }}
        writer.putInt( ttl );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        { int uuid_new_length = buf.getInt(); byte[] uuid_new_bytes = new byte[uuid_new_length]; buf.get( uuid_new_bytes ); uuid = new String( uuid_new_bytes ); if (uuid_new_length % 4 > 0) {for (int k = 0; k < (4 - (uuid_new_length % 4)); k++) { buf.get(); } } }
        version = buf.getLong();
        { int protocol_new_length = buf.getInt(); byte[] protocol_new_bytes = new byte[protocol_new_length]; buf.get( protocol_new_bytes ); protocol = new String( protocol_new_bytes ); if (protocol_new_length % 4 > 0) {for (int k = 0; k < (4 - (protocol_new_length % 4)); k++) { buf.get(); } } }
        { int address_new_length = buf.getInt(); byte[] address_new_bytes = new byte[address_new_length]; buf.get( address_new_bytes ); address = new String( address_new_bytes ); if (address_new_length % 4 > 0) {for (int k = 0; k < (4 - (address_new_length % 4)); k++) { buf.get(); } } }
        port = buf.getInt();
        { int match_network_new_length = buf.getInt(); byte[] match_network_new_bytes = new byte[match_network_new_length]; buf.get( match_network_new_bytes ); match_network = new String( match_network_new_bytes ); if (match_network_new_length % 4 > 0) {for (int k = 0; k < (4 - (match_network_new_length % 4)); k++) { buf.get(); } } }
        ttl = buf.getInt();    
    }
    
    public int getSize()
    {
        int my_size = 0;
        my_size += 4 + ( uuid.length() + 4 - ( uuid.length() % 4 ) );
        my_size += ( Long.SIZE / 8 );
        my_size += 4 + ( protocol.length() + 4 - ( protocol.length() % 4 ) );
        my_size += 4 + ( address.length() + 4 - ( address.length() % 4 ) );
        my_size += ( Integer.SIZE / 8 );
        my_size += 4 + ( match_network.length() + 4 - ( match_network.length() % 4 ) );
        my_size += ( Integer.SIZE / 8 );
        return my_size;
    }

    private String uuid;
    private long version;
    private String protocol;
    private String address;
    private int port;
    private String match_network;
    private int ttl;

}

