package org.xtreemfs.interfaces;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class XCap extends Struct
{
    public static final int TAG = 2009090231;
    
    public XCap() {  }
    public XCap( String file_id, int access_mode, long expires_s, String client_identity, int truncate_epoch, boolean replicateOnClose, String server_signature ) { this.file_id = file_id; this.access_mode = access_mode; this.expires_s = expires_s; this.client_identity = client_identity; this.truncate_epoch = truncate_epoch; this.replicateOnClose = replicateOnClose; this.server_signature = server_signature; }

    public String getFile_id() { return file_id; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }
    public int getAccess_mode() { return access_mode; }
    public void setAccess_mode( int access_mode ) { this.access_mode = access_mode; }
    public long getExpires_s() { return expires_s; }
    public void setExpires_s( long expires_s ) { this.expires_s = expires_s; }
    public String getClient_identity() { return client_identity; }
    public void setClient_identity( String client_identity ) { this.client_identity = client_identity; }
    public int getTruncate_epoch() { return truncate_epoch; }
    public void setTruncate_epoch( int truncate_epoch ) { this.truncate_epoch = truncate_epoch; }
    public boolean getReplicateOnClose() { return replicateOnClose; }
    public void setReplicateOnClose( boolean replicateOnClose ) { this.replicateOnClose = replicateOnClose; }
    public String getServer_signature() { return server_signature; }
    public void setServer_signature( String server_signature ) { this.server_signature = server_signature; }

    // java.io.Serializable
    public static final long serialVersionUID = 2009090231;    

    // yidl.Object
    public int getTag() { return 2009090231; }
    public String getTypeName() { return "org::xtreemfs::interfaces::XCap"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8 + ( file_id != null ? ( ( file_id.getBytes().length % 4 == 0 ) ? file_id.getBytes().length : ( file_id.getBytes().length + 4 - file_id.getBytes().length % 4 ) ) : 0 ); // file_id
        my_size += Integer.SIZE / 8; // access_mode
        my_size += Long.SIZE / 8; // expires_s
        my_size += Integer.SIZE / 8 + ( client_identity != null ? ( ( client_identity.getBytes().length % 4 == 0 ) ? client_identity.getBytes().length : ( client_identity.getBytes().length + 4 - client_identity.getBytes().length % 4 ) ) : 0 ); // client_identity
        my_size += Integer.SIZE / 8; // truncate_epoch
        my_size += Integer.SIZE / 8; // replicateOnClose
        my_size += Integer.SIZE / 8 + ( server_signature != null ? ( ( server_signature.getBytes().length % 4 == 0 ) ? server_signature.getBytes().length : ( server_signature.getBytes().length + 4 - server_signature.getBytes().length % 4 ) ) : 0 ); // server_signature
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "file_id", file_id );
        marshaller.writeUint32( "access_mode", access_mode );
        marshaller.writeUint64( "expires_s", expires_s );
        marshaller.writeString( "client_identity", client_identity );
        marshaller.writeUint32( "truncate_epoch", truncate_epoch );
        marshaller.writeBoolean( "replicateOnClose", replicateOnClose );
        marshaller.writeString( "server_signature", server_signature );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        file_id = unmarshaller.readString( "file_id" );
        access_mode = unmarshaller.readUint32( "access_mode" );
        expires_s = unmarshaller.readUint64( "expires_s" );
        client_identity = unmarshaller.readString( "client_identity" );
        truncate_epoch = unmarshaller.readUint32( "truncate_epoch" );
        replicateOnClose = unmarshaller.readBoolean( "replicateOnClose" );
        server_signature = unmarshaller.readString( "server_signature" );    
    }
        
    

    private String file_id;
    private int access_mode;
    private long expires_s;
    private String client_identity;
    private int truncate_epoch;
    private boolean replicateOnClose;
    private String server_signature;    

}

