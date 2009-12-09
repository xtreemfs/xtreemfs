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


    public static int getVersion() { return 2009121010; }

    public static ONCRPCException createException( int accept_stat ) throws Exception
    {
        switch( accept_stat )
        {
            case 2009121030: return new ConcurrentModificationException();
            case 2009121033: return new DIRException();
            case 2009121031: return new InvalidArgumentException();
            case 2009121032: return new ProtocolException();
            case 2009121034: return new RedirectException();

            default: throw new Exception( "unknown accept_stat " + Integer.toString( accept_stat ) );
        }
    }

    public static Request createRequest( ONCRPCRequestHeader header ) throws Exception
    {
        switch( header.getProcedure() )
        {
            case 2009121011: return new xtreemfs_address_mappings_getRequest();
            case 2009121012: return new xtreemfs_address_mappings_removeRequest();
            case 2009121013: return new xtreemfs_address_mappings_setRequest();
            case 2009121014: return new xtreemfs_checkpointRequest();
            case 2009121015: return new xtreemfs_discover_dirRequest();
            case 2009121016: return new xtreemfs_global_time_s_getRequest();
            case 2009121017: return new xtreemfs_replication_to_masterRequest();
            case 2009121022: return new xtreemfs_service_deregisterRequest();
            case 2009121020: return new xtreemfs_service_get_by_nameRequest();
            case 2009121018: return new xtreemfs_service_get_by_typeRequest();
            case 2009121019: return new xtreemfs_service_get_by_uuidRequest();
            case 2009121023: return new xtreemfs_service_offlineRequest();
            case 2009121021: return new xtreemfs_service_registerRequest();
            case 2009121024: return new xtreemfs_shutdownRequest();

            default: throw new Exception( "unknown request tag " + Integer.toString( header.getProcedure() ) );
        }
    }
            
    public static Response createResponse( ONCRPCResponseHeader header ) throws Exception
    {
        switch( header.getXID() )
        {
            case 2009121011: return new xtreemfs_address_mappings_getResponse();            case 2009121012: return new xtreemfs_address_mappings_removeResponse();            case 2009121013: return new xtreemfs_address_mappings_setResponse();            case 2009121014: return new xtreemfs_checkpointResponse();            case 2009121015: return new xtreemfs_discover_dirResponse();            case 2009121016: return new xtreemfs_global_time_s_getResponse();            case 2009121017: return new xtreemfs_replication_to_masterResponse();            case 2009121022: return new xtreemfs_service_deregisterResponse();            case 2009121020: return new xtreemfs_service_get_by_nameResponse();            case 2009121018: return new xtreemfs_service_get_by_typeResponse();            case 2009121019: return new xtreemfs_service_get_by_uuidResponse();            case 2009121023: return new xtreemfs_service_offlineResponse();            case 2009121021: return new xtreemfs_service_registerResponse();            case 2009121024: return new xtreemfs_shutdownResponse();
            default: throw new Exception( "unknown response XID " + Integer.toString( header.getXID() ) );
        }
    }    

}
