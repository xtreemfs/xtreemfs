package org.xtreemfs.interfaces.OSDInterface;

import java.io.StringWriter;
import org.xtreemfs.*;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.oncrpc.utils.*;
import org.xtreemfs.interfaces.*;
import yidl.runtime.PrettyPrinter;




public class OSDInterface
{
    public static final int HTTP_PORT_DEFAULT = 30640;
    public static final int ONC_RPC_PORT_DEFAULT = 32640;

    public static long getProg() { return 2546902128l; }
    public static int getVersion() { return 2010031216; }

    public static ONCRPCException createException( int accept_stat ) throws Exception
    {
        switch( accept_stat )
        {
            case 2010031217: return new ConcurrentModificationException();
            case 2010031218: return new errnoException();
            case 2010031219: return new InvalidArgumentException();
            case 2010031220: return new OSDException();
            case 2010031221: return new ProtocolException();
            case 2010031222: return new RedirectException();
            default: throw new Exception( "unknown accept_stat " + Integer.toString( accept_stat ) );
        }
    }

    public static Request createRequest( ONCRPCRequestHeader header ) throws Exception
    {
        switch( header.getProcedure() )
        {
            case 2010031226: return new readRequest();
            case 2010031227: return new truncateRequest();
            case 2010031228: return new unlinkRequest();
            case 2010031229: return new writeRequest();
            case 2010031236: return new xtreemfs_broadcast_gmaxRequest();
            case 2010031237: return new xtreemfs_check_objectRequest();
            case 2010031246: return new xtreemfs_cleanup_get_resultsRequest();
            case 2010031247: return new xtreemfs_cleanup_is_runningRequest();
            case 2010031248: return new xtreemfs_cleanup_startRequest();
            case 2010031249: return new xtreemfs_cleanup_statusRequest();
            case 2010031250: return new xtreemfs_cleanup_stopRequest();
            case 2010031289: return new xtreemfs_rwr_fetchRequest();
            case 2010031287: return new xtreemfs_rwr_flease_msgRequest();
            case 2010031291: return new xtreemfs_rwr_notifyRequest();
            case 2010031292: return new xtreemfs_rwr_statusRequest();
            case 2010031290: return new xtreemfs_rwr_truncateRequest();
            case 2010031288: return new xtreemfs_rwr_updateRequest();
            case 2010031256: return new xtreemfs_internal_get_gmaxRequest();
            case 2010031257: return new xtreemfs_internal_truncateRequest();
            case 2010031258: return new xtreemfs_internal_get_file_sizeRequest();
            case 2010031259: return new xtreemfs_internal_read_localRequest();
            case 2010031260: return new xtreemfs_internal_get_object_setRequest();
            case 2010031266: return new xtreemfs_lock_acquireRequest();
            case 2010031267: return new xtreemfs_lock_checkRequest();
            case 2010031268: return new xtreemfs_lock_releaseRequest();
            case 2010031276: return new xtreemfs_pingRequest();
            case 2010031286: return new xtreemfs_shutdownRequest();
            default: throw new Exception( "unknown request tag " + Integer.toString( header.getProcedure() ) );
        }
    }

    public static Response createResponse( ONCRPCResponseHeader header ) throws Exception
    {
        switch( header.getXID() )
        {
            case 2010031226: return new readResponse();
            case 2010031227: return new truncateResponse();
            case 2010031228: return new unlinkResponse();
            case 2010031229: return new writeResponse();
            case 2010031236: return new xtreemfs_broadcast_gmaxResponse();
            case 2010031237: return new xtreemfs_check_objectResponse();
            case 2010031246: return new xtreemfs_cleanup_get_resultsResponse();
            case 2010031247: return new xtreemfs_cleanup_is_runningResponse();
            case 2010031248: return new xtreemfs_cleanup_startResponse();
            case 2010031249: return new xtreemfs_cleanup_statusResponse();
            case 2010031250: return new xtreemfs_cleanup_stopResponse();
            case 2010031289: return new xtreemfs_rwr_fetchResponse();
            case 2010031287: return new xtreemfs_rwr_flease_msgResponse();
            case 2010031291: return new xtreemfs_rwr_notifyResponse();
            case 2010031292: return new xtreemfs_rwr_statusResponse();
            case 2010031290: return new xtreemfs_rwr_truncateResponse();
            case 2010031288: return new xtreemfs_rwr_updateResponse();
            case 2010031256: return new xtreemfs_internal_get_gmaxResponse();
            case 2010031257: return new xtreemfs_internal_truncateResponse();
            case 2010031258: return new xtreemfs_internal_get_file_sizeResponse();
            case 2010031259: return new xtreemfs_internal_read_localResponse();
            case 2010031260: return new xtreemfs_internal_get_object_setResponse();
            case 2010031266: return new xtreemfs_lock_acquireResponse();
            case 2010031267: return new xtreemfs_lock_checkResponse();
            case 2010031268: return new xtreemfs_lock_releaseResponse();
            case 2010031276: return new xtreemfs_pingResponse();
            case 2010031286: return new xtreemfs_shutdownResponse();
            default: throw new Exception( "unknown response XID " + Integer.toString( header.getXID() ) );
        }
    }
}