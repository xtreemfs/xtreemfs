package org.xtreemfs.interfaces.NettestInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.PrettyPrinter;




public class NettestInterface
{
    public static long getProg() { return 2546902228l; }
    public static int getVersion() { return 2010031316; }

    public static Request createRequest( ONCRPCRequestHeader header ) throws Exception
    {
        switch( header.getProcedure() )
        {
            case 2010031317: return new nopRequestRequest();
            case 2010031318: return new send_bufferRequestRequest();
            case 2010031319: return new recv_bufferRequestRequest();
            default: throw new Exception( "unknown request tag " + Integer.toString( header.getProcedure() ) );
        }
    }

    public static Response createResponse( ONCRPCResponseHeader header ) throws Exception
    {
        switch( header.getXID() )
        {
            case 2010031317: return new nopResponseResponse();case 2010031318: return new send_bufferResponseResponse();case 2010031319: return new recv_bufferResponseResponse();
            default: throw new Exception( "unknown response XID " + Integer.toString( header.getXID() ) );
        }
    }

}
