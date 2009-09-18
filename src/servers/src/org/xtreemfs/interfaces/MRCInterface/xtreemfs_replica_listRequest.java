package org.xtreemfs.interfaces.MRCInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.PrettyPrinter;
import yidl.Struct;
import yidl.Unmarshaller;




public class xtreemfs_replica_listRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009090449;
    
    public xtreemfs_replica_listRequest() {  }
    public xtreemfs_replica_listRequest( String file_id ) { this.file_id = file_id; }

    public String getFile_id() { return file_id; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }

    // java.lang.Object
    public String toString() 
    { 
        StringWriter string_writer = new StringWriter();
        PrettyPrinter pretty_printer = new PrettyPrinter( string_writer );
        pretty_printer.writeStruct( "", this );
        return string_writer.toString();
    }

    // Request
    public Response createDefaultResponse() { return new xtreemfs_replica_listResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009090449;    

    // yidl.Object
    public int getTag() { return 2009090449; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_replica_listRequest"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8 + ( file_id != null ? ( ( file_id.getBytes().length % 4 == 0 ) ? file_id.getBytes().length : ( file_id.getBytes().length + 4 - file_id.getBytes().length % 4 ) ) : 0 ); // file_id
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "file_id", file_id );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        file_id = unmarshaller.readString( "file_id" );    
    }
        
    

    private String file_id;    

}

