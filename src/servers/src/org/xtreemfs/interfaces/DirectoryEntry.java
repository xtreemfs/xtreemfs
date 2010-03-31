package org.xtreemfs.interfaces;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class DirectoryEntry implements Struct
{
    public static final int TAG = 2010030958;

    public DirectoryEntry() { stbuf = new StatSet();  }
    public DirectoryEntry( String name, StatSet stbuf ) { this.name = name; this.stbuf = stbuf; }

    public String getName() { return name; }
    public StatSet getStbuf() { return stbuf; }
    public void setName( String name ) { this.name = name; }
    public void setStbuf( StatSet stbuf ) { this.stbuf = stbuf; }

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
    public static final long serialVersionUID = 2010030958;

    // yidl.runtime.Object
    public int getTag() { return 2010030958; }
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
        marshaller.writeSequence( "stbuf", stbuf );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        name = unmarshaller.readString( "name" );
        stbuf = new StatSet(); unmarshaller.readSequence( "stbuf", stbuf );
    }

    private String name;
    private StatSet stbuf;
}
