package org.xtreemfs.interfaces.Exceptions;

import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.Exceptions.*;

import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         
public class ProtocolException extends org.xtreemfs.interfaces.utils.ONCRPCException 
{
    public ProtocolException() { accept_stat = 0; error_code = 0; stack_trace = ""; }
    public ProtocolException( int accept_stat, int error_code, String stack_trace ) { this.accept_stat = accept_stat; this.error_code = error_code; this.stack_trace = stack_trace; }

    public int getAccept_stat() { return accept_stat; }
    public void setAccept_stat( int accept_stat ) { this.accept_stat = accept_stat; }
    public int getError_code() { return error_code; }
    public void setError_code( int error_code ) { this.error_code = error_code; }
    public String getStack_trace() { return stack_trace; }
    public void setStack_trace( String stack_trace ) { this.stack_trace = stack_trace; }

    // Object
    public String toString()
    {
        return "ProtocolException( " + Integer.toString( accept_stat ) + ", " + Integer.toString( error_code ) + ", " + "\"" + stack_trace + "\"" + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::Exceptions::ProtocolException"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        writer.putInt( accept_stat );
        writer.putInt( error_code );
        { final byte[] bytes = stack_trace.getBytes(); writer.putInt( bytes.length ); writer.put( bytes );  if (bytes.length % 4 > 0) {for (int k = 0; k < (4 - (bytes.length % 4)); k++) { writer.put((byte)0); } }}        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        accept_stat = buf.getInt();
        error_code = buf.getInt();
        { int stack_trace_new_length = buf.getInt(); byte[] stack_trace_new_bytes = new byte[stack_trace_new_length]; buf.get( stack_trace_new_bytes ); stack_trace = new String( stack_trace_new_bytes ); if (stack_trace_new_length % 4 > 0) {for (int k = 0; k < (4 - (stack_trace_new_length % 4)); k++) { buf.get(); } } }    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += ( Integer.SIZE / 8 );
        my_size += ( Integer.SIZE / 8 );
        my_size += 4 + ( stack_trace.length() + 4 - ( stack_trace.length() % 4 ) );
        return my_size;
    }

    private int accept_stat;
    private int error_code;
    private String stack_trace;
    
}

