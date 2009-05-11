package org.xtreemfs.interfaces;

import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class AddressMapping implements org.xtreemfs.interfaces.utils.Serializable
{
    public AddressMapping() { uuid = ""; version = 0; protocol = ""; address = ""; port = 0; match_network = ""; ttl_s = 0; uri = ""; }
    public AddressMapping( String uuid, long version, String protocol, String address, int port, String match_network, int ttl_s, String uri ) { this.uuid = uuid; this.version = version; this.protocol = protocol; this.address = address; this.port = port; this.match_network = match_network; this.ttl_s = ttl_s; this.uri = uri; }
    public AddressMapping( Object from_hash_map ) { uuid = ""; version = 0; protocol = ""; address = ""; port = 0; match_network = ""; ttl_s = 0; uri = ""; this.deserialize( from_hash_map ); }
    public AddressMapping( Object[] from_array ) { uuid = ""; version = 0; protocol = ""; address = ""; port = 0; match_network = ""; ttl_s = 0; uri = "";this.deserialize( from_array ); }

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

    public String getTypeName() { return "org::xtreemfs::interfaces::AddressMapping"; }    
    public long getTypeId() { return 0; }

    public String toString()
    {
        return "AddressMapping( " + "\"" + uuid + "\"" + ", " + Long.toString( version ) + ", " + "\"" + protocol + "\"" + ", " + "\"" + address + "\"" + ", " + Integer.toString( port ) + ", " + "\"" + match_network + "\"" + ", " + Integer.toString( ttl_s ) + ", " + "\"" + uri + "\"" + " )";
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.uuid = ( String )from_hash_map.get( "uuid" );
        this.version = ( ( Long )from_hash_map.get( "version" ) ).longValue();
        this.protocol = ( String )from_hash_map.get( "protocol" );
        this.address = ( String )from_hash_map.get( "address" );
        this.port = ( ( Integer )from_hash_map.get( "port" ) ).intValue();
        this.match_network = ( String )from_hash_map.get( "match_network" );
        this.ttl_s = ( ( Integer )from_hash_map.get( "ttl_s" ) ).intValue();
        this.uri = ( String )from_hash_map.get( "uri" );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.uuid = ( String )from_array[0];
        this.version = ( ( Long )from_array[1] ).longValue();
        this.protocol = ( String )from_array[2];
        this.address = ( String )from_array[3];
        this.port = ( ( Integer )from_array[4] ).intValue();
        this.match_network = ( String )from_array[5];
        this.ttl_s = ( ( Integer )from_array[6] ).intValue();
        this.uri = ( String )from_array[7];        
    }

    public void deserialize( ReusableBuffer buf )
    {
        uuid = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        version = buf.getLong();
        protocol = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        address = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        port = buf.getInt();
        match_network = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        ttl_s = buf.getInt();
        uri = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "uuid", uuid );
        to_hash_map.put( "version", new Long( version ) );
        to_hash_map.put( "protocol", protocol );
        to_hash_map.put( "address", address );
        to_hash_map.put( "port", new Integer( port ) );
        to_hash_map.put( "match_network", match_network );
        to_hash_map.put( "ttl_s", new Integer( ttl_s ) );
        to_hash_map.put( "uri", uri );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( uuid, writer );
        writer.putLong( version );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( protocol, writer );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( address, writer );
        writer.putInt( port );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( match_network, writer );
        writer.putInt( ttl_s );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( uri, writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(uuid);
        my_size += ( Long.SIZE / 8 );
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(protocol);
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(address);
        my_size += ( Integer.SIZE / 8 );
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(match_network);
        my_size += ( Integer.SIZE / 8 );
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(uri);
        return my_size;
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

