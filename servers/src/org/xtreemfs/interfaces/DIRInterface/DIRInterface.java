package org.xtreemfs.interfaces.DIRInterface;

import org.xtreemfs.interfaces.utils.ONCRPCRequestHeader;
import org.xtreemfs.interfaces.utils.ONCRPCResponseHeader;
import org.xtreemfs.interfaces.utils.Request;
import org.xtreemfs.interfaces.utils.Response;
import org.xtreemfs.interfaces.Exceptions.*;


public class DIRInterface
{
    public static int getVersion() { return 1; }

    public static Request createRequest( ONCRPCRequestHeader header ) throws Exception
    {
        switch( header.getOperationNumber() )
        {
            case 1: return new getAddressMappingsRequest();
            case 2: return new getGlobalTimeRequest();
            default: throw new Exception( "unknown request number " + Integer.toString( header.getOperationNumber() ) );
        }
    }
            
    public static Response createResponse( ONCRPCResponseHeader header ) throws Exception
    {
        switch( header.getXID() )
        {
            case 1: return new getAddressMappingsResponse();
            case 2: return new getGlobalTimeResponse();
            default: throw new Exception( "unknown response number " + Integer.toString( header.getXID() ) );
        }
    }    
}
