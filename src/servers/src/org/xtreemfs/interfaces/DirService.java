package org.xtreemfs.interfaces;

import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class DirService implements org.xtreemfs.interfaces.utils.Serializable
{
    public static final int TAG = 2009082653;

    
    public DirService() { address = ""; port = 0; protocol = ""; interface_version = 0; }
    public DirService( String address, int port, String protocol, int interface_version ) { this.address = address; this.port = port; this.protocol = protocol; this.interface_version = interface_version; }
    public DirService( Object from_hash_map ) { address = ""; port = 0; protocol = ""; interface_version = 0; this.deserialize( from_hash_map ); }
    public DirService( Object[] from_array ) { address = ""; port = 0; protocol = ""; interface_version = 0;this.deserialize( from_array ); }

    public String getAddress() { return address; }
    public void setAddress( String address ) { this.address = address; }
    public int getPort() { return port; }
    public void setPort( int port ) { this.port = port; }
    public String getProtocol() { return protocol; }
    public void setProtocol( String protocol ) { this.protocol = protocol; }
    public int getInterface_version() { return interface_version; }
    public void setInterface_version( int interface_version ) { this.interface_version = interface_version; }

    // Object
    public String toString()
    {
        return "DirService( " + "\"" + address + "\"" + ", " + Integer.toString( port ) + ", " + "\"" + protocol + "\"" + ", " + Integer.toString( interface_version ) + " )";
    }

    // Serializable
    public int getTag() { return 2009082653; }
    public String getTypeName() { return "org::xtreemfs::interfaces::DirService"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.address = ( String )from_hash_map.get( "address" );
        this.port = ( ( Integer )from_hash_map.get( "port" ) ).intValue();
        this.protocol = ( String )from_hash_map.get( "protocol" );
        this.interface_version = ( ( Integer )from_hash_map.get( "interface_version" ) ).intValue();
    }
    
    public void deserialize( Object[] from_array )
    {
        this.address = ( String )from_array[0];
        this.port = ( ( Integer )from_array[1] ).intValue();
        this.protocol = ( String )from_array[2];
        this.interface_version = ( ( Integer )from_array[3] ).intValue();        
    }

    public void deserialize( ReusableBuffer buf )
    {
        address = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        port = buf.getInt();
        protocol = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        interface_version = buf.getInt();
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "address", address );
        to_hash_map.put( "port", new Integer( port ) );
        to_hash_map.put( "protocol", protocol );
        to_hash_map.put( "interface_version", new Integer( interface_version ) );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( address, writer );
        writer.putInt( port );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( protocol, writer );
        writer.putInt( interface_version );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(address);
        my_size += ( Integer.SIZE / 8 );
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(protocol);
        my_size += ( Integer.SIZE / 8 );
        return my_size;
    }


    private String address;
    private int port;
    private String protocol;
    private int interface_version;    

}

