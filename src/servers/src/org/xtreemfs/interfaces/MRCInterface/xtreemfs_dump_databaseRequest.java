package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class xtreemfs_dump_databaseRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009082850;
    
    public xtreemfs_dump_databaseRequest() {  }
    public xtreemfs_dump_databaseRequest( String dump_file ) { this.dump_file = dump_file; }

    public String getDump_file() { return dump_file; }
    public void setDump_file( String dump_file ) { this.dump_file = dump_file; }

    // Request
    public Response createDefaultResponse() { return new xtreemfs_dump_databaseResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009082850;    

    // yidl.Object
    public int getTag() { return 2009082850; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_dump_databaseRequest"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8 + ( dump_file != null ? ( ( dump_file.getBytes().length % 4 == 0 ) ? dump_file.getBytes().length : ( dump_file.getBytes().length + 4 - dump_file.getBytes().length % 4 ) ) : 0 ); // dump_file
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "dump_file", dump_file );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        dump_file = unmarshaller.readString( "dump_file" );    
    }
        
    

    private String dump_file;    

}

