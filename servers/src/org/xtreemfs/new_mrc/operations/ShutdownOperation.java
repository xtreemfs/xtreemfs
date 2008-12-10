/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.new_mrc.operations;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.new_mrc.MRCRequest;
import org.xtreemfs.new_mrc.MRCRequestDispatcher;

/**
 *
 * @author bjko
 */
public class ShutdownOperation extends MRCOperation {

    public static final String RPC_NAME = ".shutdown";
    
    public ShutdownOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) {
        try {
            master.shutdown();
        } catch (Exception ex) {
            Logging.logMessage(Logging.LEVEL_ERROR, this,ex);
        }
    }
    
}
