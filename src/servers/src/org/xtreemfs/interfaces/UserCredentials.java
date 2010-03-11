package org.xtreemfs.interfaces;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class UserCredentials implements Struct
{
    public static final int TAG = 2010030918;

    public UserCredentials() { group_ids = new StringSet();  }
    public UserCredentials( String user_id, StringSet group_ids, String password ) { this.user_id = user_id; this.group_ids = group_ids; this.password = password; }

    public String getUser_id() { return user_id; }
    public StringSet getGroup_ids() { return group_ids; }
    public String getPassword() { return password; }
    public void setUser_id( String user_id ) { this.user_id = user_id; }
    public void setGroup_ids( StringSet group_ids ) { this.group_ids = group_ids; }
    public void setPassword( String password ) { this.password = password; }

    // java.lang.Object
    public String toString()
    {
        StringWriter string_writer = new StringWriter();
        string_writer.append(this.getClass().getCanonicalName());
        string_writer.append(" ");
        PrettyPrinter pretty_printer = new PrettyPrinter( string_writer );
        pretty_printer.writeStruct( "", this );
        return string_writer.toString();
    }

    // java.io.Serializable
    public static final long serialVersionUID = 2010030918;

    // yidl.runtime.Object
    public int getTag() { return 2010030918; }
    public String getTypeName() { return "org::xtreemfs::interfaces::UserCredentials"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8 + ( user_id != null ? ( ( user_id.getBytes().length % 4 == 0 ) ? user_id.getBytes().length : ( user_id.getBytes().length + 4 - user_id.getBytes().length % 4 ) ) : 0 ); // user_id
        my_size += group_ids.getXDRSize(); // group_ids
        my_size += Integer.SIZE / 8 + ( password != null ? ( ( password.getBytes().length % 4 == 0 ) ? password.getBytes().length : ( password.getBytes().length + 4 - password.getBytes().length % 4 ) ) : 0 ); // password
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
