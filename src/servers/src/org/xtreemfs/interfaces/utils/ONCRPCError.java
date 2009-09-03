/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.interfaces.utils;

import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import yidl.Marshaller;
import yidl.Unmarshaller;

/**
 *
 * @author bjko
 */
public class ONCRPCError extends ONCRPCException {

    final int accept_stat;
    
    public ONCRPCError(int accept_stat) {
        this.accept_stat = accept_stat;
    }

    public int getAcceptStat() {
        return accept_stat;
    }

    @Override
    public int getTag() {
        throw new RuntimeException("this exception must not be serialized");
    }

    @Override
    public String getTypeName() {
        return "ONCRPCError";
    }

    @Override
    public void marshal(Marshaller writer) {
        throw new RuntimeException("this exception must not be serialized");
    }

    @Override
    public void unmarshal(Unmarshaller buf) {
        throw new RuntimeException("this exception must not be serialized");
    }

    @Override
    public int getXDRSize() {
        throw new RuntimeException("this exception must not be serialized");
    }



}
