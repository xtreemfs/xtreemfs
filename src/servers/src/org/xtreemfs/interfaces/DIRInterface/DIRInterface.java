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


    public static int getVersion() { return 2009120314; }

    public static ONCRPCException createException( int accept_stat ) throws Exception
    {
        switch( accept_stat )
        {
            case 2009120334: return new ConcurrentModificationException();
            case 2009120337: return new DIRException();
            case 2009120335: return new InvalidArgumentException();
            case 2009120336: return new ProtocolException();
            case 2009120338: return new RedirectException();

            default: throw new Exception( "unknown accept_stat " + Integer.toString( accept_stat ) );
        }
    }

    public static Request createRequest( ONCRPCRequestHeader header ) throws Exception
    {
        switch( header.getProcedure() )
        {
            case 2009120315: return new xtreemfs_address_mappings_getRequest();
            case 2009120316: return new xtreemfs_address_mappings_removeRequest();
            case 2009120317: return new xtreemfs_address_mappings_setRequest();
            case 2009120318: return new xtreemfs_checkpointRequest();
            case 2009120319: return new xtreemfs_discover_dirRequest();
            case 2009120320: return new xtreemfs_global_time_s_getRequest();
            case 2009120321: return new xtreemfs_replication_to_masterRequest();
            case 2009120326: return new xtreemfs_service_deregisterRequest();
            case 2009120324: return new xtreemfs_service_get_by_nameRequest();
            case 2009120322: return new xtreemfs_service_get_by_typeRequest();
            case 2009120323: return new xtreemfs_service_get_by_uuidRequest();
            case 2009120327: return new xtreemfs_service_offlineRequest();
            case 2009120325: return new xtreemfs_service_registerRequest();
            case 2009120328: return new xtreemfs_shutdownRequest();

            default: throw new Exception( "unknown request tag " + Integer.toString( header.getProcedure() ) );
        }
    }
            
    public static Response createResponse( ONCRPCResponseHeader header ) throws Exception
    {
        switch( header.getXID() )
        {
            case 2009120315: return new xtreemfs_address_mappings_getResponse();            case 2009120316: return new xtreemfs_address_mappings_removeResponse();            case 2009120317: return new xtreemfs_address_mappings_setResponse();            case 2009120318: return new xtreemfs_checkpointResponse();            case 2009120319: return new xtreemfs_discover_dirResponse();            case 2009120320: return new xtreemfs_global_time_s_getResponse();            case 2009120321: return new xtreemfs_replication_to_masterResponse();            case 2009120326: return new xtreemfs_service_deregisterResponse();            case 2009120324: return new xtreemfs_service_get_by_nameResponse();            case 2009120322: return new xtreemfs_service_get_by_typeResponse();            case 2009120323: return new xtreemfs_service_get_by_uuidResponse();            case 2009120327: return new xtreemfs_service_offlineResponse();            case 2009120325: return new xtreemfs_service_registerResponse();            case 2009120328: return new xtreemfs_shutdownResponse();
            default: throw new Exception( "unknown response XID " + Integer.toString( header.getXID() ) );
        }
    }    

}
