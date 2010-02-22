package org.xtreemfs.interfaces.DIRInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class xtreemfs_service_get_by_uuidRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2010022324;

    public xtreemfs_service_get_by_uuidRequest() {  }
    public xtreemfs_service_get_by_uuidRequest( String uuid ) { this.uuid = uuid; }

    public String getUuid() { return uuid; }
    public void setUuid( String uuid ) { this.uuid = uuid; }

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

    // Request
    public Response createDefaultResponse() { return new xtreemfs_service_get_by_uuidResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2010022324;

    // yidl.runtime.Object
    public int getTag() { return 2010022324; }
    public String getTypeName() { return "org::xtreemfs::interfaces::DIRInterface::xtreemfs_service_get_by_uuidRequest"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8 + ( uuid != null ? ( ( uuid.getBytes().length % 4 == 0 ) ? uuid.getBytes().length : ( uuid.getBytes().length + 4 - uuid.getBytes().length % 4 ) ) : 0 ); // uuid
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "uuid", uuid );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        uuid = unmarshaller.readString( "uuid" );
    }

    

    private String uuid;

}

