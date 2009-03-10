package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class chownRequest implements org.xtreemfs.interfaces.utils.Request
{
    public chownRequest() { context = new Context(); path = ""; userId = ""; groupId = ""; }
    public chownRequest( Context context, String path, String userId, String groupId ) { this.context = context; this.path = path; this.userId = userId; this.groupId = groupId; }
    public chownRequest( Object from_hash_map ) { context = new Context(); path = ""; userId = ""; groupId = ""; this.deserialize( from_hash_map ); }
    public chownRequest( Object[] from_array ) { context = new Context(); path = ""; userId = ""; groupId = "";this.deserialize( from_array ); }

    public Context getContext() { return context; }
    public void setContext( Context context ) { this.context = context; }
    public String getPath() { return path; }
    public void setPath( String path ) { this.path = path; }
    public String getUserId() { return userId; }
    public void setUserId( String userId ) { this.userId = userId; }
    public String getGroupId() { return groupId; }
    public void setGroupId( String groupId ) { this.groupId = groupId; }

    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::chownRequest"; }    
    public long getTypeId() { return 3; }

    public String toString()
    {
        return "chownRequest( " + context.toString() + ", " + "\"" + path + "\"" + ", " + "\"" + userId + "\"" + ", " + "\"" + groupId + "\"" + " )"; 
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.context.deserialize( from_hash_map.get( "context" ) );
        this.path = ( String )from_hash_map.get( "path" );
        this.userId = ( String )from_hash_map.get( "userId" );
        this.groupId = ( String )from_hash_map.get( "groupId" );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.context.deserialize( from_array[0] );
        this.path = ( String )from_array[1];
        this.userId = ( String )from_array[2];
        this.groupId = ( String )from_array[3];        
    }

    public void deserialize( ReusableBuffer buf )
    {
        context = new Context(); context.deserialize( buf );
        path = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        userId = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        groupId = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "context", context.serialize() );
        to_hash_map.put( "path", path );
        to_hash_map.put( "userId", userId );
        to_hash_map.put( "groupId", groupId );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        context.serialize( writer );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( path, writer );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( userId, writer );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( groupId, writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += context.calculateSize();
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(path);
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(userId);
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(groupId);
        return my_size;
    }

    // Request
    public int getOperationNumber() { return 3; }
    public Response createDefaultResponse() { return new chownResponse(); }


    private Context context;
    private String path;
    private String userId;
    private String groupId;

}

