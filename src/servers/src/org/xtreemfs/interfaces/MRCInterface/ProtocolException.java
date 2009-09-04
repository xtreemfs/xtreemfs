package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.Marshaller;
import yidl.Struct;
import yidl.Unmarshaller;




public class ProtocolException extends org.xtreemfs.interfaces.utils.ONCRPCException
{
    public static final int TAG = 2009090463;
    
    public ProtocolException() {  }
    public ProtocolException( int accept_stat, int error_code, String stack_trace ) { this.accept_stat = accept_stat; this.error_code = error_code; this.stack_trace = stack_trace; }

    public int getAccept_stat() { return accept_stat; }
    public void setAccept_stat( int accept_stat ) { this.accept_stat = accept_stat; }
    public int getError_code() { return error_code; }
    public void setError_code( int error_code ) { this.error_code = error_code; }
    public String getStack_trace() { return stack_trace; }
    public void setStack_trace( String stack_trace ) { this.stack_trace = stack_trace; }

    // java.io.Serializable
    public static final long serialVersionUID = 2009090463;    

    // yidl.Object
    public int getTag() { return 2009090463; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::ProtocolException"; }
    
    public int getXDRSize()
    {
        int my_size = 0;
        my_size += Integer.SIZE / 8; // accept_stat
        my_size += Integer.SIZE / 8; // error_code
        my_size += Integer.SIZE / 8 + ( stack_trace != null ? ( ( stack_trace.getBytes().length % 4 == 0 ) ? stack_trace.getBytes().length : ( stack_trace.getBytes().length + 4 - stack_trace.getBytes().length % 4 ) ) : 0 ); // stack_trace
        return my_size;
    }    
    
    public void marshal( Marshaller marshaller )
    {
        marshaller.writeUint32( "accept_stat", accept_stat );
        marshaller.writeUint32( "error_code", error_code );
        marshaller.writeString( "stack_trace", stack_trace );
    }
    
    public void unmarshal( Unmarshaller unmarshaller ) 
    {
        accept_stat = unmarshaller.readUint32( "accept_stat" );
        error_code = unmarshaller.readUint32( "error_code" );
        stack_trace = unmarshaller.readString( "stack_trace" );    
    }
        
    

    private int accept_stat;
    private int error_code;
    private String stack_trace;    

}

