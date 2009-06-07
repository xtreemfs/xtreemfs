package org.xtreemfs.interfaces.DIRInterface;

import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class DIRInterface
{
    public static final int DEFAULT_ONCRPC_PORT = 32638;
    public static final int DEFAULT_ONCRPCS_PORT = 32638;
    public static final int DEFAULT_HTTP_PORT = 30638;


    public static int getVersion() { return 1100; }

    public static ONCRPCException createException( int accept_stat ) throws Exception
    {
        switch( accept_stat )
        {
            case 1106: return new ConcurrentModificationException();
            case 1108: return new InvalidArgumentException();
            case 1109: return new ProtocolException();

            default: throw new Exception( "unknown accept_stat " + Integer.toString( accept_stat ) );
        }
    }

    public static Request createRequest( ONCRPCRequestHeader header ) throws Exception
    {
        switch( header.getProcedure() )
        {
            case 1101: return new xtreemfs_address_mappings_getRequest();
            case 1103: return new xtreemfs_address_mappings_removeRequest();
            case 1102: return new xtreemfs_address_mappings_setRequest();
            case 1150: return new xtreemfs_checkpointRequest();
            case 1152: return new xtreemfs_discover_dirRequest();
            case 1108: return new xtreemfs_global_time_s_getRequest();
            case 1106: return new xtreemfs_service_get_by_typeRequest();
            case 1107: return new xtreemfs_service_get_by_uuidRequest();
            case 1109: return new xtreemfs_service_get_by_nameRequest();
            case 1104: return new xtreemfs_service_registerRequest();
            case 1105: return new xtreemfs_service_deregisterRequest();
            case 1110: return new xtreemfs_service_offlineRequest();
            case 1151: return new xtreemfs_shutdownRequest();

            default: throw new Exception( "unknown request tag " + Integer.toString( header.getProcedure() ) );
        }
    }
            
    public static Response createResponse( ONCRPCResponseHeader header ) throws Exception
    {
        switch( header.getXID() )
        {
            case 1101: return new xtreemfs_address_mappings_getResponse();            case 1103: return new xtreemfs_address_mappings_removeResponse();            case 1102: return new xtreemfs_address_mappings_setResponse();            case 1150: return new xtreemfs_checkpointResponse();            case 1152: return new xtreemfs_discover_dirResponse();            case 1108: return new xtreemfs_global_time_s_getResponse();            case 1106: return new xtreemfs_service_get_by_typeResponse();            case 1107: return new xtreemfs_service_get_by_uuidResponse();            case 1109: return new xtreemfs_service_get_by_nameResponse();            case 1104: return new xtreemfs_service_registerResponse();            case 1105: return new xtreemfs_service_deregisterResponse();            case 1110: return new xtreemfs_service_offlineResponse();            case 1151: return new xtreemfs_shutdownResponse();
            default: throw new Exception( "unknown response XID " + Integer.toString( header.getXID() ) );
        }
    }    

}
