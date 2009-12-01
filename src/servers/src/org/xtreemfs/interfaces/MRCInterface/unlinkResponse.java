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




public class unlinkResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2009120331;
    
    public unlinkResponse() { file_credentials = new FileCredentialsSet();  }
    public unlinkResponse( FileCredentialsSet file_credentials ) { this.file_credentials = file_credentials; }

    public FileCredentialsSet getFile_credentials() { return file_credentials; }
    public void setFile_credentials( FileCredentialsSet file_credentials ) { this.file_credentials = file_credentials; }

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
    public static final long serialVersionUID = 2009120331;    

    // yidl.runtime.Object
    public int getTag() { return 2009120331; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::unlinkResponse"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += file_credentials.getXDRSize(); // file_credentials
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeSequence( "file_credentials", file_credentials );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        file_credentials = new FileCredentialsSet(); unmarshaller.readSequence( "file_credentials", file_credentials );    
    }
        
    

    private FileCredentialsSet file_credentials;    

}

