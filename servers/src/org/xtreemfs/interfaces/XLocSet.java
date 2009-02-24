package org.xtreemfs.interfaces;

import org.xtreemfs.interfaces.*;

import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         
   
public class XLocSet implements Serializable
{
    public XLocSet() { osds_by_striping_policy = new org.xtreemfs.interfaces.OSDsByStripingPolicySet(); version = 0; repUpdatePolicy = ""; __json = ""; }
    public XLocSet( OSDsByStripingPolicySet osds_by_striping_policy, long version, String repUpdatePolicy, String __json ) { this.osds_by_striping_policy = osds_by_striping_policy; this.version = version; this.repUpdatePolicy = repUpdatePolicy; this.__json = __json; }

    public OSDsByStripingPolicySet getOsds_by_striping_policy() { return osds_by_striping_policy; }
    public void setOsds_by_striping_policy( OSDsByStripingPolicySet osds_by_striping_policy ) { this.osds_by_striping_policy = osds_by_striping_policy; }
    public long getVersion() { return version; }
    public void setVersion( long version ) { this.version = version; }
    public String getRepupdatepolicy() { return repUpdatePolicy; }
    public void setRepupdatepolicy( String repUpdatePolicy ) { this.repUpdatePolicy = repUpdatePolicy; }
    public String get__json() { return __json; }
    public void set__json( String __json ) { this.__json = __json; }

    // Object
    public String toString()
    {
        return "XLocSet( " + osds_by_striping_policy.toString() + ", " + Long.toString( version ) + ", " + "\"" + repUpdatePolicy + "\"" + ", " + "\"" + __json + "\"" + " )";
    }    

    // Serializable
    public void serialize(ONCRPCBufferWriter writer) {
        osds_by_striping_policy.serialize( writer );
        writer.putLong( version );
        { final byte[] bytes = repUpdatePolicy.getBytes(); writer.putInt( bytes.length ); writer.put( bytes );  if (bytes.length % 4 > 0) {for (int k = 0; k < (4 - (bytes.length % 4)); k++) { writer.put((byte)0); } }}
        { final byte[] bytes = __json.getBytes(); writer.putInt( bytes.length ); writer.put( bytes );  if (bytes.length % 4 > 0) {for (int k = 0; k < (4 - (bytes.length % 4)); k++) { writer.put((byte)0); } }}        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        osds_by_striping_policy = new org.xtreemfs.interfaces.OSDsByStripingPolicySet(); osds_by_striping_policy.deserialize( buf );
        version = buf.getLong();
        { int repUpdatePolicy_new_length = buf.getInt(); byte[] repUpdatePolicy_new_bytes = new byte[repUpdatePolicy_new_length]; buf.get( repUpdatePolicy_new_bytes ); repUpdatePolicy = new String( repUpdatePolicy_new_bytes ); if (repUpdatePolicy_new_length % 4 > 0) {for (int k = 0; k < (4 - (repUpdatePolicy_new_length % 4)); k++) { buf.get(); } } }
        { int __json_new_length = buf.getInt(); byte[] __json_new_bytes = new byte[__json_new_length]; buf.get( __json_new_bytes ); __json = new String( __json_new_bytes ); if (__json_new_length % 4 > 0) {for (int k = 0; k < (4 - (__json_new_length % 4)); k++) { buf.get(); } } }    
    }
    
    public int getSize()
    {
        int my_size = 0;
        my_size += osds_by_striping_policy.getSize();
        my_size += ( Long.SIZE / 8 );
        my_size += 4 + ( repUpdatePolicy.length() + 4 - ( repUpdatePolicy.length() % 4 ) );
        my_size += 4 + ( __json.length() + 4 - ( __json.length() % 4 ) );
        return my_size;
    }

    private OSDsByStripingPolicySet osds_by_striping_policy;
    private long version;
    private String repUpdatePolicy;
    private String __json;

}

