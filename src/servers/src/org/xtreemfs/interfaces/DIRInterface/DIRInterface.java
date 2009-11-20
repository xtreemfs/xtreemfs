package org.xtreemfs.interfaces.DIRInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.PrettyPrinter;




public class DIRInterface
{
    public static final int DEFAULT_ONCRPC_PORT = 32638;
    public static final int DEFAULT_ONCRPCG_PORT = 32638;
    public static final int DEFAULT_ONCRPCS_PORT = 32638;
    public static final int DEFAULT_ONCRPCU_PORT = 32638;
    public static final int DEFAULT_HTTP_PORT = 30638;


    public static int getVersion() { return 2009082718; }

    public static ONCRPCException createException( int accept_stat ) throws Exception
    {
        switch( accept_stat )
        {
            case 2009082738: return new ConcurrentModificationException();
            case 2009082739: return new InvalidArgumentException();
            case 2009082740: return new ProtocolException();
            case 2009082741: return new DIRException();
            case 2009082742: return new RedirectException();

            default: throw new Exception( "unknown accept_stat " + Integer.toString( accept_stat ) );
        }
    }

    public static Request createRequest( ONCRPCRequestHeader header ) throws Exception
    {
        switch( header.getProcedure() )
        {
            case 2009082719: return new xtreemfs_address_mappings_getRequest();
            case 2009082720: return new xtreemfs_address_mappings_removeRequest();
            case 2009082721: return new xtreemfs_address_mappings_setRequest();
            case 2009082722: return new xtreemfs_checkpointRequest();
            case 2009082723: return new xtreemfs_discover_dirRequest();
            case 2009082724: return new xtreemfs_global_time_s_getRequest();
            case 2009082725: return new xtreemfs_replication_to_masterRequest();
            case 2009082726: return new xtreemfs_service_get_by_typeRequest();
            case 2009082727: return new xtreemfs_service_get_by_uuidRequest();
            case 2009082728: return new xtreemfs_service_get_by_nameRequest();
            case 2009082729: return new xtreemfs_service_registerRequest();
            case 2009082730: return new xtreemfs_service_deregisterRequest();
            case 2009082731: return new xtreemfs_service_offlineRequest();
            case 2009082732: return new xtreemfs_shutdownRequest();

            default: throw new Exception( "unknown request tag " + Integer.toString( header.getProcedure() ) );
        }
    }
            
    public static Response createResponse( ONCRPCResponseHeader header ) throws Exception
    {
        switch( header.getXID() )
        {
            case 2009082719: return new xtreemfs_address_mappings_getResponse();            case 2009082720: return new xtreemfs_address_mappings_removeResponse();            case 2009082721: return new xtreemfs_address_mappings_setResponse();            case 2009082722: return new xtreemfs_checkpointResponse();            case 2009082723: return new xtreemfs_discover_dirResponse();            case 2009082724: return new xtreemfs_global_time_s_getResponse();            case 2009082725: return new xtreemfs_replication_to_masterResponse();            case 2009082726: return new xtreemfs_service_get_by_typeResponse();            case 2009082727: return new xtreemfs_service_get_by_uuidResponse();            case 2009082728: return new xtreemfs_service_get_by_nameResponse();            case 2009082729: return new xtreemfs_service_registerResponse();            case 2009082730: return new xtreemfs_service_deregisterResponse();            case 2009082731: return new xtreemfs_service_offlineResponse();            case 2009082732: return new xtreemfs_shutdownResponse();
            default: throw new Exception( "unknown response XID " + Integer.toString( header.getXID() ) );
        }
    }    

}
