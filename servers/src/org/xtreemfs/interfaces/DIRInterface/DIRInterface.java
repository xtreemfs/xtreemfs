package org.xtreemfs.interfaces.DIRInterface;

import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.Exceptions.*;




public class DIRInterface
{        
    public static int getVersion() { return 1; }

    public static Request createRequest( ONCRPCRequestHeader header ) throws Exception
    {
        switch( header.getOperationNumber() )
        {
            case 1: return new xtreemfs_address_mappings_getRequest();
            case 3: return new xtreemfs_address_mappings_removeRequest();
            case 2: return new xtreemfs_address_mappings_setRequest();
            case 50: return new xtreemfs_checkpointRequest();
            case 8: return new xtreemfs_global_time_s_getRequest();
            case 6: return new xtreemfs_service_get_by_typeRequest();
            case 7: return new xtreemfs_service_get_by_uuidRequest();
            case 9: return new xtreemfs_service_get_by_nameRequest();
            case 4: return new xtreemfs_service_registerRequest();
            case 5: return new xtreemfs_service_deregisterRequest();
            case 10: return new xtreemfs_service_offlineRequest();
            case 51: return new xtreemfs_shutdownRequest();

            default: throw new Exception( "unknown request number " + Integer.toString( header.getOperationNumber() ) );
        }
    }
            
    public static Response createResponse( ONCRPCResponseHeader header ) throws Exception
    {
        switch( header.getXID() )
        {
            case 1: return new xtreemfs_address_mappings_getResponse();            case 3: return new xtreemfs_address_mappings_removeResponse();            case 2: return new xtreemfs_address_mappings_setResponse();            case 50: return new xtreemfs_checkpointResponse();            case 8: return new xtreemfs_global_time_s_getResponse();            case 6: return new xtreemfs_service_get_by_typeResponse();            case 7: return new xtreemfs_service_get_by_uuidResponse();            case 9: return new xtreemfs_service_get_by_nameResponse();            case 4: return new xtreemfs_service_registerResponse();            case 5: return new xtreemfs_service_deregisterResponse();            case 10: return new xtreemfs_service_offlineResponse();            case 51: return new xtreemfs_shutdownResponse();
            default: throw new Exception( "unknown response number " + Integer.toString( header.getXID() ) );
        }
    }    

}
