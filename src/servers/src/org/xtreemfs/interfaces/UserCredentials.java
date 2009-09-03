package org.xtreemfs.interfaces;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class UserCredentials extends Struct
{
    public static final int TAG = 2009082620;
    
    public UserCredentials() { group_ids = new StringSet();  }
    public UserCredentials( String user_id, StringSet group_ids, String password ) { this.user_id = user_id; this.group_ids = group_ids; this.password = password; }

    public String getUser_id() { return user_id; }
    public void setUser_id( String user_id ) { this.user_id = user_id; }
    public StringSet getGroup_ids() { return group_ids; }
    public void setGroup_ids( StringSet group_ids ) { this.group_ids = group_ids; }
    public String getPassword() { return password; }
    public void setPassword( String password ) { this.password = password; }

    // java.io.Serializable
    public static final long serialVersionUID = 2009082620;    

    // yidl.Object
    public int getTag() { return 2009082620; }
    public String getTypeName() { return "org::xtreemfs::interfaces::UserCredentials"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += ( ( user_id.getBytes().length + Integer.SIZE/8 ) % 4 == 0 ) ? ( user_id.getBytes().length + Integer.SIZE/8 ) : ( user_id.getBytes().length + Integer.SIZE/8 + 4 - ( user_id.getBytes().length + Integer.SIZE/8 ) % 4 );
        my_size += group_ids.getXDRSize();
        my_size += ( ( password.getBytes().length + Integer.SIZE/8 ) % 4 == 0 ) ? ( password.getBytes().length + Integer.SIZE/8 ) : ( password.getBytes().length + Integer.SIZE/8 + 4 - ( password.getBytes().length + Integer.SIZE/8 ) % 4 );
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "user_id", user_id );
        marshaller.writeSequence( "group_ids", group_ids );
        marshaller.writeString( "password", password );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        user_id = unmarshaller.readString( "user_id" );
        group_ids = new StringSet(); unmarshaller.readSequence( "group_ids", group_ids );
        password = unmarshaller.readString( "password" );    
    }
        
    

    private String user_id;
    private StringSet group_ids;
    private String password;    

}

