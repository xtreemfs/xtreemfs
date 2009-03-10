package org.xtreemfs.interfaces.Exceptions;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class OSDException extends org.xtreemfs.interfaces.utils.ONCRPCException
{
    public OSDException() { error_code = 0; errro_message = ""; stack_trace = ""; }
    public OSDException( int error_code, String errro_message, String stack_trace ) { this.error_code = error_code; this.errro_message = errro_message; this.stack_trace = stack_trace; }
    public OSDException( Object from_hash_map ) { error_code = 0; errro_message = ""; stack_trace = ""; this.deserialize( from_hash_map ); }
    public OSDException( Object[] from_array ) { error_code = 0; errro_message = ""; stack_trace = "";this.deserialize( from_array ); }

    public int getError_code() { return error_code; }
    public void setError_code( int error_code ) { this.error_code = error_code; }
    public String getErrro_message() { return errro_message; }
    public void setErrro_message( String errro_message ) { this.errro_message = errro_message; }
    public String getStack_trace() { return stack_trace; }
    public void setStack_trace( String stack_trace ) { this.stack_trace = stack_trace; }

    public String getTypeName() { return "org::xtreemfs::interfaces::Exceptions::OSDException"; }    
    public long getTypeId() { return 0; }

    public String toString()
    {
        return "OSDException( " + Integer.toString( error_code ) + ", " + "\"" + errro_message + "\"" + ", " + "\"" + stack_trace + "\"" + " )"; 
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.error_code = ( ( Integer )from_hash_map.get( "error_code" ) ).intValue();
        this.errro_message = ( String )from_hash_map.get( "errro_message" );
        this.stack_trace = ( String )from_hash_map.get( "stack_trace" );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.error_code = ( ( Integer )from_array[0] ).intValue();
        this.errro_message = ( String )from_array[1];
        this.stack_trace = ( String )from_array[2];        
    }

    public void deserialize( ReusableBuffer buf )
    {
        error_code = buf.getInt();
        errro_message = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        stack_trace = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "error_code", new Integer( error_code ) );
        to_hash_map.put( "errro_message", errro_message );
        to_hash_map.put( "stack_trace", stack_trace );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        writer.putInt( error_code );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( errro_message, writer );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( stack_trace, writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += ( Integer.SIZE / 8 );
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(errro_message);
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(stack_trace);
        return my_size;
    }


    private int error_code;
    private String errro_message;
    private String stack_trace;

}

