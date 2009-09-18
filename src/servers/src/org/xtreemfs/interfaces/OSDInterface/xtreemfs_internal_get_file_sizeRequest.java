package org.xtreemfs.interfaces.OSDInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.PrettyPrinter;
import yidl.Struct;
import yidl.Unmarshaller;




public class xtreemfs_internal_get_file_sizeRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009082960;
    
    public xtreemfs_internal_get_file_sizeRequest() { file_credentials = new FileCredentials();  }
    public xtreemfs_internal_get_file_sizeRequest( FileCredentials file_credentials, String file_id ) { this.file_credentials = file_credentials; this.file_id = file_id; }

    public FileCredentials getFile_credentials() { return file_credentials; }
    public void setFile_credentials( FileCredentials file_credentials ) { this.file_credentials = file_credentials; }
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
    public Response createDefaultResponse() { return new xtreemfs_internal_get_file_sizeResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009082960;    

    // yidl.Object
    public int getTag() { return 2009082960; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_internal_get_file_sizeRequest"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += file_credentials.getXDRSize(); // file_credentials
        my_size += Integer.SIZE / 8 + ( file_id != null ? ( ( file_id.getBytes().length % 4 == 0 ) ? file_id.getBytes().length : ( file_id.getBytes().length + 4 - file_id.getBytes().length % 4 ) ) : 0 ); // file_id
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeStruct( "file_credentials", file_credentials );
        marshaller.writeString( "file_id", file_id );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        file_credentials = new FileCredentials(); unmarshaller.readStruct( "file_credentials", file_credentials );
        file_id = unmarshaller.readString( "file_id" );    
    }
        
    

    private FileCredentials file_credentials;
    private String file_id;    

}

