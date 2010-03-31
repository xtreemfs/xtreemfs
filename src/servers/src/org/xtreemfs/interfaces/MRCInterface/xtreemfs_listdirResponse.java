package org.xtreemfs.interfaces.MRCInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class xtreemfs_listdirResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2010022450;

    public xtreemfs_listdirResponse() { names = new StringSet();  }
    public xtreemfs_listdirResponse( StringSet names ) { this.names = names; }

    public StringSet getNames() { return names; }
    public void setNames( StringSet names ) { this.names = names; }

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
    public static final long serialVersionUID = 2010022450;

    // yidl.runtime.Object
    public int getTag() { return 2010022450; }
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

