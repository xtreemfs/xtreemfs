package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class xtreemfs_check_file_existsResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2009090440;
    
    public xtreemfs_check_file_existsResponse() {  }
    public xtreemfs_check_file_existsResponse( String bitmap ) { this.bitmap = bitmap; }

    public String getBitmap() { return bitmap; }
    public void setBitmap( String bitmap ) { this.bitmap = bitmap; }

    // java.io.Serializable
    public static final long serialVersionUID = 2009090440;    

    // yidl.Object
    public int getTag() { return 2009090440; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_check_file_existsResponse"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE/8 + ( bitmap != null ? ( ( bitmap.getBytes().length % 4 == 0 ) ? bitmap.getBytes().length : ( bitmap.getBytes().length + 4 - bitmap.getBytes().length % 4 ) ) : 0 );
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "bitmap", bitmap );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        bitmap = unmarshaller.readString( "bitmap" );    
    }
        
    

    private String bitmap;    

}

