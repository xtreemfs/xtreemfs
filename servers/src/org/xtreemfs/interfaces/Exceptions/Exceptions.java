package org.xtreemfs.interfaces.Exceptions;

import org.xtreemfs.interfaces.utils.ONCRPCRequestHeader;
import org.xtreemfs.interfaces.utils.ONCRPCResponseHeader;
import org.xtreemfs.interfaces.utils.Request;
import org.xtreemfs.interfaces.utils.Response;
import org.xtreemfs.interfaces.utils.ONCRPCException;
import org.xtreemfs.interfaces.Exceptions.*;


public class Exceptions
{
    public static int getVersion() { return 1; }

    public static ONCRPCException createException( String exception_type_name ) throws java.io.IOException
    {
        if ( exception_type_name.equals("xtreemfs::interfaces::Exceptions::ProtocolException") ) return new ProtocolException();
        else if ( exception_type_name.equals("xtreemfs::interfaces::Exceptions::errnoException") ) return new errnoException();
        else if ( exception_type_name.equals("xtreemfs::interfaces::Exceptions::RedirectException") ) return new RedirectException();
        else if ( exception_type_name.equals("xtreemfs::interfaces::Exceptions::MRCException") ) return new MRCException();
        else if ( exception_type_name.equals("xtreemfs::interfaces::Exceptions::OSDException") ) return new OSDException();
        else if ( exception_type_name.equals("xtreemfs::interfaces::Exceptions::ConcurrentModificationException") ) return new ConcurrentModificationException();
        else if ( exception_type_name.equals("xtreemfs::interfaces::Exceptions::InvalidArgumentException") ) return new InvalidArgumentException();
        else throw new java.io.IOException( "unknown exception type " + exception_type_name );
    }
}
