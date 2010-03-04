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




public class openResponse extends org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2010030523;

    public openResponse() { file_credentials = new FileCredentials();  }
    public openResponse( FileCredentials file_credentials ) { this.file_credentials = file_credentials; }

    public FileCredentials getFile_credentials() { return file_credentials; }
    public void setFile_credentials( FileCredentials file_credentials ) { this.file_credentials = file_credentials; }

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
    public static final long serialVersionUID = 2010030523;

    // yidl.runtime.Object
    public int getTag() { return 2010030523; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::openResponse"; }

    public int getXDRSize()
    {
        int my_size = 0;
        my_size += file_credentials.getXDRSize(); // file_credentials
        return my_size;
    }

    public void marshal( Marshaller marshaller )
    {
        marshaller.writeStruct( "file_credentials", file_credentials );
    }

    public void unmarshal( Unmarshaller unmarshaller )
    {
        file_credentials = new FileCredentials(); unmarshaller.readStruct( "file_credentials", file_credentials );
    }

    

    private FileCredentials file_credentials;

}

