package org.xtreemfs.interfaces;

import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         
   
public class XCap implements org.xtreemfs.interfaces.utils.Serializable
{
    public XCap() { file_id = ""; access_mode = 0; expires = 0; client_identity = ""; truncate_epoch = 0; server_signature = ""; }
    public XCap( String file_id, int access_mode, long expires, String client_identity, int truncate_epoch, String server_signature ) { this.file_id = file_id; this.access_mode = access_mode; this.expires = expires; this.client_identity = client_identity; this.truncate_epoch = truncate_epoch; this.server_signature = server_signature; }

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

    // Object
    public String toString()
    {
        return "XCap( " + "\"" + file_id + "\"" + ", " + Integer.toString( access_mode ) + ", " + Long.toString( expires ) + ", " + "\"" + client_identity + "\"" + ", " + Integer.toString( truncate_epoch ) + ", " + "\"" + server_signature + "\"" + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::XCap"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(file_id,writer); }
        writer.putInt( access_mode );
        writer.putLong( expires );
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(client_identity,writer); }
        writer.putInt( truncate_epoch );
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(server_signature,writer); }        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        { file_id = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        access_mode = buf.getInt();
        expires = buf.getLong();
        { client_identity = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        truncate_epoch = buf.getInt();
        { server_signature = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += 4 + ( file_id.length() + 4 - ( file_id.length() % 4 ) );
        my_size += ( Integer.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        my_size += 4 + ( client_identity.length() + 4 - ( client_identity.length() % 4 ) );
        my_size += ( Integer.SIZE / 8 );
        my_size += 4 + ( server_signature.length() + 4 - ( server_signature.length() % 4 ) );
        return my_size;
    }

    private String file_id;
    private int access_mode;
    private long expires;
    private String client_identity;
    private int truncate_epoch;
    private String server_signature;

}

