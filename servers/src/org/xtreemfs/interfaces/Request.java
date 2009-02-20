package org.xtreemfs.interfaces;


public interface Request extends Serializable
{
    int getOperationNumber();
    Response createDefaultResponse();
};   
