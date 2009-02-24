package org.xtreemfs.interfaces.Exceptions;

import org.xtreemfs.interfaces.utils.ONCRPCRequestHeader;
import org.xtreemfs.interfaces.utils.ONCRPCResponseHeader;
import org.xtreemfs.interfaces.utils.Request;
import org.xtreemfs.interfaces.utils.Response;
import org.xtreemfs.interfaces.Exceptions.*;


public class Exceptions
{
    public static int getVersion() { return 1; }

    public static Exception createException( String exception_type_name ) throws Exception
    {
        if ( exception_type_name == "xtreemfs::interfaces::Exceptions::ProtocolException" ) return new ProtocolException();
        else if ( exception_type_name == "xtreemfs::interfaces::Exceptions::errnoException" ) return new errnoException();
        else if ( exception_type_name == "xtreemfs::interfaces::Exceptions::RedirectException" ) return new RedirectException();
        else if ( exception_type_name == "xtreemfs::interfaces::Exceptions::ConcurrentModificationException" ) return new ConcurrentModificationException();
        else throw new Exception( "unknown exception type " + exception_type_name );
    }
}
