package org.xtreemfs.interfaces;

import org.xtreemfs.interfaces.*;

import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         
   
public class AddressMapping implements Serializable
{
    public AddressMapping() { uuid = ""; version = 0; protocol = ""; address = ""; port = 0; match_network = ""; ttl = 0; }
    public AddressMapping( String uuid, long version, String protocol, String address, long port, String match_network, long ttl ) { this.uuid = uuid; this.version = version; this.protocol = protocol; this.address = address; this.port = port; this.match_network = match_network; this.ttl = ttl; }


    // Object
    public String toString()
    {
        return "AddressMapping( " + "\"" + uuid + "\"" + ", " + Long.toString( version ) + ", " + "\"" + protocol + "\"" + ", " + "\"" + address + "\"" + ", " + Long.toString( port ) + ", " + "\"" + match_network + "\"" + ", " + Long.toString( ttl ) + " )";
    }    

    // Serializable
    public void serialize(ONCRPCBufferWriter writer) {
        { final byte[] bytes = uuid.getBytes(); writer.putInt( bytes.length ); writer.put( bytes );  if (bytes.length % 4 > 0) {for (int k = 0; k < (4 - (bytes.length % 4)); k++) { writer.put((byte)0); } }}
        writer.putLong( version );
        { final byte[] bytes = protocol.getBytes(); writer.putInt( bytes.length ); writer.put( bytes );  if (bytes.length % 4 > 0) {for (int k = 0; k < (4 - (bytes.length % 4)); k++) { writer.put((byte)0); } }}
        { final byte[] bytes = address.getBytes(); writer.putInt( bytes.length ); writer.put( bytes );  if (bytes.length % 4 > 0) {for (int k = 0; k < (4 - (bytes.length % 4)); k++) { writer.put((byte)0); } }}
        writer.putLong( port );
        { final byte[] bytes = match_network.getBytes(); writer.putInt( bytes.length ); writer.put( bytes );  if (bytes.length % 4 > 0) {for (int k = 0; k < (4 - (bytes.length % 4)); k++) { writer.put((byte)0); } }}
        writer.putLong( ttl );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        { int uuid_new_length = buf.getInt(); byte[] uuid_new_bytes = new byte[uuid_new_length]; buf.get( uuid_new_bytes ); uuid = new String( uuid_new_bytes ); if (uuid_new_length % 4 > 0) {for (int k = 0; k < (4 - (uuid_new_length % 4)); k++) { buf.get(); } } }
        version = buf.getLong();
        { int protocol_new_length = buf.getInt(); byte[] protocol_new_bytes = new byte[protocol_new_length]; buf.get( protocol_new_bytes ); protocol = new String( protocol_new_bytes ); if (protocol_new_length % 4 > 0) {for (int k = 0; k < (4 - (protocol_new_length % 4)); k++) { buf.get(); } } }
        { int address_new_length = buf.getInt(); byte[] address_new_bytes = new byte[address_new_length]; buf.get( address_new_bytes ); address = new String( address_new_bytes ); if (address_new_length % 4 > 0) {for (int k = 0; k < (4 - (address_new_length % 4)); k++) { buf.get(); } } }
        port = buf.getLong();
        { int match_network_new_length = buf.getInt(); byte[] match_network_new_bytes = new byte[match_network_new_length]; buf.get( match_network_new_bytes ); match_network = new String( match_network_new_bytes ); if (match_network_new_length % 4 > 0) {for (int k = 0; k < (4 - (match_network_new_length % 4)); k++) { buf.get(); } } }
        ttl = buf.getLong();    
    }
    
    public int getSize()
    {
        int my_size = 0;
        my_size += 4 + ( uuid.length() + 4 - ( uuid.length() % 4 ) );
        my_size += ( Long.SIZE / 8 );
        my_size += 4 + ( protocol.length() + 4 - ( protocol.length() % 4 ) );
        my_size += 4 + ( address.length() + 4 - ( address.length() % 4 ) );
        my_size += ( Long.SIZE / 8 );
        my_size += 4 + ( match_network.length() + 4 - ( match_network.length() % 4 ) );
        my_size += ( Long.SIZE / 8 );
        return my_size;
    }

    public String uuid;
    public long version;
    public String protocol;
    public String address;
    public long port;
    public String match_network;
    public long ttl;

}

