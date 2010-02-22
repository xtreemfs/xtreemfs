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
    public static final int TAG = 2010022426;

    public readdirResponse() { more_directory_entries = new DirectoryEntrySet();  }
    public readdirResponse( DirectoryEntrySet more_directory_entries ) { this.more_directory_entries = more_directory_entries; }

    public DirectoryEntrySet getMore_directory_entries() { return more_directory_entries; }
    public void setMore_directory_entries( DirectoryEntrySet more_directory_entries ) { this.more_directory_entries = more_directory_entries; }

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
    public static final long serialVersionUID = 2010022426;

    // yidl.runtime.Object
    public int getTag() { return 2010022426; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::readdirResponse"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += more_directory_entries.getXDRSize(); // more_directory_entries
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeSequence( "more_directory_entries", more_directory_entries );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        more_directory_entries = new DirectoryEntrySet(); unmarshaller.readSequence( "more_directory_entries", more_directory_entries );
    }

    

    private DirectoryEntrySet more_directory_entries;

}

