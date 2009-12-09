package org.xtreemfs.interfaces.MRCInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class readdirResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2009121122;
    
    public readdirResponse() { directory_entries = new DirectoryEntrySet();  }
    public readdirResponse( DirectoryEntrySet directory_entries ) { this.directory_entries = directory_entries; }

    public DirectoryEntrySet getDirectory_entries() { return directory_entries; }
    public void setDirectory_entries( DirectoryEntrySet directory_entries ) { this.directory_entries = directory_entries; }

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
    public static final long serialVersionUID = 2009121122;    

    // yidl.runtime.Object
    public int getTag() { return 2009121122; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::readdirResponse"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += directory_entries.getXDRSize(); // directory_entries
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeSequence( "directory_entries", directory_entries );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        directory_entries = new DirectoryEntrySet(); unmarshaller.readSequence( "directory_entries", directory_entries );    
    }
        
    

    private DirectoryEntrySet directory_entries;    

}

