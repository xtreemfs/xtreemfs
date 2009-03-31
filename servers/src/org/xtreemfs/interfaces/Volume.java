package org.xtreemfs.interfaces;

import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class Volume implements org.xtreemfs.interfaces.utils.Serializable
{
    public Volume() { name = ""; mode = 0; osd_selection_policy = 0; default_striping_policy = new StripingPolicy(); access_control_policy = 0; }
    public Volume( String name, int mode, int osd_selection_policy, StripingPolicy default_striping_policy, int access_control_policy ) { this.name = name; this.mode = mode; this.osd_selection_policy = osd_selection_policy; this.default_striping_policy = default_striping_policy; this.access_control_policy = access_control_policy; }
    public Volume( Object from_hash_map ) { name = ""; mode = 0; osd_selection_policy = 0; default_striping_policy = new StripingPolicy(); access_control_policy = 0; this.deserialize( from_hash_map ); }
    public Volume( Object[] from_array ) { name = ""; mode = 0; osd_selection_policy = 0; default_striping_policy = new StripingPolicy(); access_control_policy = 0;this.deserialize( from_array ); }

    public String getName() { return name; }
    public void setName( String name ) { this.name = name; }
    public int getMode() { return mode; }
    public void setMode( int mode ) { this.mode = mode; }
    public int getOsd_selection_policy() { return osd_selection_policy; }
    public void setOsd_selection_policy( int osd_selection_policy ) { this.osd_selection_policy = osd_selection_policy; }
    public StripingPolicy getDefault_striping_policy() { return default_striping_policy; }
    public void setDefault_striping_policy( StripingPolicy default_striping_policy ) { this.default_striping_policy = default_striping_policy; }
    public int getAccess_control_policy() { return access_control_policy; }
    public void setAccess_control_policy( int access_control_policy ) { this.access_control_policy = access_control_policy; }

    public String getTypeName() { return "org::xtreemfs::interfaces::Volume"; }    
    public long getTypeId() { return 0; }

    public String toString()
    {
        return "Volume( " + "\"" + name + "\"" + ", " + Integer.toString( mode ) + ", " + Integer.toString( osd_selection_policy ) + ", " + default_striping_policy.toString() + ", " + Integer.toString( access_control_policy ) + " )";
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.name = ( String )from_hash_map.get( "name" );
        this.mode = ( ( Integer )from_hash_map.get( "mode" ) ).intValue();
        this.osd_selection_policy = ( ( Integer )from_hash_map.get( "osd_selection_policy" ) ).intValue();
        this.default_striping_policy.deserialize( from_hash_map.get( "default_striping_policy" ) );
        this.access_control_policy = ( ( Integer )from_hash_map.get( "access_control_policy" ) ).intValue();
    }
    
    public void deserialize( Object[] from_array )
    {
        this.name = ( String )from_array[0];
        this.mode = ( ( Integer )from_array[1] ).intValue();
        this.osd_selection_policy = ( ( Integer )from_array[2] ).intValue();
        this.default_striping_policy.deserialize( from_array[3] );
        this.access_control_policy = ( ( Integer )from_array[4] ).intValue();        
    }

    public void deserialize( ReusableBuffer buf )
    {
        name = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        mode = buf.getInt();
        osd_selection_policy = buf.getInt();
        default_striping_policy = new StripingPolicy(); default_striping_policy.deserialize( buf );
        access_control_policy = buf.getInt();
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "name", name );
        to_hash_map.put( "mode", new Integer( mode ) );
        to_hash_map.put( "osd_selection_policy", new Integer( osd_selection_policy ) );
        to_hash_map.put( "default_striping_policy", default_striping_policy.serialize() );
        to_hash_map.put( "access_control_policy", new Integer( access_control_policy ) );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( name, writer );
        writer.putInt( mode );
        writer.putInt( osd_selection_policy );
        default_striping_policy.serialize( writer );
        writer.putInt( access_control_policy );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(name);
        my_size += ( Integer.SIZE / 8 );
        my_size += ( Integer.SIZE / 8 );
        my_size += default_striping_policy.calculateSize();
        my_size += ( Integer.SIZE / 8 );
        return my_size;
    }


    private String name;
    private int mode;
    private int osd_selection_policy;
    private StripingPolicy default_striping_policy;
    private int access_control_policy;

}

