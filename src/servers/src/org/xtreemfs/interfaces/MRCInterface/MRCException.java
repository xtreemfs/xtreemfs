package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class MRCException extends org.xtreemfs.interfaces.utils.ONCRPCException
{
    public MRCException() { error_code = 0; error_message = ""; stack_trace = ""; }
    public MRCException( int error_code, String error_message, String stack_trace ) { this.error_code = error_code; this.error_message = error_message; this.stack_trace = stack_trace; }
    public MRCException( Object from_hash_map ) { error_code = 0; error_message = ""; stack_trace = ""; this.deserialize( from_hash_map ); }
    public MRCException( Object[] from_array ) { error_code = 0; error_message = ""; stack_trace = "";this.deserialize( from_array ); }

    public int getError_code() { return error_code; }
    public void setError_code( int error_code ) { this.error_code = error_code; }
    public String getError_message() { return error_message; }
    public void setError_message( String error_message ) { this.error_message = error_message; }
    public String getStack_trace() { return stack_trace; }
    public void setStack_trace( String stack_trace ) { this.stack_trace = stack_trace; }

    public long getTag() { return 1211; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::MRCException"; }

    public String toString()
    {
        return "MRCException( " + Integer.toString( error_code ) + ", " + "\"" + error_message + "\"" + ", " + "\"" + stack_trace + "\"" + " )";
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.error_code = ( ( Integer )from_hash_map.get( "error_code" ) ).intValue();
        this.error_message = ( String )from_hash_map.get( "error_message" );
        this.stack_trace = ( String )from_hash_map.get( "stack_trace" );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.error_code = ( ( Integer )from_array[0] ).intValue();
        this.error_message = ( String )from_array[1];
        this.stack_trace = ( String )from_array[2];        
    }

    public void deserialize( ReusableBuffer buf )
    {
        error_code = buf.getInt();
        error_message = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        stack_trace = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "error_code", new Integer( error_code ) );
        to_hash_map.put( "error_message", error_message );
        to_hash_map.put( "stack_trace", stack_trace );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        writer.putInt( error_code );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( error_message, writer );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( stack_trace, writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += ( Integer.SIZE / 8 );
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(error_message);
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(stack_trace);
        return my_size;
    }


    private int error_code;
    private String error_message;
    private String stack_trace;    

}

