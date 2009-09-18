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




public class xtreemfs_get_suitable_osdsRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009090442;
    
    public xtreemfs_get_suitable_osdsRequest() {  }
    public xtreemfs_get_suitable_osdsRequest( String file_id, int numOSDs ) { this.file_id = file_id; this.numOSDs = numOSDs; }

    public String getFile_id() { return file_id; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }
    public int getNumOSDs() { return numOSDs; }
    public void setNumOSDs( int numOSDs ) { this.numOSDs = numOSDs; }

    // java.lang.Object
    public String toString() 
    { 
        StringWriter string_writer = new StringWriter();
        PrettyPrinter pretty_printer = new PrettyPrinter( string_writer );
        pretty_printer.writeStruct( "", this );
        return string_writer.toString();
    }

    // Request
    public Response createDefaultResponse() { return new xtreemfs_get_suitable_osdsResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009090442;    

    // yidl.Object
    public int getTag() { return 2009090442; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_get_suitable_osdsRequest"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8 + ( file_id != null ? ( ( file_id.getBytes().length % 4 == 0 ) ? file_id.getBytes().length : ( file_id.getBytes().length + 4 - file_id.getBytes().length % 4 ) ) : 0 ); // file_id
        my_size += Integer.SIZE / 8; // numOSDs
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "file_id", file_id );
        marshaller.writeUint32( "numOSDs", numOSDs );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        file_id = unmarshaller.readString( "file_id" );
        numOSDs = unmarshaller.readUint32( "numOSDs" );    
    }
        
    

    private String file_id;
    private int numOSDs;    

}

