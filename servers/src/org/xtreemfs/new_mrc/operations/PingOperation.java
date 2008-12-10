/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.new_mrc.operations;

import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.foundation.pinky.HTTPUtils.DATA_TYPE;
import org.xtreemfs.new_mrc.MRCRequest;
import org.xtreemfs.new_mrc.MRCRequestDispatcher;

/**
 *
 * @author bjko
 */
public class PingOperation extends MRCOperation {

    public PingOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    public boolean hasArguments() {
        return true;
    }
    
    @Override
    public void startRequest(MRCRequest rq) {
        rq.setData(ReusableBuffer.wrap("ping".getBytes()));
        rq.setDataType(DATA_TYPE.HTML);
        this.finishRequest(rq);
    }
    
}
