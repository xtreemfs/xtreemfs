package org.xtreemfs.interfaces;

import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         
   
public class ServiceRegistry implements org.xtreemfs.interfaces.utils.Serializable
{
    public ServiceRegistry() { uuid = ""; version = 0; service_type = 0; service_name = ""; data = new org.xtreemfs.interfaces.KeyValuePairSet(); }
    public ServiceRegistry( String uuid, long version, int service_type, String service_name, KeyValuePairSet data ) { this.uuid = uuid; this.version = version; this.service_type = service_type; this.service_name = service_name; this.data = data; }

    public String getUuid() { return uuid; }
    public void setUuid( String uuid ) { this.uuid = uuid; }
    public long getVersion() { return version; }
    public void setVersion( long version ) { this.version = version; }
    public int getService_type() { return service_type; }
    public void setService_type( int service_type ) { this.service_type = service_type; }
    public String getService_name() { return service_name; }
    public void setService_name( String service_name ) { this.service_name = service_name; }
    public KeyValuePairSet getData() { return data; }
    public void setData( KeyValuePairSet data ) { this.data = data; }

    // Object
    public String toString()
    {
        return "ServiceRegistry( " + "\"" + uuid + "\"" + ", " + Long.toString( version ) + ", " + Integer.toString( service_type ) + ", " + "\"" + service_name + "\"" + ", " + data.toString() + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::ServiceRegistry"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(uuid,writer); }
        writer.putLong( version );
        writer.putInt( service_type );
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(service_name,writer); }
        data.serialize( writer );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        { uuid = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        version = buf.getLong();
        service_type = buf.getInt();
        { service_name = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        data = new org.xtreemfs.interfaces.KeyValuePairSet(); data.deserialize( buf );    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += 4 + ( uuid.length() + 4 - ( uuid.length() % 4 ) );
        my_size += ( Long.SIZE / 8 );
        my_size += ( Integer.SIZE / 8 );
        my_size += 4 + ( service_name.length() + 4 - ( service_name.length() % 4 ) );
        my_size += data.calculateSize();
        return my_size;
    }

    private String uuid;
    private long version;
    private int service_type;
    private String service_name;
    private KeyValuePairSet data;

}

