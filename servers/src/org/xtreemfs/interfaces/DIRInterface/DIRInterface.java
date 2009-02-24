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
            case 2: return new setAddressMappingsRequest();
            case 3: return new registerServiceRequest();
            case 4: return new deregisterServiceRequest();
            case 5: return new getServicesRequest();
            case 6: return new getGlobalTimeRequest();
            default: throw new Exception( "unknown request number " + Integer.toString( header.getOperationNumber() ) );
        }
    }
            
    public static Response createResponse( ONCRPCResponseHeader header ) throws Exception
    {
        switch( header.getXID() )
        {
            case 1: return new getAddressMappingsResponse();
            case 2: return new setAddressMappingsResponse();
            case 3: return new registerServiceResponse();
            case 4: return new deregisterServiceResponse();
            case 5: return new getServicesResponse();
            case 6: return new getGlobalTimeResponse();
            default: throw new Exception( "unknown response number " + Integer.toString( header.getXID() ) );
        }
    }    
}
