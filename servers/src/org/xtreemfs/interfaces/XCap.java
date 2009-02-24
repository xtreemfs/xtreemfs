package org.xtreemfs.interfaces;

import org.xtreemfs.interfaces.*;

import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         
   
public class XCap implements Serializable
{
    public XCap() { file_id = ""; access_mode = 0; expires = 0; clientIdentity = ""; truncateEpoch = 0; serverSignature = ""; __json = ""; }
    public XCap( String file_id, int access_mode, long expires, String clientIdentity, long truncateEpoch, String serverSignature, String __json ) { this.file_id = file_id; this.access_mode = access_mode; this.expires = expires; this.clientIdentity = clientIdentity; this.truncateEpoch = truncateEpoch; this.serverSignature = serverSignature; this.__json = __json; }

    public String getFile_id() { return file_id; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }
    public int getAccess_mode() { return access_mode; }
    public void setAccess_mode( int access_mode ) { this.access_mode = access_mode; }
    public long getExpires() { return expires; }
    public void setExpires( long expires ) { this.expires = expires; }
    public String getClientidentity() { return clientIdentity; }
    public void setClientidentity( String clientIdentity ) { this.clientIdentity = clientIdentity; }
    public long getTruncateepoch() { return truncateEpoch; }
    public void setTruncateepoch( long truncateEpoch ) { this.truncateEpoch = truncateEpoch; }
    public String getServersignature() { return serverSignature; }
    public void setServersignature( String serverSignature ) { this.serverSignature = serverSignature; }
    public String get__json() { return __json; }
    public void set__json( String __json ) { this.__json = __json; }

    // Object
    public String toString()
    {
        return "XCap( " + "\"" + file_id + "\"" + ", " + Integer.toString( access_mode ) + ", " + Long.toString( expires ) + ", " + "\"" + clientIdentity + "\"" + ", " + Long.toString( truncateEpoch ) + ", " + "\"" + serverSignature + "\"" + ", " + "\"" + __json + "\"" + " )";
    }    

    // Serializable
    public void serialize(ONCRPCBufferWriter writer) {
        { final byte[] bytes = file_id.getBytes(); writer.putInt( bytes.length ); writer.put( bytes );  if (bytes.length % 4 > 0) {for (int k = 0; k < (4 - (bytes.length % 4)); k++) { writer.put((byte)0); } }}
        writer.putInt( access_mode );
        writer.putLong( expires );
        { final byte[] bytes = clientIdentity.getBytes(); writer.putInt( bytes.length ); writer.put( bytes );  if (bytes.length % 4 > 0) {for (int k = 0; k < (4 - (bytes.length % 4)); k++) { writer.put((byte)0); } }}
        writer.putLong( truncateEpoch );
        { final byte[] bytes = serverSignature.getBytes(); writer.putInt( bytes.length ); writer.put( bytes );  if (bytes.length % 4 > 0) {for (int k = 0; k < (4 - (bytes.length % 4)); k++) { writer.put((byte)0); } }}
        { final byte[] bytes = __json.getBytes(); writer.putInt( bytes.length ); writer.put( bytes );  if (bytes.length % 4 > 0) {for (int k = 0; k < (4 - (bytes.length % 4)); k++) { writer.put((byte)0); } }}        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        { int file_id_new_length = buf.getInt(); byte[] file_id_new_bytes = new byte[file_id_new_length]; buf.get( file_id_new_bytes ); file_id = new String( file_id_new_bytes ); if (file_id_new_length % 4 > 0) {for (int k = 0; k < (4 - (file_id_new_length % 4)); k++) { buf.get(); } } }
        access_mode = buf.getInt();
        expires = buf.getLong();
        { int clientIdentity_new_length = buf.getInt(); byte[] clientIdentity_new_bytes = new byte[clientIdentity_new_length]; buf.get( clientIdentity_new_bytes ); clientIdentity = new String( clientIdentity_new_bytes ); if (clientIdentity_new_length % 4 > 0) {for (int k = 0; k < (4 - (clientIdentity_new_length % 4)); k++) { buf.get(); } } }
        truncateEpoch = buf.getLong();
        { int serverSignature_new_length = buf.getInt(); byte[] serverSignature_new_bytes = new byte[serverSignature_new_length]; buf.get( serverSignature_new_bytes ); serverSignature = new String( serverSignature_new_bytes ); if (serverSignature_new_length % 4 > 0) {for (int k = 0; k < (4 - (serverSignature_new_length % 4)); k++) { buf.get(); } } }
        { int __json_new_length = buf.getInt(); byte[] __json_new_bytes = new byte[__json_new_length]; buf.get( __json_new_bytes ); __json = new String( __json_new_bytes ); if (__json_new_length % 4 > 0) {for (int k = 0; k < (4 - (__json_new_length % 4)); k++) { buf.get(); } } }    
    }
    
    public int getSize()
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

