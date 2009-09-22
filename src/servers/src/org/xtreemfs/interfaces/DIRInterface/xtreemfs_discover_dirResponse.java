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




public class xtreemfs_discover_dirResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2009082723;
    
    public xtreemfs_discover_dirResponse() { dir_service = new DirService();  }
    public xtreemfs_discover_dirResponse( DirService dir_service ) { this.dir_service = dir_service; }

    public DirService getDir_service() { return dir_service; }
    public void setDir_service( DirService dir_service ) { this.dir_service = dir_service; }

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


    // java.io.Serializable
    public static final long serialVersionUID = 2009082723;    

    // yidl.runtime.Object
    public int getTag() { return 2009082723; }
    public String getTypeName() { return "org::xtreemfs::interfaces::DIRInterface::xtreemfs_discover_dirResponse"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += dir_service.getXDRSize(); // dir_service
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

