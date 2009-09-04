package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class xtreemfs_listdirResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2009082854;
    
    public xtreemfs_listdirResponse() { names = new StringSet();  }
    public xtreemfs_listdirResponse( StringSet names ) { this.names = names; }

    public StringSet getNames() { return names; }
    public void setNames( StringSet names ) { this.names = names; }

    // java.io.Serializable
    public static final long serialVersionUID = 2009082854;    

    // yidl.Object
    public int getTag() { return 2009082854; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_listdirResponse"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += names.getXDRSize(); // names
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeSequence( "names", names );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        names = new StringSet(); unmarshaller.readSequence( "names", names );    
    }
        
    

    private StringSet names;    

}

