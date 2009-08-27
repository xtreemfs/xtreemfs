package org.xtreemfs.interfaces;

import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class Service implements org.xtreemfs.interfaces.utils.Serializable
{
    public static final int TAG = 2009082652;

    
    public Service() { type = ServiceType.SERVICE_TYPE_MIXED; uuid = ""; version = 0; name = ""; last_updated_s = 0; data = new ServiceDataMap(); }
    public Service( ServiceType type, String uuid, long version, String name, long last_updated_s, ServiceDataMap data ) { this.type = type; this.uuid = uuid; this.version = version; this.name = name; this.last_updated_s = last_updated_s; this.data = data; }
    public Service( Object from_hash_map ) { type = ServiceType.SERVICE_TYPE_MIXED; uuid = ""; version = 0; name = ""; last_updated_s = 0; data = new ServiceDataMap(); this.deserialize( from_hash_map ); }
    public Service( Object[] from_array ) { type = ServiceType.SERVICE_TYPE_MIXED; uuid = ""; version = 0; name = ""; last_updated_s = 0; data = new ServiceDataMap();this.deserialize( from_array ); }

    public ServiceType getType() { return type; }
    public void setType( ServiceType type ) { this.type = type; }
    public String getUuid() { return uuid; }
    public void setUuid( String uuid ) { this.uuid = uuid; }
    public long getVersion() { return version; }
    public void setVersion( long version ) { this.version = version; }
    public String getName() { return name; }
    public void setName( String name ) { this.name = name; }
    public long getLast_updated_s() { return last_updated_s; }
    public void setLast_updated_s( long last_updated_s ) { this.last_updated_s = last_updated_s; }
    public ServiceDataMap getData() { return data; }
    public void setData( ServiceDataMap data ) { this.data = data; }

    // Object
    public String toString()
    {
        return "Service( " + type.toString() + ", " + "\"" + uuid + "\"" + ", " + Long.toString( version ) + ", " + "\"" + name + "\"" + ", " + Long.toString( last_updated_s ) + ", " + data.toString() + " )";
    }

    // Serializable
    public int getTag() { return 2009082652; }
    public String getTypeName() { return "org::xtreemfs::interfaces::Service"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        
        this.uuid = ( String )from_hash_map.get( "uuid" );
        this.version = ( ( Long )from_hash_map.get( "version" ) ).longValue();
        this.name = ( String )from_hash_map.get( "name" );
        this.last_updated_s = ( ( Long )from_hash_map.get( "last_updated_s" ) ).longValue();
        this.data.deserialize( ( HashMap<String, Object> )from_hash_map.get( "data" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        
        this.uuid = ( String )from_array[1];
        this.version = ( ( Long )from_array[2] ).longValue();
        this.name = ( String )from_array[3];
        this.last_updated_s = ( ( Long )from_array[4] ).longValue();
        this.data.deserialize( ( HashMap<String, Object> )from_array[5] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        type = ServiceType.parseInt( buf.getInt() );
        uuid = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        version = buf.getLong();
        name = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        last_updated_s = buf.getLong();
        data = new ServiceDataMap(); data.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "type", type );
        to_hash_map.put( "uuid", uuid );
        to_hash_map.put( "version", new Long( version ) );
        to_hash_map.put( "name", name );
        to_hash_map.put( "last_updated_s", new Long( last_updated_s ) );
        to_hash_map.put( "data", data.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        writer.putInt( type.intValue() );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( uuid, writer );
        writer.putLong( version );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( name, writer );
        writer.putLong( last_updated_s );
        data.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += 4;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(uuid);
        my_size += ( Long.SIZE / 8 );
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(name);
        my_size += ( Long.SIZE / 8 );
        my_size += data.calculateSize();
        return my_size;
    }


    private ServiceType type;
    private String uuid;
    private long version;
    private String name;
    private long last_updated_s;
    private ServiceDataMap data;    

}

