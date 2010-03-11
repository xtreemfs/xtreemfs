package org.xtreemfs.interfaces.DIRInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.PrettyPrinter;




public class DIRInterface
{
    public static final int HTTP_PORT_DEFAULT = 30638;
    public static final int ONCRPC_PORT_DEFAULT = 32638;
    public static final int ONCRPCG_PORT_DEFAULT = 32638;
    public static final int ONCRPCS_PORT_DEFAULT = 32638;
    public static final int ONCRPCU_PORT_DEFAULT = 32638;

    public static long getProg() { return 2546901928l; }
    public static int getVersion() { return 2010031016; }

    public static ONCRPCException createException( int accept_stat ) throws Exception
    {
        switch( accept_stat )
        {
            case 2010031036: return new ConcurrentModificationException();
            case 2010031039: return new DIRException();
            case 2010031037: return new InvalidArgumentException();
            case 2010031038: return new ProtocolException();
            case 2010031040: return new RedirectException();
            default: throw new Exception( "unknown accept_stat " + Integer.toString( accept_stat ) );
        }
    }

    public static Request createRequest( ONCRPCRequestHeader header ) throws Exception
    {
        switch( header.getProcedure() )
        {
            case 2010031017: return new xtreemfs_address_mappings_getRequestRequest();
            case 2010031018: return new xtreemfs_address_mappings_removeRequestRequest();
            case 2010031019: return new xtreemfs_address_mappings_setRequestRequest();
            case 2010031020: return new xtreemfs_checkpointRequestRequest();
            case 2010031021: return new xtreemfs_discover_dirRequestRequest();
            case 2010031022: return new xtreemfs_global_time_s_getRequestRequest();
            case 2010031023: return new xtreemfs_replication_to_masterRequestRequest();
            case 2010031028: return new xtreemfs_service_deregisterRequestRequest();
            case 2010031026: return new xtreemfs_service_get_by_nameRequestRequest();
            case 2010031024: return new xtreemfs_service_get_by_typeRequestRequest();
            case 2010031025: return new xtreemfs_service_get_by_uuidRequestRequest();
            case 2010031029: return new xtreemfs_service_offlineRequestRequest();
            case 2010031027: return new xtreemfs_service_registerRequestRequest();
            case 2010031030: return new xtreemfs_shutdownRequestRequest();
            default: throw new Exception( "unknown request tag " + Integer.toString( header.getProcedure() ) );
        }
    }

    public static Response createResponse( ONCRPCResponseHeader header ) throws Exception
    {
        switch( header.getXID() )
        {
            case 2010031017: return new xtreemfs_address_mappings_getResponseResponse();case 2010031018: return new xtreemfs_address_mappings_removeResponseResponse();case 2010031019: return new xtreemfs_address_mappings_setResponseResponse();case 2010031020: return new xtreemfs_checkpointResponseResponse();case 2010031021: return new xtreemfs_discover_dirResponseResponse();case 2010031022: return new xtreemfs_global_time_s_getResponseResponse();case 2010031023: return new xtreemfs_replication_to_masterResponseResponse();case 2010031028: return new xtreemfs_service_deregisterResponseResponse();case 2010031026: return new xtreemfs_service_get_by_nameResponseResponse();case 2010031024: return new xtreemfs_service_get_by_typeResponseResponse();case 2010031025: return new xtreemfs_service_get_by_uuidResponseResponse();case 2010031029: return new xtreemfs_service_offlineResponseResponse();case 2010031027: return new xtreemfs_service_registerResponseResponse();case 2010031030: return new xtreemfs_shutdownResponseResponse();
            default: throw new Exception( "unknown response XID " + Integer.toString( header.getXID() ) );
        }
    }

}
