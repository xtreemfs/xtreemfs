package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class OSDInterface
{
    public static final int DEFAULT_ONCRPC_PORT = 32640;
    public static final int DEFAULT_ONCRPCS_PORT = 32640;
    public static final int DEFAULT_HTTP_PORT = 30640;


    public static int getVersion() { return 1300; }

    public static ONCRPCException createException( int accept_stat ) throws Exception
    {
        switch( accept_stat )
        {
            case 1306: return new ConcurrentModificationException();
            case 1307: return new errnoException();
            case 1308: return new InvalidArgumentException();
            case 1311: return new OSDException();
            case 1309: return new ProtocolException();
            case 1310: return new RedirectException();

            default: throw new Exception( "unknown accept_stat " + Integer.toString( accept_stat ) );
        }
    }

    public static Request createRequest( ONCRPCRequestHeader header ) throws Exception
    {
        switch( header.getProcedure() )
        {
            case 1301: return new readRequest();
            case 1302: return new truncateRequest();
            case 1303: return new unlinkRequest();
            case 1304: return new writeRequest();
            case 2300: return new xtreemfs_broadcast_gmaxRequest();
            case 1403: return new xtreemfs_check_objectRequest();
            case 1409: return new xtreemfs_cleanup_get_resultsRequest();
            case 1408: return new xtreemfs_cleanup_is_runningRequest();
            case 1405: return new xtreemfs_cleanup_startRequest();
            case 1407: return new xtreemfs_cleanup_statusRequest();
            case 1406: return new xtreemfs_cleanup_stopRequest();
            case 1400: return new xtreemfs_internal_get_gmaxRequest();
            case 1401: return new xtreemfs_internal_truncateRequest();
            case 1404: return new xtreemfs_internal_get_file_sizeRequest();
            case 1402: return new xtreemfs_internal_read_localRequest();
            case 1410: return new xtreemfs_internal_get_object_setRequest();
            case 2301: return new xtreemfs_pingRequest();
            case 1350: return new xtreemfs_shutdownRequest();

            default: throw new Exception( "unknown request tag " + Integer.toString( header.getProcedure() ) );
        }
    }
            
    public static Response createResponse( ONCRPCResponseHeader header ) throws Exception
    {
        switch( header.getXID() )
        {
            case 1301: return new readResponse();            case 1302: return new truncateResponse();            case 1303: return new unlinkResponse();            case 1304: return new writeResponse();            case 2300: return new xtreemfs_broadcast_gmaxResponse();            case 1403: return new xtreemfs_check_objectResponse();            case 1409: return new xtreemfs_cleanup_get_resultsResponse();            case 1408: return new xtreemfs_cleanup_is_runningResponse();            case 1405: return new xtreemfs_cleanup_startResponse();            case 1407: return new xtreemfs_cleanup_statusResponse();            case 1406: return new xtreemfs_cleanup_stopResponse();            case 1400: return new xtreemfs_internal_get_gmaxResponse();            case 1401: return new xtreemfs_internal_truncateResponse();            case 1404: return new xtreemfs_internal_get_file_sizeResponse();            case 1402: return new xtreemfs_internal_read_localResponse();            case 1410: return new xtreemfs_internal_get_object_setResponse();            case 2301: return new xtreemfs_pingResponse();            case 1350: return new xtreemfs_shutdownResponse();
            default: throw new Exception( "unknown response XID " + Integer.toString( header.getXID() ) );
        }
    }    

}
