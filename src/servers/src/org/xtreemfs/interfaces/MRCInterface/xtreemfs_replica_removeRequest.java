package org.xtreemfs.interfaces.MRCInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class xtreemfs_replica_removeRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009120353;
    
    public xtreemfs_replica_removeRequest() {  }
    public xtreemfs_replica_removeRequest( String file_id, String osd_uuid ) { this.file_id = file_id; this.osd_uuid = osd_uuid; }

    public String getFile_id() { return file_id; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }
    public String getOsd_uuid() { return osd_uuid; }
    public void setOsd_uuid( String osd_uuid ) { this.osd_uuid = osd_uuid; }

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
    public Response createDefaultResponse() { return new xtreemfs_replica_removeResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009120353;    

    // yidl.runtime.Object
    public int getTag() { return 2009120353; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_replica_removeRequest"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8 + ( file_id != null ? ( ( file_id.getBytes().length % 4 == 0 ) ? file_id.getBytes().length : ( file_id.getBytes().length + 4 - file_id.getBytes().length % 4 ) ) : 0 ); // file_id
        my_size += Integer.SIZE / 8 + ( osd_uuid != null ? ( ( osd_uuid.getBytes().length % 4 == 0 ) ? osd_uuid.getBytes().length : ( osd_uuid.getBytes().length + 4 - osd_uuid.getBytes().length % 4 ) ) : 0 ); // osd_uuid
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "file_id", file_id );
        marshaller.writeString( "osd_uuid", osd_uuid );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        file_id = unmarshaller.readString( "file_id" );
        osd_uuid = unmarshaller.readString( "osd_uuid" );    
    }
        
    

    private String file_id;
    private String osd_uuid;    

}

