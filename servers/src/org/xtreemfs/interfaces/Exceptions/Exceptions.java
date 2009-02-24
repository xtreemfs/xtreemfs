package org.xtreemfs.interfaces.Exceptions;

import org.xtreemfs.interfaces.utils.ONCRPCRequestHeader;
import org.xtreemfs.interfaces.utils.ONCRPCResponseHeader;
import org.xtreemfs.interfaces.utils.Request;
import org.xtreemfs.interfaces.utils.Response;


class Exceptions
{
    public static int getVersion() { return 1; }

    public static Request createRequest( ONCRPCRequestHeader header ) throws Exception
    {
        return createRequest( header.getOperationNumber() );
    }

    public static Request createRequest( int uid ) throws Exception
    {
        switch( uid )
        {

            default: throw new Exception( "unknown request number " + Integer.toString( uid ) );
        }
    }
    
    public static Response createResponse( ONCRPCResponseHeader header ) throws Exception
    {
        if ( header.getReplyStat() == ONCRPCResponseHeader.ACCEPT_STAT_SUCCESS )
            return createResponse( header.getXID() );
        else
            throw new Exception( "not implemented" );
    }

    public static Response createResponse( int uid ) throws Exception
    {
        switch( uid )
        {

            default: throw new Exception( "unknown response number " + Integer.toString( uid ) );
        }
    }    
}
