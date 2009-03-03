package org.xtreemfs.interfaces.DIRInterface;

import org.xtreemfs.interfaces.utils.ONCRPCRequestHeader;
import org.xtreemfs.interfaces.utils.ONCRPCResponseHeader;
import org.xtreemfs.interfaces.utils.Request;
import org.xtreemfs.interfaces.utils.Response;
import org.xtreemfs.interfaces.utils.ONCRPCException;
import org.xtreemfs.interfaces.Exceptions.*;


public class DIRInterface
{
    public static int getVersion() { return 1; }

    public static Request createRequest( ONCRPCRequestHeader header ) throws Exception
    {
        switch( header.getOperationNumber() )
        {
            case 1: return new address_mappings_getRequest();
            case 2: return new address_mappings_setRequest();
            case 3: return new address_mappings_deleteRequest();
            case 4: return new service_registerRequest();
            case 5: return new service_deregisterRequest();
            case 6: return new service_get_by_typeRequest();
            case 7: return new service_get_by_uuidRequest();
            case 8: return new global_time_getRequest();
            case 50: return new admin_checkpointRequest();
            case 51: return new admin_shutdownRequest();
            default: throw new Exception( "unknown request number " + Integer.toString( header.getOperationNumber() ) );
        }
    }
            
    public static Response createResponse( ONCRPCResponseHeader header ) throws Exception
    {
        switch( header.getXID() )
        {
            case 1: return new address_mappings_getResponse();
            case 2: return new address_mappings_setResponse();
            case 3: return new address_mappings_deleteResponse();
            case 4: return new service_registerResponse();
            case 5: return new service_deregisterResponse();
            case 6: return new service_get_by_typeResponse();
            case 7: return new service_get_by_uuidResponse();
            case 8: return new global_time_getResponse();
            case 50: return new admin_checkpointResponse();
            case 51: return new admin_shutdownResponse();
            default: throw new Exception( "unknown response number " + Integer.toString( header.getXID() ) );
        }
    }    
}
