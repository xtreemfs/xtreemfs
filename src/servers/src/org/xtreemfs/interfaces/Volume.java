package org.xtreemfs.interfaces;

import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class Volume implements org.xtreemfs.interfaces.utils.Serializable
{
    public static final int TAG = 1044;

    
    public Volume() { name = ""; mode = 0; osd_selection_policy = OSDSelectionPolicyType.OSD_SELECTION_POLICY_SIMPLE; default_striping_policy = new StripingPolicy(); access_control_policy = AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL; id = ""; owner_user_id = ""; owner_group_id = ""; }
    public Volume( String name, int mode, OSDSelectionPolicyType osd_selection_policy, StripingPolicy default_striping_policy, AccessControlPolicyType access_control_policy, String id, String owner_user_id, String owner_group_id ) { this.name = name; this.mode = mode; this.osd_selection_policy = osd_selection_policy; this.default_striping_policy = default_striping_policy; this.access_control_policy = access_control_policy; this.id = id; this.owner_user_id = owner_user_id; this.owner_group_id = owner_group_id; }
    public Volume( Object from_hash_map ) { name = ""; mode = 0; osd_selection_policy = OSDSelectionPolicyType.OSD_SELECTION_POLICY_SIMPLE; default_striping_policy = new StripingPolicy(); access_control_policy = AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL; id = ""; owner_user_id = ""; owner_group_id = ""; this.deserialize( from_hash_map ); }
    public Volume( Object[] from_array ) { name = ""; mode = 0; osd_selection_policy = OSDSelectionPolicyType.OSD_SELECTION_POLICY_SIMPLE; default_striping_policy = new StripingPolicy(); access_control_policy = AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL; id = ""; owner_user_id = ""; owner_group_id = "";this.deserialize( from_array ); }

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

    // Object
    public String toString()
    {
        return "Volume( " + "\"" + name + "\"" + ", " + Integer.toString( mode ) + ", " + osd_selection_policy.toString() + ", " + default_striping_policy.toString() + ", " + access_control_policy.toString() + ", " + "\"" + id + "\"" + ", " + "\"" + owner_user_id + "\"" + ", " + "\"" + owner_group_id + "\"" + " )";
    }

    // Serializable
    public int getTag() { return 1044; }
    public String getTypeName() { return "org::xtreemfs::interfaces::Volume"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.name = ( String )from_hash_map.get( "name" );
        this.mode = ( from_hash_map.get( "mode" ) instanceof Integer ) ? ( ( Integer )from_hash_map.get( "mode" ) ).intValue() : ( ( Long )from_hash_map.get( "mode" ) ).intValue();
        
        this.default_striping_policy.deserialize( from_hash_map.get( "default_striping_policy" ) );
        
        this.id = ( String )from_hash_map.get( "id" );
        this.owner_user_id = ( String )from_hash_map.get( "owner_user_id" );
        this.owner_group_id = ( String )from_hash_map.get( "owner_group_id" );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.name = ( String )from_array[0];
        this.mode = ( from_array[1] instanceof Integer ) ? ( ( Integer )from_array[1] ).intValue() : ( ( Long )from_array[1] ).intValue();
        
        this.default_striping_policy.deserialize( from_array[3] );
        
        this.id = ( String )from_array[5];
        this.owner_user_id = ( String )from_array[6];
        this.owner_group_id = ( String )from_array[7];        
    }

    public void deserialize( ReusableBuffer buf )
    {
        name = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        mode = buf.getInt();
        osd_selection_policy = OSDSelectionPolicyType.parseInt( buf.getInt() );
        default_striping_policy = new StripingPolicy(); default_striping_policy.deserialize( buf );
        access_control_policy = AccessControlPolicyType.parseInt( buf.getInt() );
        id = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        owner_user_id = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        owner_group_id = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "name", name );
        to_hash_map.put( "mode", new Integer( mode ) );
        to_hash_map.put( "osd_selection_policy", osd_selection_policy );
        to_hash_map.put( "default_striping_policy", default_striping_policy.serialize() );
        to_hash_map.put( "access_control_policy", access_control_policy );
        to_hash_map.put( "id", id );
        to_hash_map.put( "owner_user_id", owner_user_id );
        to_hash_map.put( "owner_group_id", owner_group_id );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( name, writer );
        writer.putInt( mode );
        writer.putInt( osd_selection_policy.intValue() );
        default_striping_policy.serialize( writer );
        writer.putInt( access_control_policy.intValue() );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( id, writer );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( owner_user_id, writer );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( owner_group_id, writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(name);
        my_size += ( Integer.SIZE / 8 );
        my_size += 4;
        my_size += default_striping_policy.calculateSize();
        my_size += 4;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(id);
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(owner_user_id);
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(owner_group_id);
        return my_size;
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

