package org.xtreemfs.interfaces.OSDInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;
import yidl.runtime.PrettyPrinter;




public class OSDInterface
{
    public static final int HTTP_PORT_DEFAULT = 30640;
    public static final int ONCRPC_PORT_DEFAULT = 32640;
    public static final int ONCRPCG_PORT_DEFAULT = 32640;
    public static final int ONCRPCS_PORT_DEFAULT = 32640;
    public static final int ONCRPCU_PORT_DEFAULT = 32640;


    public static int getVersion() { return 2010012413; }

    public static ONCRPCException createException( int accept_stat ) throws Exception
    {
        switch( accept_stat )
        {
            case 2010012414: return new ConcurrentModificationException();
            case 2010012415: return new errnoException();
            case 2010012416: return new InvalidArgumentException();
            case 2010012417: return new OSDException();
            case 2010012418: return new ProtocolException();
            case 2010012419: return new RedirectException();

            default: throw new Exception( "unknown accept_stat " + Integer.toString( accept_stat ) );
        }
    }

    public static Request createRequest( ONCRPCRequestHeader header ) throws Exception
    {
        switch( header.getProcedure() )
        {
            case 2010012423: return new readRequest();
            case 2010012424: return new truncateRequest();
            case 2010012425: return new unlinkRequest();
            case 2010012426: return new writeRequest();
            case 2010012433: return new xtreemfs_broadcast_gmaxRequest();
            case 2010012434: return new xtreemfs_check_objectRequest();
            case 2010012443: return new xtreemfs_cleanup_get_resultsRequest();
            case 2010012444: return new xtreemfs_cleanup_is_runningRequest();
            case 2010012445: return new xtreemfs_cleanup_startRequest();
            case 2010012446: return new xtreemfs_cleanup_statusRequest();
            case 2010012447: return new xtreemfs_cleanup_stopRequest();
            case 2010012453: return new xtreemfs_internal_get_gmaxRequest();
            case 2010012454: return new xtreemfs_internal_truncateRequest();
            case 2010012455: return new xtreemfs_internal_get_file_sizeRequest();
            case 2010012456: return new xtreemfs_internal_read_localRequest();
            case 2010012457: return new xtreemfs_internal_get_object_setRequest();
            case 2010012463: return new xtreemfs_lock_acquireRequest();
            case 2010012464: return new xtreemfs_lock_checkRequest();
            case 2010012465: return new xtreemfs_lock_releaseRequest();
            case 2010012473: return new xtreemfs_pingRequest();
            case 2010012483: return new xtreemfs_shutdownRequest();

            default: throw new Exception( "unknown request tag " + Integer.toString( header.getProcedure() ) );
        }
    }
            
    public static Response createResponse( ONCRPCResponseHeader header ) throws Exception
    {
        switch( header.getXID() )
        {
            case 2010012423: return new readResponse();            case 2010012424: return new truncateResponse();            case 2010012425: return new unlinkResponse();            case 2010012426: return new writeResponse();            case 2010012433: return new xtreemfs_broadcast_gmaxResponse();            case 2010012434: return new xtreemfs_check_objectResponse();            case 2010012443: return new xtreemfs_cleanup_get_resultsResponse();            case 2010012444: return new xtreemfs_cleanup_is_runningResponse();            case 2010012445: return new xtreemfs_cleanup_startResponse();            case 2010012446: return new xtreemfs_cleanup_statusResponse();            case 2010012447: return new xtreemfs_cleanup_stopResponse();            case 2010012453: return new xtreemfs_internal_get_gmaxResponse();            case 2010012454: return new xtreemfs_internal_truncateResponse();            case 2010012455: return new xtreemfs_internal_get_file_sizeResponse();            case 2010012456: return new xtreemfs_internal_read_localResponse();            case 2010012457: return new xtreemfs_internal_get_object_setResponse();            case 2010012463: return new xtreemfs_lock_acquireResponse();            case 2010012464: return new xtreemfs_lock_checkResponse();            case 2010012465: return new xtreemfs_lock_releaseResponse();            case 2010012473: return new xtreemfs_pingResponse();            case 2010012483: return new xtreemfs_shutdownResponse();
            default: throw new Exception( "unknown response XID " + Integer.toString( header.getXID() ) );
        }
    }    

}
