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
    public String getTypeName() { return "xtreemfs::interfaces::AddressMapping"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(uuid,writer); }
        writer.putLong( version );
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(protocol,writer); }
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(address,writer); }
        writer.putInt( port );
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(match_network,writer); }
        writer.putInt( ttl );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        { uuid = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        version = buf.getLong();
        { protocol = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        { address = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        port = buf.getInt();
        { match_network = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        ttl = buf.getInt();    
    }
    
    public int calculateSize()
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

