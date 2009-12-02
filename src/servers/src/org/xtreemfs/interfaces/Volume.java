package org.xtreemfs.interfaces;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class Volume implements Struct
{
    public static final int TAG = 2009120258;
    
    public Volume() { access_control_policy = AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL; default_striping_policy = new StripingPolicy();  }
    public Volume( AccessControlPolicyType access_control_policy, StripingPolicy default_striping_policy, String id, int mode, String name, String owner_group_id, String owner_user_id ) { this.access_control_policy = access_control_policy; this.default_striping_policy = default_striping_policy; this.id = id; this.mode = mode; this.name = name; this.owner_group_id = owner_group_id; this.owner_user_id = owner_user_id; }

    public AccessControlPolicyType getAccess_control_policy() { return access_control_policy; }
    public void setAccess_control_policy( AccessControlPolicyType access_control_policy ) { this.access_control_policy = access_control_policy; }
    public StripingPolicy getDefault_striping_policy() { return default_striping_policy; }
    public void setDefault_striping_policy( StripingPolicy default_striping_policy ) { this.default_striping_policy = default_striping_policy; }
    public String getId() { return id; }
    public void setId( String id ) { this.id = id; }
    public int getMode() { return mode; }
    public void setMode( int mode ) { this.mode = mode; }
    public String getName() { return name; }
    public void setName( String name ) { this.name = name; }
    public String getOwner_group_id() { return owner_group_id; }
    public void setOwner_group_id( String owner_group_id ) { this.owner_group_id = owner_group_id; }
    public String getOwner_user_id() { return owner_user_id; }
    public void setOwner_user_id( String owner_user_id ) { this.owner_user_id = owner_user_id; }

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
    public static final long serialVersionUID = 2009120258;    

    // yidl.runtime.Object
    public int getTag() { return 2009120258; }
    public String getTypeName() { return "org::xtreemfs::interfaces::Volume"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8; // access_control_policy
        my_size += default_striping_policy.getXDRSize(); // default_striping_policy
        my_size += Integer.SIZE / 8 + ( id != null ? ( ( id.getBytes().length % 4 == 0 ) ? id.getBytes().length : ( id.getBytes().length + 4 - id.getBytes().length % 4 ) ) : 0 ); // id
        my_size += Integer.SIZE / 8; // mode
        my_size += Integer.SIZE / 8 + ( name != null ? ( ( name.getBytes().length % 4 == 0 ) ? name.getBytes().length : ( name.getBytes().length + 4 - name.getBytes().length % 4 ) ) : 0 ); // name
        my_size += Integer.SIZE / 8 + ( owner_group_id != null ? ( ( owner_group_id.getBytes().length % 4 == 0 ) ? owner_group_id.getBytes().length : ( owner_group_id.getBytes().length + 4 - owner_group_id.getBytes().length % 4 ) ) : 0 ); // owner_group_id
        my_size += Integer.SIZE / 8 + ( owner_user_id != null ? ( ( owner_user_id.getBytes().length % 4 == 0 ) ? owner_user_id.getBytes().length : ( owner_user_id.getBytes().length + 4 - owner_user_id.getBytes().length % 4 ) ) : 0 ); // owner_user_id
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeInt32( access_control_policy, access_control_policy.intValue() );
        marshaller.writeStruct( "default_striping_policy", default_striping_policy );
        marshaller.writeString( "id", id );
        marshaller.writeUint32( "mode", mode );
        marshaller.writeString( "name", name );
        marshaller.writeString( "owner_group_id", owner_group_id );
        marshaller.writeString( "owner_user_id", owner_user_id );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        access_control_policy = AccessControlPolicyType.parseInt( unmarshaller.readInt32( "access_control_policy" ) );
        default_striping_policy = new StripingPolicy(); unmarshaller.readStruct( "default_striping_policy", default_striping_policy );
        id = unmarshaller.readString( "id" );
        mode = unmarshaller.readUint32( "mode" );
        name = unmarshaller.readString( "name" );
        owner_group_id = unmarshaller.readString( "owner_group_id" );
        owner_user_id = unmarshaller.readString( "owner_user_id" );    
    }
        
    

    private AccessControlPolicyType access_control_policy;
    private StripingPolicy default_striping_policy;
    private String id;
    private int mode;
    private String name;
    private String owner_group_id;
    private String owner_user_id;    

}

