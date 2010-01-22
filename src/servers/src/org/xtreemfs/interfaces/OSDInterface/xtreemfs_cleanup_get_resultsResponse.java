package org.xtreemfs.interfaces.OSDInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class xtreemfs_cleanup_get_resultsResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2010012443;
    
    public xtreemfs_cleanup_get_resultsResponse() { results = new StringSet();  }
    public xtreemfs_cleanup_get_resultsResponse( StringSet results ) { this.results = results; }

    public StringSet getResults() { return results; }
    public void setResults( StringSet results ) { this.results = results; }

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
    public static final long serialVersionUID = 2010012443;    

    // yidl.runtime.Object
    public int getTag() { return 2010012443; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_cleanup_get_resultsResponse"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += results.getXDRSize(); // results
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeSequence( "results", results );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        results = new StringSet(); unmarshaller.readSequence( "results", results );    
    }
        
    

    private StringSet results;    

}

