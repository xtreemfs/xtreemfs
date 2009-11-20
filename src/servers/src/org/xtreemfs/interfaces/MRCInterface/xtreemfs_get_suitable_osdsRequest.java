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




public class xtreemfs_get_suitable_osdsRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009112247;
    
    public xtreemfs_get_suitable_osdsRequest() {  }
    public xtreemfs_get_suitable_osdsRequest( String file_id, int num_osds ) { this.file_id = file_id; this.num_osds = num_osds; }

    public String getFile_id() { return file_id; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }
    public int getNum_osds() { return num_osds; }
    public void setNum_osds( int num_osds ) { this.num_osds = num_osds; }

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
    public Response createDefaultResponse() { return new xtreemfs_get_suitable_osdsResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009112247;    

    // yidl.runtime.Object
    public int getTag() { return 2009112247; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_get_suitable_osdsRequest"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8 + ( file_id != null ? ( ( file_id.getBytes().length % 4 == 0 ) ? file_id.getBytes().length : ( file_id.getBytes().length + 4 - file_id.getBytes().length % 4 ) ) : 0 ); // file_id
        my_size += Integer.SIZE / 8; // num_osds
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "file_id", file_id );
        marshaller.writeUint32( "num_osds", num_osds );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        file_id = unmarshaller.readString( "file_id" );
        num_osds = unmarshaller.readUint32( "num_osds" );    
    }
        
    

    private String file_id;
    private int num_osds;    

}

