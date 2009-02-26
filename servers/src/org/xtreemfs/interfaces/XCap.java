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
    public XCap() { file_id = ""; access_mode = 0; expires = 0; clientIdentity = ""; truncateEpoch = 0; serverSignature = ""; __json = ""; }
    public XCap( String file_id, int access_mode, long expires, String clientIdentity, long truncateEpoch, String serverSignature, String __json ) { this.file_id = file_id; this.access_mode = access_mode; this.expires = expires; this.clientIdentity = clientIdentity; this.truncateEpoch = truncateEpoch; this.serverSignature = serverSignature; this.__json = __json; }

    public String getFile_id() { return file_id; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }
    public int getAccess_mode() { return access_mode; }
    public void setAccess_mode( int access_mode ) { this.access_mode = access_mode; }
    public long getExpires() { return expires; }
    public void setExpires( long expires ) { this.expires = expires; }
    public String getClientIdentity() { return clientIdentity; }
    public void setClientIdentity( String clientIdentity ) { this.clientIdentity = clientIdentity; }
    public long getTruncateEpoch() { return truncateEpoch; }
    public void setTruncateEpoch( long truncateEpoch ) { this.truncateEpoch = truncateEpoch; }
    public String getServerSignature() { return serverSignature; }
    public void setServerSignature( String serverSignature ) { this.serverSignature = serverSignature; }
    public String get__json() { return __json; }
    public void set__json( String __json ) { this.__json = __json; }

    // Object
    public String toString()
    {
        return "XCap( " + "\"" + file_id + "\"" + ", " + Integer.toString( access_mode ) + ", " + Long.toString( expires ) + ", " + "\"" + clientIdentity + "\"" + ", " + Long.toString( truncateEpoch ) + ", " + "\"" + serverSignature + "\"" + ", " + "\"" + __json + "\"" + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::XCap"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(file_id,writer); }
        writer.putInt( access_mode );
        writer.putLong( expires );
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(clientIdentity,writer); }
        writer.putLong( truncateEpoch );
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(serverSignature,writer); }
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(__json,writer); }        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        { file_id = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        access_mode = buf.getInt();
        expires = buf.getLong();
        { clientIdentity = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        truncateEpoch = buf.getLong();
        { serverSignature = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        { __json = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += 4 + ( file_id.length() + 4 - ( file_id.length() % 4 ) );
        my_size += ( Integer.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        my_size += 4 + ( clientIdentity.length() + 4 - ( clientIdentity.length() % 4 ) );
        my_size += ( Long.SIZE / 8 );
        my_size += 4 + ( serverSignature.length() + 4 - ( serverSignature.length() % 4 ) );
        my_size += 4 + ( __json.length() + 4 - ( __json.length() % 4 ) );
        return my_size;
    }

    private String file_id;
    private int access_mode;
    private long expires;
    private String clientIdentity;
    private long truncateEpoch;
    private String serverSignature;
    private String __json;

}

