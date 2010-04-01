package org.xtreemfs.interfaces.NettestInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.oncrpc.utils.*;
import org.xtreemfs.interfaces.*;
import yidl.runtime.PrettyPrinter;




public class NettestInterface
{
    public static long getProg() { return 2546902228l; }
    public static int getVersion() { return 2010031316; }

    public static Request createRequest( ONCRPCRequestHeader header ) throws Exception
    {
        switch( header.getProcedure() )
        {
            case 2010031317: return new nopRequest();
            case 2010031318: return new send_bufferRequest();
            case 2010031319: return new recv_bufferRequest();
            default: throw new Exception( "unknown request tag " + Integer.toString( header.getProcedure() ) );
        }
    }

    public static Response createResponse( ONCRPCResponseHeader header ) throws Exception
    {
        switch( header.getXID() )
        {
            case 2010031317: return new nopResponse();
            case 2010031318: return new send_bufferResponse();
            case 2010031319: return new recv_bufferResponse();
            default: throw new Exception( "unknown response XID " + Integer.toString( header.getXID() ) );
        }
    }
}