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
            case 2010031226: return new readRequestRequest();
            case 2010031227: return new truncateRequestRequest();
            case 2010031228: return new unlinkRequestRequest();
            case 2010031229: return new writeRequestRequest();
            case 2010031236: return new xtreemfs_broadcast_gmaxRequestRequest();
            case 2010031237: return new xtreemfs_check_objectRequestRequest();
            case 2010031246: return new xtreemfs_cleanup_get_resultsRequestRequest();
            case 2010031247: return new xtreemfs_cleanup_is_runningRequestRequest();
            case 2010031248: return new xtreemfs_cleanup_startRequestRequest();
            case 2010031249: return new xtreemfs_cleanup_statusRequestRequest();
            case 2010031250: return new xtreemfs_cleanup_stopRequestRequest();
            case 2010031289: return new xtreemfs_rwr_fetchRequestRequest();
            case 2010031287: return new xtreemfs_rwr_flease_msgRequestRequest();
            case 2010031291: return new xtreemfs_rwr_notifyRequestRequest();
            case 2010031292: return new xtreemfs_rwr_statusRequestRequest();
            case 2010031290: return new xtreemfs_rwr_truncateRequestRequest();
            case 2010031288: return new xtreemfs_rwr_updateRequestRequest();
            case 2010031256: return new xtreemfs_internal_get_gmaxRequestRequest();
            case 2010031257: return new xtreemfs_internal_truncateRequestRequest();
            case 2010031258: return new xtreemfs_internal_get_file_sizeRequestRequest();
            case 2010031259: return new xtreemfs_internal_read_localRequestRequest();
            case 2010031260: return new xtreemfs_internal_get_object_setRequestRequest();
            case 2010031266: return new xtreemfs_lock_acquireRequestRequest();
            case 2010031267: return new xtreemfs_lock_checkRequestRequest();
            case 2010031268: return new xtreemfs_lock_releaseRequestRequest();
            case 2010031276: return new xtreemfs_pingRequestRequest();
            case 2010031286: return new xtreemfs_shutdownRequestRequest();
            default: throw new Exception( "unknown request tag " + Integer.toString( header.getProcedure() ) );
        }
    }

    public static Response createResponse( ONCRPCResponseHeader header ) throws Exception
    {
        switch( header.getXID() )
        {
            case 2010031226: return new readResponseResponse();case 2010031227: return new truncateResponseResponse();case 2010031228: return new unlinkResponseResponse();case 2010031229: return new writeResponseResponse();case 2010031236: return new xtreemfs_broadcast_gmaxResponseResponse();case 2010031237: return new xtreemfs_check_objectResponseResponse();case 2010031246: return new xtreemfs_cleanup_get_resultsResponseResponse();case 2010031247: return new xtreemfs_cleanup_is_runningResponseResponse();case 2010031248: return new xtreemfs_cleanup_startResponseResponse();case 2010031249: return new xtreemfs_cleanup_statusResponseResponse();case 2010031250: return new xtreemfs_cleanup_stopResponseResponse();case 2010031289: return new xtreemfs_rwr_fetchResponseResponse();case 2010031287: return new xtreemfs_rwr_flease_msgResponseResponse();case 2010031291: return new xtreemfs_rwr_notifyResponseResponse();case 2010031292: return new xtreemfs_rwr_statusResponseResponse();case 2010031290: return new xtreemfs_rwr_truncateResponseResponse();case 2010031288: return new xtreemfs_rwr_updateResponseResponse();case 2010031256: return new xtreemfs_internal_get_gmaxResponseResponse();case 2010031257: return new xtreemfs_internal_truncateResponseResponse();case 2010031258: return new xtreemfs_internal_get_file_sizeResponseResponse();case 2010031259: return new xtreemfs_internal_read_localResponseResponse();case 2010031260: return new xtreemfs_internal_get_object_setResponseResponse();case 2010031266: return new xtreemfs_lock_acquireResponseResponse();case 2010031267: return new xtreemfs_lock_checkResponseResponse();case 2010031268: return new xtreemfs_lock_releaseResponseResponse();case 2010031276: return new xtreemfs_pingResponseResponse();case 2010031286: return new xtreemfs_shutdownResponseResponse();
            default: throw new Exception( "unknown response XID " + Integer.toString( header.getXID() ) );
        }
    }

}
