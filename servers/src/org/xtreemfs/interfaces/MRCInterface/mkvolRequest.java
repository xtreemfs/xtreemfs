package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class mkvolRequest implements org.xtreemfs.interfaces.utils.Request
{
    public mkvolRequest() { context = new Context(); password = ""; volume_name = ""; osd_selection_policy = 0; default_striping_policy = new StripingPolicy(); access_control_policy = 0; }
    public mkvolRequest( Context context, String password, String volume_name, int osd_selection_policy, StripingPolicy default_striping_policy, int access_control_policy ) { this.context = context; this.password = password; this.volume_name = volume_name; this.osd_selection_policy = osd_selection_policy; this.default_striping_policy = default_striping_policy; this.access_control_policy = access_control_policy; }
    public mkvolRequest( Object from_hash_map ) { context = new Context(); password = ""; volume_name = ""; osd_selection_policy = 0; default_striping_policy = new StripingPolicy(); access_control_policy = 0; this.deserialize( from_hash_map ); }
    public mkvolRequest( Object[] from_array ) { context = new Context(); password = ""; volume_name = ""; osd_selection_policy = 0; default_striping_policy = new StripingPolicy(); access_control_policy = 0;this.deserialize( from_array ); }

    public Context getContext() { return context; }
    public void setContext( Context context ) { this.context = context; }
    public String getPassword() { return password; }
    public void setPassword( String password ) { this.password = password; }
    public String getVolume_name() { return volume_name; }
    public void setVolume_name( String volume_name ) { this.volume_name = volume_name; }
    public int getOsd_selection_policy() { return osd_selection_policy; }
    public void setOsd_selection_policy( int osd_selection_policy ) { this.osd_selection_policy = osd_selection_policy; }
    public StripingPolicy getDefault_striping_policy() { return default_striping_policy; }
    public void setDefault_striping_policy( StripingPolicy default_striping_policy ) { this.default_striping_policy = default_striping_policy; }
    public int getAccess_control_policy() { return access_control_policy; }
    public void setAccess_control_policy( int access_control_policy ) { this.access_control_policy = access_control_policy; }

    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::mkvolRequest"; }    
    public long getTypeId() { return 10; }

    public String toString()
    {
        return "mkvolRequest( " + context.toString() + ", " + "\"" + password + "\"" + ", " + "\"" + volume_name + "\"" + ", " + Integer.toString( osd_selection_policy ) + ", " + default_striping_policy.toString() + ", " + Integer.toString( access_control_policy ) + " )"; 
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.context.deserialize( from_hash_map.get( "context" ) );
        this.password = ( String )from_hash_map.get( "password" );
        this.volume_name = ( String )from_hash_map.get( "volume_name" );
        this.osd_selection_policy = ( ( Integer )from_hash_map.get( "osd_selection_policy" ) ).intValue();
        this.default_striping_policy.deserialize( from_hash_map.get( "default_striping_policy" ) );
        this.access_control_policy = ( ( Integer )from_hash_map.get( "access_control_policy" ) ).intValue();
    }
    
    public void deserialize( Object[] from_array )
    {
        this.context.deserialize( from_array[0] );
        this.password = ( String )from_array[1];
        this.volume_name = ( String )from_array[2];
        this.osd_selection_policy = ( ( Integer )from_array[3] ).intValue();
        this.default_striping_policy.deserialize( from_array[4] );
        this.access_control_policy = ( ( Integer )from_array[5] ).intValue();        
    }

    public void deserialize( ReusableBuffer buf )
    {
        context = new Context(); context.deserialize( buf );
        password = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        volume_name = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        osd_selection_policy = buf.getInt();
        default_striping_policy = new StripingPolicy(); default_striping_policy.deserialize( buf );
        access_control_policy = buf.getInt();
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "context", context.serialize() );
        to_hash_map.put( "password", password );
        to_hash_map.put( "volume_name", volume_name );
        to_hash_map.put( "osd_selection_policy", new Integer( osd_selection_policy ) );
        to_hash_map.put( "default_striping_policy", default_striping_policy.serialize() );
        to_hash_map.put( "access_control_policy", new Integer( access_control_policy ) );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        context.serialize( writer );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( password, writer );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( volume_name, writer );
        writer.putInt( osd_selection_policy );
        default_striping_policy.serialize( writer );
        writer.putInt( access_control_policy );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += context.calculateSize();
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(password);
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(volume_name);
        my_size += ( Integer.SIZE / 8 );
        my_size += default_striping_policy.calculateSize();
        my_size += ( Integer.SIZE / 8 );
        return my_size;
    }

    // Request
    public int getOperationNumber() { return 10; }
    public Response createDefaultResponse() { return new mkvolResponse(); }


    private Context context;
    private String password;
    private String volume_name;
    private int osd_selection_policy;
    private StripingPolicy default_striping_policy;
    private int access_control_policy;

}

