package org.xtreemfs.interfaces;

import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class XCap implements org.xtreemfs.interfaces.utils.Serializable
{
    public XCap() { file_id = ""; access_mode = 0; expires = 0; client_identity = ""; truncate_epoch = 0; server_signature = ""; }
    public XCap( String file_id, int access_mode, long expires, String client_identity, int truncate_epoch, String server_signature ) { this.file_id = file_id; this.access_mode = access_mode; this.expires = expires; this.client_identity = client_identity; this.truncate_epoch = truncate_epoch; this.server_signature = server_signature; }
    public XCap( Object from_hash_map ) { file_id = ""; access_mode = 0; expires = 0; client_identity = ""; truncate_epoch = 0; server_signature = ""; this.deserialize( from_hash_map ); }
    public XCap( Object[] from_array ) { file_id = ""; access_mode = 0; expires = 0; client_identity = ""; truncate_epoch = 0; server_signature = "";this.deserialize( from_array ); }

    public String getFile_id() { return file_id; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }
    public int getAccess_mode() { return access_mode; }
    public void setAccess_mode( int access_mode ) { this.access_mode = access_mode; }
    public long getExpires() { return expires; }
    public void setExpires( long expires ) { this.expires = expires; }
    public String getClient_identity() { return client_identity; }
    public void setClient_identity( String client_identity ) { this.client_identity = client_identity; }
    public int getTruncate_epoch() { return truncate_epoch; }
    public void setTruncate_epoch( int truncate_epoch ) { this.truncate_epoch = truncate_epoch; }
    public String getServer_signature() { return server_signature; }
    public void setServer_signature( String server_signature ) { this.server_signature = server_signature; }

    // Serializable
    public String getTypeName() { return "org::xtreemfs::interfaces::XCap"; }    
    public long getTypeId() { return 0; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.file_id = ( String )from_hash_map.get( "file_id" );
        this.access_mode = ( ( Integer )from_hash_map.get( "access_mode" ) ).intValue();
        this.expires = ( ( Long )from_hash_map.get( "expires" ) ).longValue();
        this.client_identity = ( String )from_hash_map.get( "client_identity" );
        this.truncate_epoch = ( ( Integer )from_hash_map.get( "truncate_epoch" ) ).intValue();
        this.server_signature = ( String )from_hash_map.get( "server_signature" );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.file_id = ( String )from_array[0];
        this.access_mode = ( ( Integer )from_array[1] ).intValue();
        this.expires = ( ( Long )from_array[2] ).longValue();
        this.client_identity = ( String )from_array[3];
        this.truncate_epoch = ( ( Integer )from_array[4] ).intValue();
        this.server_signature = ( String )from_array[5];        
    }

    public void deserialize( ReusableBuffer buf )
    {
        file_id = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        access_mode = buf.getInt();
        expires = buf.getLong();
        client_identity = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        truncate_epoch = buf.getInt();
        server_signature = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "file_id", file_id );
        to_hash_map.put( "access_mode", new Integer( access_mode ) );
        to_hash_map.put( "expires", new Long( expires ) );
        to_hash_map.put( "client_identity", client_identity );
        to_hash_map.put( "truncate_epoch", new Integer( truncate_epoch ) );
        to_hash_map.put( "server_signature", server_signature );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( file_id, writer );
        writer.putInt( access_mode );
        writer.putLong( expires );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( client_identity, writer );
        writer.putInt( truncate_epoch );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( server_signature, writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(file_id);
        my_size += ( Integer.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(client_identity);
        my_size += ( Integer.SIZE / 8 );
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(server_signature);
        return my_size;
    }


    private String file_id;
    private int access_mode;
    private long expires;
    private String client_identity;
    private int truncate_epoch;
    private String server_signature;

}

