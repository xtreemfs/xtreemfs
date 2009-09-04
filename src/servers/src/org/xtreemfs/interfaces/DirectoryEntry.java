package org.xtreemfs.interfaces;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class DirectoryEntry extends Struct
{
    public static final int TAG = 2009090250;
    
    public DirectoryEntry() { stbuf = new Stat();  }
    public DirectoryEntry( String name, Stat stbuf ) { this.name = name; this.stbuf = stbuf; }

    public String getName() { return name; }
    public void setName( String name ) { this.name = name; }
    public Stat getStbuf() { return stbuf; }
    public void setStbuf( Stat stbuf ) { this.stbuf = stbuf; }

    // java.io.Serializable
    public static final long serialVersionUID = 2009090250;    

    // yidl.Object
    public int getTag() { return 2009090250; }
    public String getTypeName() { return "org::xtreemfs::interfaces::DirectoryEntry"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8 + ( name != null ? ( ( name.getBytes().length % 4 == 0 ) ? name.getBytes().length : ( name.getBytes().length + 4 - name.getBytes().length % 4 ) ) : 0 ); // name
        my_size += stbuf.getXDRSize(); // stbuf
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "name", name );
        marshaller.writeStruct( "stbuf", stbuf );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        name = unmarshaller.readString( "name" );
        stbuf = new Stat(); unmarshaller.readStruct( "stbuf", stbuf );    
    }
        
    

    private String name;
    private Stat stbuf;    

}

