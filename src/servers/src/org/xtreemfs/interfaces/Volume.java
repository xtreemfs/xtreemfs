package org.xtreemfs.interfaces;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class Volume extends Struct
{
    public static final int TAG = 2009082662;
    
    public Volume() { osd_selection_policy = OSDSelectionPolicyType.OSD_SELECTION_POLICY_SIMPLE; default_striping_policy = new StripingPolicy(); access_control_policy = AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL;  }
    public Volume( String name, int mode, OSDSelectionPolicyType osd_selection_policy, StripingPolicy default_striping_policy, AccessControlPolicyType access_control_policy, String id, String owner_user_id, String owner_group_id ) { this.name = name; this.mode = mode; this.osd_selection_policy = osd_selection_policy; this.default_striping_policy = default_striping_policy; this.access_control_policy = access_control_policy; this.id = id; this.owner_user_id = owner_user_id; this.owner_group_id = owner_group_id; }

    public String getName() { return name; }
    public void setName( String name ) { this.name = name; }
    public int getMode() { return mode; }
    public void setMode( int mode ) { this.mode = mode; }
    public OSDSelectionPolicyType getOsd_selection_policy() { return osd_selection_policy; }
    public void setOsd_selection_policy( OSDSelectionPolicyType osd_selection_policy ) { this.osd_selection_policy = osd_selection_policy; }
    public StripingPolicy getDefault_striping_policy() { return default_striping_policy; }
    public void setDefault_striping_policy( StripingPolicy default_striping_policy ) { this.default_striping_policy = default_striping_policy; }
    public AccessControlPolicyType getAccess_control_policy() { return access_control_policy; }
    public void setAccess_control_policy( AccessControlPolicyType access_control_policy ) { this.access_control_policy = access_control_policy; }
    public String getId() { return id; }
    public void setId( String id ) { this.id = id; }
    public String getOwner_user_id() { return owner_user_id; }
    public void setOwner_user_id( String owner_user_id ) { this.owner_user_id = owner_user_id; }
    public String getOwner_group_id() { return owner_group_id; }
    public void setOwner_group_id( String owner_group_id ) { this.owner_group_id = owner_group_id; }

    // java.io.Serializable
    public static final long serialVersionUID = 2009082662;    

    // yidl.Object
    public int getTag() { return 2009082662; }
    public String getTypeName() { return "org::xtreemfs::interfaces::Volume"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += ( ( name.getBytes().length + Integer.SIZE/8 ) % 4 == 0 ) ? ( name.getBytes().length + Integer.SIZE/8 ) : ( name.getBytes().length + Integer.SIZE/8 + 4 - ( name.getBytes().length + Integer.SIZE/8 ) % 4 );
        my_size += ( Integer.SIZE / 8 );
        my_size += 4;
        my_size += default_striping_policy.getXDRSize();
        my_size += 4;
        my_size += ( ( id.getBytes().length + Integer.SIZE/8 ) % 4 == 0 ) ? ( id.getBytes().length + Integer.SIZE/8 ) : ( id.getBytes().length + Integer.SIZE/8 + 4 - ( id.getBytes().length + Integer.SIZE/8 ) % 4 );
        my_size += ( ( owner_user_id.getBytes().length + Integer.SIZE/8 ) % 4 == 0 ) ? ( owner_user_id.getBytes().length + Integer.SIZE/8 ) : ( owner_user_id.getBytes().length + Integer.SIZE/8 + 4 - ( owner_user_id.getBytes().length + Integer.SIZE/8 ) % 4 );
        my_size += ( ( owner_group_id.getBytes().length + Integer.SIZE/8 ) % 4 == 0 ) ? ( owner_group_id.getBytes().length + Integer.SIZE/8 ) : ( owner_group_id.getBytes().length + Integer.SIZE/8 + 4 - ( owner_group_id.getBytes().length + Integer.SIZE/8 ) % 4 );
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "name", name );
        marshaller.writeUint32( "mode", mode );
        marshaller.writeInt32( osd_selection_policy, osd_selection_policy.intValue() );
        marshaller.writeStruct( "default_striping_policy", default_striping_policy );
        marshaller.writeInt32( access_control_policy, access_control_policy.intValue() );
        marshaller.writeString( "id", id );
        marshaller.writeString( "owner_user_id", owner_user_id );
        marshaller.writeString( "owner_group_id", owner_group_id );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        name = unmarshaller.readString( "name" );
        mode = unmarshaller.readUint32( "mode" );
        osd_selection_policy = OSDSelectionPolicyType.parseInt( unmarshaller.readInt32( "osd_selection_policy" ) );
        default_striping_policy = new StripingPolicy(); unmarshaller.readStruct( "default_striping_policy", default_striping_policy );
        access_control_policy = AccessControlPolicyType.parseInt( unmarshaller.readInt32( "access_control_policy" ) );
        id = unmarshaller.readString( "id" );
        owner_user_id = unmarshaller.readString( "owner_user_id" );
        owner_group_id = unmarshaller.readString( "owner_group_id" );    
    }
        
    

    private String name;
    private int mode;
    private OSDSelectionPolicyType osd_selection_policy;
    private StripingPolicy default_striping_policy;
    private AccessControlPolicyType access_control_policy;
    private String id;
    private String owner_user_id;
    private String owner_group_id;    

}

