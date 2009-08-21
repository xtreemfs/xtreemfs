package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_lock_acquireRequest implements org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 1350;

    
    public xtreemfs_lock_acquireRequest() { file_credentials = new FileCredentials(); client_uuid = ""; client_pid = 0; file_id = ""; offset = 0; length = 0; exclusive = false; }
    public xtreemfs_lock_acquireRequest( FileCredentials file_credentials, String client_uuid, int client_pid, String file_id, long offset, long length, boolean exclusive ) { this.file_credentials = file_credentials; this.client_uuid = client_uuid; this.client_pid = client_pid; this.file_id = file_id; this.offset = offset; this.length = length; this.exclusive = exclusive; }
    public xtreemfs_lock_acquireRequest( Object from_hash_map ) { file_credentials = new FileCredentials(); client_uuid = ""; client_pid = 0; file_id = ""; offset = 0; length = 0; exclusive = false; this.deserialize( from_hash_map ); }
    public xtreemfs_lock_acquireRequest( Object[] from_array ) { file_credentials = new FileCredentials(); client_uuid = ""; client_pid = 0; file_id = ""; offset = 0; length = 0; exclusive = false;this.deserialize( from_array ); }

    public FileCredentials getFile_credentials() { return file_credentials; }
    public void setFile_credentials( FileCredentials file_credentials ) { this.file_credentials = file_credentials; }
    public String getClient_uuid() { return client_uuid; }
    public void setClient_uuid( String client_uuid ) { this.client_uuid = client_uuid; }
    public int getClient_pid() { return client_pid; }
    public void setClient_pid( int client_pid ) { this.client_pid = client_pid; }
    public String getFile_id() { return file_id; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }
    public long getOffset() { return offset; }
    public void setOffset( long offset ) { this.offset = offset; }
    public long getLength() { return length; }
    public void setLength( long length ) { this.length = length; }
    public boolean getExclusive() { return exclusive; }
    public void setExclusive( boolean exclusive ) { this.exclusive = exclusive; }

    // Object
    public String toString()
    {
        return "xtreemfs_lock_acquireRequest( " + file_credentials.toString() + ", " + "\"" + client_uuid + "\"" + ", " + Integer.toString( client_pid ) + ", " + "\"" + file_id + "\"" + ", " + Long.toString( offset ) + ", " + Long.toString( length ) + ", " + Boolean.toString( exclusive ) + " )";
    }

    // Serializable
    public int getTag() { return 1350; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_lock_acquireRequest"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.file_credentials.deserialize( from_hash_map.get( "file_credentials" ) );
        this.client_uuid = ( String )from_hash_map.get( "client_uuid" );
        this.client_pid = ( ( Integer )from_hash_map.get( "client_pid" ) ).intValue();
        this.file_id = ( String )from_hash_map.get( "file_id" );
        this.offset = ( ( Long )from_hash_map.get( "offset" ) ).longValue();
        this.length = ( ( Long )from_hash_map.get( "length" ) ).longValue();
        this.exclusive = ( ( Boolean )from_hash_map.get( "exclusive" ) ).booleanValue();
    }
    
    public void deserialize( Object[] from_array )
    {
        this.file_credentials.deserialize( from_array[0] );
        this.client_uuid = ( String )from_array[1];
        this.client_pid = ( ( Integer )from_array[2] ).intValue();
        this.file_id = ( String )from_array[3];
        this.offset = ( ( Long )from_array[4] ).longValue();
        this.length = ( ( Long )from_array[5] ).longValue();
        this.exclusive = ( ( Boolean )from_array[6] ).booleanValue();        
    }

    public void deserialize( ReusableBuffer buf )
    {
        file_credentials = new FileCredentials(); file_credentials.deserialize( buf );
        client_uuid = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        client_pid = buf.getInt();
        file_id = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        offset = buf.getLong();
        length = buf.getLong();
        exclusive = buf.getInt() != 0;
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "file_credentials", file_credentials.serialize() );
        to_hash_map.put( "client_uuid", client_uuid );
        to_hash_map.put( "client_pid", new Integer( client_pid ) );
        to_hash_map.put( "file_id", file_id );
        to_hash_map.put( "offset", new Long( offset ) );
        to_hash_map.put( "length", new Long( length ) );
        to_hash_map.put( "exclusive", new Boolean( exclusive ) );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        file_credentials.serialize( writer );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( client_uuid, writer );
        writer.putInt( client_pid );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( file_id, writer );
        writer.putLong( offset );
        writer.putLong( length );
        writer.putInt( exclusive ? 1 : 0 );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += file_credentials.calculateSize();
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(client_uuid);
        my_size += ( Integer.SIZE / 8 );
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(file_id);
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        my_size += 4;
        return my_size;
    }

    // Request
    public Response createDefaultResponse() { return new xtreemfs_lock_acquireResponse(); }


    private FileCredentials file_credentials;
    private String client_uuid;
    private int client_pid;
    private String file_id;
    private long offset;
    private long length;
    private boolean exclusive;    

}

