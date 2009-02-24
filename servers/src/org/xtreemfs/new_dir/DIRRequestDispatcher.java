/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.new_dir;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.LifeCycleListener;
import org.xtreemfs.foundation.oncrpc.server.ONCRPCRequest;
import org.xtreemfs.foundation.oncrpc.server.RPCNIOSocketServer;
import org.xtreemfs.foundation.oncrpc.server.RPCServerRequestListener;
import org.xtreemfs.foundation.pinky.SSLOptions;
import org.xtreemfs.interfaces.DIRInterface.DIRInterface;
import org.xtreemfs.interfaces.Exceptions.ProtocolException;
import org.xtreemfs.interfaces.Exceptions.errnoException;
import org.xtreemfs.interfaces.utils.ONCRPCRequestHeader;
import org.xtreemfs.interfaces.utils.ONCRPCResponseHeader;
import org.xtreemfs.new_dir.operations.DIROperation;
import org.xtreemfs.new_dir.operations.GetAddressMappingOperation;
import org.xtreemfs.new_dir.operations.GetGlobalTimeOperation;
import org.xtreemfs.new_mrc.ErrNo;

/**
 *
 * @author bjko
 */
public class DIRRequestDispatcher implements RPCServerRequestListener, LifeCycleListener {

    private final Map<Integer,DIROperation> registry;

    private final RPCNIOSocketServer        server;

    
    public DIRRequestDispatcher(DIRConfig config) throws IOException {
        registry = new HashMap();

        registerOperations();

        //start the server

        SSLOptions sslOptions = null;
        if (config.isUsingSSL()) {
            sslOptions = new SSLOptions(config.getServiceCredsFile(), config
                    .getServiceCredsPassphrase(), config.getServiceCredsContainer(), config
                    .getTrustedCertsFile(), config.getTrustedCertsPassphrase(), config
                    .getTrustedCertsContainer(), false);
        }

        server = new RPCNIOSocketServer(config.getPort(), null, this, sslOptions);
    }
    
    public void startup() throws Exception {
        server.start();
        
        server.waitForStartup();
    }

    public void shutdown() throws Exception {
        server.shutdown();
        server.waitForShutdown();
    }

    private void registerOperations() {

        DIROperation op;
        op = new GetGlobalTimeOperation(this);
        registry.put(op.getProcedureId(),op);

        op = new GetAddressMappingOperation(this);
        registry.put(op.getProcedureId(),op);
    }

    @Override
    public void receiveRecord(ONCRPCRequest rq) {
        final ONCRPCRequestHeader hdr = rq.getRequestHeader();

        if (hdr.getInterfaceVersion() != DIRInterface.getVersion()) {
            rq.sendProtocolException(new ProtocolException(ONCRPCResponseHeader.ACCEPT_STAT_PROG_MISMATCH, 0, "invalid version requested"));
            return;
        }

        //everything ok, find the right operation
        DIROperation op = registry.get(hdr.getOperationNumber());
        if (op == null) {
            rq.sendProtocolException(new ProtocolException(ONCRPCResponseHeader.ACCEPT_STAT_PROC_UNAVAIL, 0, "requested operation is not available on this DIR"));
            return;
        }


        DIRRequest dirRq = new DIRRequest(rq);
        try {
            op.parseRPCMessage(dirRq);
        } catch (Exception ex) {
            rq.sendGarbageArgs(new errnoException(ErrNo.EINVAL, ex.toString()));
            return;
        }
        op.startRequest(dirRq);
    }

    @Override
    public void startupPerformed() {
    }

    @Override
    public void shutdownPerformed() {
    }

    @Override
    public void crashPerformed() {
        Logging.logMessage(Logging.LEVEL_ERROR, this,"a component ***CRASHED***, shutting down.");
        try {
            shutdown();
        } catch (Exception e) {
            Logging.logMessage(Logging.LEVEL_ERROR, this, e);
        }
    }


}
