package org.xtreemfs.interfaces.NettestInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.PrettyPrinter;




public class NettestInterface
{
    public static long getProg() { return 2546883425l; }
    public static int getVersion() { return 2010012513; }

    public static Request createRequest( ONCRPCRequestHeader header ) throws Exception
    {
        switch( header.getProcedure() )
        {
            case 2010012514: return new nopRequest();
            case 2010012515: return new send_bufferRequest();
            case 2010012516: return new recv_bufferRequest();

            default: throw new Exception( "unknown request tag " + Integer.toString( header.getProcedure() ) );
        }
    }
            
    public static Response createResponse( ONCRPCResponseHeader header ) throws Exception
    {
        switch( header.getXID() )
        {
            case 2010012514: return new nopResponse();            case 2010012515: return new send_bufferResponse();            case 2010012516: return new recv_bufferResponse();
            default: throw new Exception( "unknown response XID " + Integer.toString( header.getXID() ) );
        }
    }    

}
