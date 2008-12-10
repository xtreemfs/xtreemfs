/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.new_mrc.stages;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;
import org.xtreemfs.foundation.pinky.HTTPUtils;
import org.xtreemfs.foundation.pinky.PinkyRequest;
import org.xtreemfs.new_mrc.ErrorRecord;
import org.xtreemfs.new_mrc.MRCRequest;
import org.xtreemfs.new_mrc.MRCRequestDispatcher;
import org.xtreemfs.new_mrc.operations.MRCOperation;
import org.xtreemfs.new_mrc.operations.PingOperation;
import org.xtreemfs.new_mrc.operations.ShutdownOperation;
import org.xtreemfs.new_mrc.operations.StatusPageOperation;

/**
 *
 * @author bjko
 */
public class ProcessingStage extends MRCStage {

    public static final int STAGEOP_PARSE_AND_EXECUTE = 1;
    
    private final MRCRequestDispatcher master;
    
    private final Map<String, MRCOperation> operations;
    
    public ProcessingStage(MRCRequestDispatcher master) {
        super("ProcSt");
        this.master = master;
        operations = new HashMap();
        
        installOperations();
    }
    
    public void installOperations() {
        operations.put(ShutdownOperation.RPC_NAME, new ShutdownOperation(master));
        operations.put(StatusPageOperation.RPC_NAME, new StatusPageOperation(master));
    }
    
    @Override
    protected void processMethod(StageMethod method) {
        if (method.getStageMethod() == STAGEOP_PARSE_AND_EXECUTE) {
            parseAndExecute(method);
        } else {
            method.getRq().setError(new ErrorRecord(ErrorRecord.ErrorClass.INTERNAL_SERVER_ERROR,"unknown stage operation"));
            master.requestFinished(method.getRq());
        }
    }

    /**
     * Parse request and execute method
     * @param method stagemethod to execute
     */
    private void parseAndExecute(StageMethod method) {
        final MRCRequest rq = method.getRq();
        final PinkyRequest theRequest = rq.getPinkyRequest();
        final String URI = theRequest.requestURI.startsWith("/") ? theRequest.requestURI.substring(1) : theRequest.requestURI;
        
        final MRCOperation op = operations.get(URI);
        if (op == null) {
            rq.setError(new ErrorRecord(ErrorRecord.ErrorClass.BAD_REQUEST, "BAD REQUEST: unknown operation '"+URI+"'"));
            master.requestFinished(rq);
            return;
        }
        
        if (op.hasArguments()) {
            if ((theRequest.requestBody == null) ||
                (theRequest.requestBody.capacity() == 0)) {
                rq.setError(new ErrorRecord(ErrorRecord.ErrorClass.BAD_REQUEST, "BAD REQUEST: operation '"+URI+"' requires arguments"));
                master.requestFinished(rq);
                return;
            }
            List<Object> args = null;
            try {
                final JSONString jst = new JSONString(new String(theRequest.getBody(),HTTPUtils.ENC_UTF8));
                Object o = JSONParser.parseJSON(jst);
                args = (List<Object>)o;
            } catch (ClassCastException ex) {
                rq.setError(new ErrorRecord(ErrorRecord.ErrorClass.BAD_REQUEST, "BAD REQUEST: arguments must be JSON List"));
                master.requestFinished(rq);
                return;
            } catch (JSONException ex) {
                rq.setError(new ErrorRecord(ErrorRecord.ErrorClass.BAD_REQUEST, "BAD REQUEST: body is not valid JSON: "+ex));
                master.requestFinished(rq);
                return;
            }
            op.parseRPCBody(rq, args);
        }
        op.startRequest(rq);
    }

}
