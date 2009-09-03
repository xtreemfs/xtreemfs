package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class renameRequest extends org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009090423;
    
    public renameRequest() {  }
    public renameRequest( String source_path, String target_path ) { this.source_path = source_path; this.target_path = target_path; }

    public String getSource_path() { return source_path; }
    public void setSource_path( String source_path ) { this.source_path = source_path; }
    public String getTarget_path() { return target_path; }
    public void setTarget_path( String target_path ) { this.target_path = target_path; }

    // Request
    public Response createDefaultResponse() { return new renameResponse(); }


    // java.io.Serializable
    public static final long serialVersionUID = 2009090423;    

    // yidl.Object
    public int getTag() { return 2009090423; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::renameRequest"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += ( ( source_path.getBytes().length + Integer.SIZE/8 ) % 4 == 0 ) ? ( source_path.getBytes().length + Integer.SIZE/8 ) : ( source_path.getBytes().length + Integer.SIZE/8 + 4 - ( source_path.getBytes().length + Integer.SIZE/8 ) % 4 );
        my_size += ( ( target_path.getBytes().length + Integer.SIZE/8 ) % 4 == 0 ) ? ( target_path.getBytes().length + Integer.SIZE/8 ) : ( target_path.getBytes().length + Integer.SIZE/8 + 4 - ( target_path.getBytes().length + Integer.SIZE/8 ) % 4 );
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "source_path", source_path );
        marshaller.writeString( "target_path", target_path );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        source_path = unmarshaller.readString( "source_path" );
        target_path = unmarshaller.readString( "target_path" );    
    }
        
    

    private String source_path;
    private String target_path;    

}

