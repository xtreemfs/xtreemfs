package org.xtreemfs.interfaces.utils;


public interface Request extends Serializable
{
    int getOperationNumber();
    Response createDefaultResponse();
};   
