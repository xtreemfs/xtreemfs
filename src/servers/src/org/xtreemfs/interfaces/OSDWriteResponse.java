package org.xtreemfs.interfaces;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class OSDWriteResponse implements Struct
{
    public static final int TAG = 2009120927;
    
    public OSDWriteResponse() { new_file_size = new NewFileSizeSet(); opaque_data = new OSDtoMRCDataSet();  }
    public OSDWriteResponse( NewFileSizeSet new_file_size, OSDtoMRCDataSet opaque_data ) { this.new_file_size = new_file_size; this.opaque_data = opaque_data; }

    public NewFileSizeSet getNew_file_size() { return new_file_size; }
    public void setNew_file_size( NewFileSizeSet new_file_size ) { this.new_file_size = new_file_size; }
    public OSDtoMRCDataSet getOpaque_data() { return opaque_data; }
    public void setOpaque_data( OSDtoMRCDataSet opaque_data ) { this.opaque_data = opaque_data; }

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
    public static final long serialVersionUID = 2009120927;    

    // yidl.runtime.Object
    public int getTag() { return 2009120927; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDWriteResponse"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += new_file_size.getXDRSize(); // new_file_size
        my_size += opaque_data.getXDRSize(); // opaque_data
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeSequence( "new_file_size", new_file_size );
        marshaller.writeSequence( "opaque_data", opaque_data );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        new_file_size = new NewFileSizeSet(); unmarshaller.readSequence( "new_file_size", new_file_size );
        opaque_data = new OSDtoMRCDataSet(); unmarshaller.readSequence( "opaque_data", opaque_data );    
    }
        
    

    private NewFileSizeSet new_file_size;
    private OSDtoMRCDataSet opaque_data;    

}

