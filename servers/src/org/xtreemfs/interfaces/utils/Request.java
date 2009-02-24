package org.xtreemfs.interfaces.utils;


public interface Request extends Serializable
{
    int getInterfaceVersion();    
    int getOperationNumber();
    Response createDefaultResponse();
};   
