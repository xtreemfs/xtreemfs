package org.xtreemfs.interfaces;

import java.nio.ByteBuffer;


public interface Serializable
{
    public void serialize( ByteBuffer buf );
    public void deserialize( ByteBuffer buf );
    public int getSize();
};   
