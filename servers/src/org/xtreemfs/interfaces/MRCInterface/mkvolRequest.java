package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.MRCInterface.*;
import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         

public class mkvolRequest implements Request
{
    public mkvolRequest() { context = new org.xtreemfs.interfaces.Context(); password = ""; volume_name = ""; osd_selection_policy = 0; default_striping_policy = new org.xtreemfs.interfaces.StripingPolicy(); access_control_policy = 0; }
    public mkvolRequest( Context context, String password, String volume_name, int osd_selection_policy, StripingPolicy default_striping_policy, int access_control_policy ) { this.context = context; this.password = password; this.volume_name = volume_name; this.osd_selection_policy = osd_selection_policy; this.default_striping_policy = default_striping_policy; this.access_control_policy = access_control_policy; }

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

    // Object
    public String toString()
    {
        return "mkvolRequest( " + context.toString() + ", " + "\"" + password + "\"" + ", " + "\"" + volume_name + "\"" + ", " + Integer.toString( osd_selection_policy ) + ", " + default_striping_policy.toString() + ", " + Integer.toString( access_control_policy ) + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::MRCInterface::mkvolRequest"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        context.serialize( writer );
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(password,writer); }
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(volume_name,writer); }
        writer.putInt( osd_selection_policy );
        default_striping_policy.serialize( writer );
        writer.putInt( access_control_policy );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        context = new org.xtreemfs.interfaces.Context(); context.deserialize( buf );
        { password = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        { volume_name = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        osd_selection_policy = buf.getInt();
        default_striping_policy = new org.xtreemfs.interfaces.StripingPolicy(); default_striping_policy.deserialize( buf );
        access_control_policy = buf.getInt();    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += context.calculateSize();
        my_size += 4 + ( password.length() + 4 - ( password.length() % 4 ) );
        my_size += 4 + ( volume_name.length() + 4 - ( volume_name.length() % 4 ) );
        my_size += ( Integer.SIZE / 8 );
        my_size += default_striping_policy.calculateSize();
        my_size += ( Integer.SIZE / 8 );
        return my_size;
    }

    private Context context;
    private String password;
    private String volume_name;
    private int osd_selection_policy;
    private StripingPolicy default_striping_policy;
    private int access_control_policy;
    

    // Request
    public int getInterfaceVersion() { return 2; }    
    public int getOperationNumber() { return 10; }
    public Response createDefaultResponse() { return new mkvolResponse(); }

}

