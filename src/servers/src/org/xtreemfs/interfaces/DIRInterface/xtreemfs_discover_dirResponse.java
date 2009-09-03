package org.xtreemfs.interfaces.DIRInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class xtreemfs_discover_dirResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2009082723;
    
    public xtreemfs_discover_dirResponse() { dir_service = new DirService();  }
    public xtreemfs_discover_dirResponse( DirService dir_service ) { this.dir_service = dir_service; }

    public DirService getDir_service() { return dir_service; }
    public void setDir_service( DirService dir_service ) { this.dir_service = dir_service; }

    // java.io.Serializable
    public static final long serialVersionUID = 2009082723;    

    // yidl.Object
    public int getTag() { return 2009082723; }
    public String getTypeName() { return "org::xtreemfs::interfaces::DIRInterface::xtreemfs_discover_dirResponse"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += dir_service.getXDRSize();
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeStruct( "dir_service", dir_service );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        dir_service = new DirService(); unmarshaller.readStruct( "dir_service", dir_service );    
    }
        
    

    private DirService dir_service;    

}

