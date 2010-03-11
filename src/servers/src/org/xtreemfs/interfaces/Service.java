package org.xtreemfs.interfaces;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class Service implements Struct
{
    public static final int TAG = 2010030950;

    public Service() { type = ServiceType.SERVICE_TYPE_MIXED; data = new ServiceDataMap();  }
    public Service( ServiceType type, String uuid, long version, String name, long last_updated_s, ServiceDataMap data ) { this.type = type; this.uuid = uuid; this.version = version; this.name = name; this.last_updated_s = last_updated_s; this.data = data; }

    public ServiceType getType() { return type; }
    public String getUuid() { return uuid; }
    public long getVersion() { return version; }
    public String getName() { return name; }
    public long getLast_updated_s() { return last_updated_s; }
    public ServiceDataMap getData() { return data; }
    public void setType( ServiceType type ) { this.type = type; }
    public void setUuid( String uuid ) { this.uuid = uuid; }
    public void setVersion( long version ) { this.version = version; }
    public void setName( String name ) { this.name = name; }
    public void setLast_updated_s( long last_updated_s ) { this.last_updated_s = last_updated_s; }
    public void setData( ServiceDataMap data ) { this.data = data; }

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
    public static final long serialVersionUID = 2010030950;

    // yidl.runtime.Object
    public int getTag() { return 2010030950; }
    public String getTypeName() { return "org::xtreemfs::interfaces::Service"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8; // type
        my_size += Integer.SIZE / 8 + ( uuid != null ? ( ( uuid.getBytes().length % 4 == 0 ) ? uuid.getBytes().length : ( uuid.getBytes().length + 4 - uuid.getBytes().length % 4 ) ) : 0 ); // uuid
        my_size += Long.SIZE / 8; // version
        my_size += Integer.SIZE / 8 + ( name != null ? ( ( name.getBytes().length % 4 == 0 ) ? name.getBytes().length : ( name.getBytes().length + 4 - name.getBytes().length % 4 ) ) : 0 ); // name
        my_size += Long.SIZE / 8; // last_updated_s
        my_size += data.getXDRSize(); // data
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeInt32( type, type.intValue() );
        marshaller.writeString( "uuid", uuid );
        marshaller.writeUint64( "version", version );
        marshaller.writeString( "name", name );
        marshaller.writeUint64( "last_updated_s", last_updated_s );
        marshaller.writeMap( "data", data );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        type = ServiceType.parseInt( unmarshaller.readInt32( "type" ) );
        uuid = unmarshaller.readString( "uuid" );
        version = unmarshaller.readUint64( "version" );
        name = unmarshaller.readString( "name" );
        last_updated_s = unmarshaller.readUint64( "last_updated_s" );
        data = new ServiceDataMap(); unmarshaller.readMap( "data", data );
    }

    private ServiceType type;
    private String uuid;
    private long version;
    private String name;
    private long last_updated_s;
    private ServiceDataMap data;
}
