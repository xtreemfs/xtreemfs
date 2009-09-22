package org.xtreemfs.interfaces;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.Marshaller;
import yidl.runtime.PrettyPrinter;
import yidl.runtime.Struct;
import yidl.runtime.Unmarshaller;




public class Lock implements Struct
{
    public static final int TAG = 2009082672;
    
    public Lock() {  }
    public Lock( String client_uuid, int client_pid, long offset, long length ) { this.client_uuid = client_uuid; this.client_pid = client_pid; this.offset = offset; this.length = length; }

    public String getClient_uuid() { return client_uuid; }
    public void setClient_uuid( String client_uuid ) { this.client_uuid = client_uuid; }
    public int getClient_pid() { return client_pid; }
    public void setClient_pid( int client_pid ) { this.client_pid = client_pid; }
    public long getOffset() { return offset; }
    public void setOffset( long offset ) { this.offset = offset; }
    public long getLength() { return length; }
    public void setLength( long length ) { this.length = length; }

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
    public static final long serialVersionUID = 2009082672;    

    // yidl.runtime.Object
    public int getTag() { return 2009082672; }
    public String getTypeName() { return "org::xtreemfs::interfaces::Lock"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8 + ( client_uuid != null ? ( ( client_uuid.getBytes().length % 4 == 0 ) ? client_uuid.getBytes().length : ( client_uuid.getBytes().length + 4 - client_uuid.getBytes().length % 4 ) ) : 0 ); // client_uuid
        my_size += Integer.SIZE / 8; // client_pid
        my_size += Long.SIZE / 8; // offset
        my_size += Long.SIZE / 8; // length
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeString( "client_uuid", client_uuid );
        marshaller.writeUint32( "client_pid", client_pid );
        marshaller.writeUint64( "offset", offset );
        marshaller.writeUint64( "length", length );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        client_uuid = unmarshaller.readString( "client_uuid" );
        client_pid = unmarshaller.readUint32( "client_pid" );
        offset = unmarshaller.readUint64( "offset" );
        length = unmarshaller.readUint64( "length" );    
    }
        
    

    private String client_uuid;
    private int client_pid;
    private long offset;
    private long length;    

}

