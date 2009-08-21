package org.xtreemfs.interfaces;

import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class Lock implements org.xtreemfs.interfaces.utils.Serializable
{
    public static final int TAG = 1054;

    
    public Lock() { client_uuid = ""; client_pid = 0; }
    public Lock( String client_uuid, int client_pid ) { this.client_uuid = client_uuid; this.client_pid = client_pid; }
    public Lock( Object from_hash_map ) { client_uuid = ""; client_pid = 0; this.deserialize( from_hash_map ); }
    public Lock( Object[] from_array ) { client_uuid = ""; client_pid = 0;this.deserialize( from_array ); }

    public String getClient_uuid() { return client_uuid; }
    public void setClient_uuid( String client_uuid ) { this.client_uuid = client_uuid; }
    public int getClient_pid() { return client_pid; }
    public void setClient_pid( int client_pid ) { this.client_pid = client_pid; }

    // Object
    public String toString()
    {
        return "Lock( " + "\"" + client_uuid + "\"" + ", " + Integer.toString( client_pid ) + " )";
    }

    // Serializable
    public int getTag() { return 1054; }
    public String getTypeName() { return "org::xtreemfs::interfaces::Lock"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.client_uuid = ( String )from_hash_map.get( "client_uuid" );
        this.client_pid = ( ( Integer )from_hash_map.get( "client_pid" ) ).intValue();
    }
    
    public void deserialize( Object[] from_array )
    {
        this.client_uuid = ( String )from_array[0];
        this.client_pid = ( ( Integer )from_array[1] ).intValue();        
    }

    public void deserialize( ReusableBuffer buf )
    {
        client_uuid = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        client_pid = buf.getInt();
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "client_uuid", client_uuid );
        to_hash_map.put( "client_pid", new Integer( client_pid ) );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( client_uuid, writer );
        writer.putInt( client_pid );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(client_uuid);
        my_size += ( Integer.SIZE / 8 );
        return my_size;
    }


    private String client_uuid;
    private int client_pid;    

}

