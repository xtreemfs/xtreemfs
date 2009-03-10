package org.xtreemfs.interfaces;

import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class ServiceRegistry implements org.xtreemfs.interfaces.utils.Serializable
{
    public ServiceRegistry() { uuid = ""; version = 0; service_type = 0; service_name = ""; data = new KeyValuePairSet(); }
    public ServiceRegistry( String uuid, long version, int service_type, String service_name, KeyValuePairSet data ) { this.uuid = uuid; this.version = version; this.service_type = service_type; this.service_name = service_name; this.data = data; }
    public ServiceRegistry( Object from_hash_map ) { uuid = ""; version = 0; service_type = 0; service_name = ""; data = new KeyValuePairSet(); this.deserialize( from_hash_map ); }
    public ServiceRegistry( Object[] from_array ) { uuid = ""; version = 0; service_type = 0; service_name = ""; data = new KeyValuePairSet();this.deserialize( from_array ); }

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

    public String getTypeName() { return "org::xtreemfs::interfaces::ServiceRegistry"; }    
    public long getTypeId() { return 0; }

    public String toString()
    {
        return "ServiceRegistry( " + "\"" + uuid + "\"" + ", " + Long.toString( version ) + ", " + Integer.toString( service_type ) + ", " + "\"" + service_name + "\"" + ", " + data.toString() + " )"; 
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.uuid = ( String )from_hash_map.get( "uuid" );
        this.version = ( ( Long )from_hash_map.get( "version" ) ).longValue();
        this.service_type = ( ( Integer )from_hash_map.get( "service_type" ) ).intValue();
        this.service_name = ( String )from_hash_map.get( "service_name" );
        this.data.deserialize( from_hash_map.get( "data" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.uuid = ( String )from_array[0];
        this.version = ( ( Long )from_array[1] ).longValue();
        this.service_type = ( ( Integer )from_array[2] ).intValue();
        this.service_name = ( String )from_array[3];
        this.data.deserialize( from_array[4] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        uuid = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        version = buf.getLong();
        service_type = buf.getInt();
        service_name = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        data = new KeyValuePairSet(); data.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "uuid", uuid );
        to_hash_map.put( "version", new Long( version ) );
        to_hash_map.put( "service_type", new Integer( service_type ) );
        to_hash_map.put( "service_name", service_name );
        to_hash_map.put( "data", data.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( uuid, writer );
        writer.putLong( version );
        writer.putInt( service_type );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( service_name, writer );
        data.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(uuid);
        my_size += ( Long.SIZE / 8 );
        my_size += ( Integer.SIZE / 8 );
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(service_name);
        my_size += data.calculateSize();
        return my_size;
    }


    private String uuid;
    private long version;
    private int service_type;
    private String service_name;
    private KeyValuePairSet data;

}

