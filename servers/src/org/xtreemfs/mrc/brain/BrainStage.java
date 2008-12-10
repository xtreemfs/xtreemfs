/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

 This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
 Grid Operating System, see <http://www.xtreemos.eu> for more details.
 The XtreemOS project has been developed with the financial support of the
 European Commission's IST program under contract #FP6-033576.

 XtreemFS is free software: you can redistribute it and/or modify it under
 the terms of the GNU General Public License as published by the Free
 Software Foundation, either version 2 of the License, or (at your option)
 any later version.

 XtreemFS is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Jan Stender (ZIB), Björn Kolbeck (ZIB)
 */

package org.xtreemfs.mrc.brain;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import org.xtreemfs.common.auth.AuthenticationException;
import org.xtreemfs.common.auth.AuthenticationProvider;
import org.xtreemfs.common.auth.UserCredentials;
import org.xtreemfs.common.clients.dir.DIRClient;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.pinky.HTTPHeaders;
import org.xtreemfs.foundation.pinky.PinkyRequest;
import org.xtreemfs.mrc.MRCConfig;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.PolicyContainer;
import org.xtreemfs.mrc.osdselection.OSDStatusManager;
import org.xtreemfs.mrc.slices.SliceManager;
import org.xtreemfs.mrc.utils.MessageUtils;

public class BrainStage extends LifeCycleThread {
    
    private final LinkedBlockingQueue<MRCRequest> queue;
    
    private final Brain                           brain;
    
    public final Map<String, Long>                _statMap;
    
    private boolean                               blocked;
    
    private AuthenticationProvider                auth;
    
    public BrainStage(MRCConfig config, DIRClient client, OSDStatusManager osdStatusManager,
        SliceManager slices, PolicyContainer policyContainer, AuthenticationProvider auth,
        String authString) throws BrainException {
        
        super("Brain");
        
        brain = new Brain(config, client, osdStatusManager, slices, policyContainer, authString);
        this.queue = new LinkedBlockingQueue<MRCRequest>();
        
        this.auth = auth;
        
        _statMap = new HashMap<String, Long>();
    }
    
    public void run() {
        
        Logging.logMessage(Logging.LEVEL_INFO, this, "operational");
        notifyStarted();
        
        try {
            for (;;) {
                
                MRCRequest request = null;
                try {
                    if (isInterrupted())
                        break;
                    request = queue.take();
                    
                    // FIXME!!!
                    while (blocked)
                        Thread.sleep(2000);
                } catch (InterruptedException e1) {
                    break;
                }
                
                // initial request
                if (request.details.context == null) {
                    
                    try {
                        
                        Object args = MessageUtils.unmarshallRequest(request);
                        
                        // parse the user Id from the "AUTHORIZATION" header
                        if (request.details.userId == null) {
                            String authHeader = request.getPinkyRequest().requestHeaders
                                    .getHeader(HTTPHeaders.HDR_AUTHORIZATION);
                            
                            if (authHeader == null)
                                throw new UserException(ErrNo.EPERM,
                                    "authorization mechanism required");
                            
                            UserCredentials cred = null;
                            try {
                                cred = auth.getEffectiveCredentials(authHeader, request
                                        .getPinkyRequest().getChannelIO());
                                request.details.superUser = cred.isSuperUser();
                                request.details.groupIds = cred.getGroupIDs();
                                request.details.userId = cred.getUserID();
                            } catch (AuthenticationException ex) {
                                throw new UserException(ErrNo.EPERM, ex.getMessage());
                            }
                            
                            /*
                             * if (authHeader.startsWith("{")) { //new JSON
                             * header format
                             * 
                             * String mech = null; String GUID = null;
                             * List<String> GGIDs = null; try { JSONString
                             * authStr = new JSONString(authHeader);
                             * Map<String,Object> authInfo = (Map<String,
                             * Object>) JSONParser.parseJSON(authStr); mech =
                             * (String) authInfo.get("mechanism"); GUID =
                             * (String) authInfo.get("guid"); GGIDs =
                             * (List<String>) authInfo.get("ggids"); } catch
                             * (Exception ex) { throw new
                             * UserException(ErrNo.EPERM, "malformed
                             * authentication credentials: "+ex); }
                             * 
                             * if (!mech.equals("nullauth")) throw new
                             * UserException(ErrNo.EPERM, "unknown authorization
                             * mechanism: " + mech);
                             * 
                             * request.userId = GUID; request.superUser = false;
                             * // FIXME: set 'true' if superuser!
                             * request.groupIds = GGIDs; } else { //old header
                             * format for comapatability! StringTokenizer st =
                             * new StringTokenizer( authHeader, " "); String
                             * mech = st.nextToken();
                             * 
                             * if (mech.equals("nullauth")) {
                             * 
                             * if (!st.hasMoreTokens()) throw new
                             * UserException(ErrNo.EPERM, "nullauth: user ID
                             * required"); // set the user ID request.userId =
                             * st.nextToken();
                             * 
                             * if (!st.hasMoreTokens()) throw new
                             * UserException(ErrNo.EPERM, "nullauth: at least
                             * one group ID required"); // set the group IDs
                             * request.groupIds = new ArrayList<String>(); while
                             * (st.hasMoreTokens())
                             * request.groupIds.add(st.nextToken());
                             * 
                             * }
                             * 
                             * else throw new UserException(ErrNo.EPERM,
                             * "unknown authorization mechanism: " + mech); }
                             */

                        }
                        
                        if (Logging.tracingEnabled()) {
                            Logging.logMessage(Logging.LEVEL_TRACE, this, "request: "
                                + request.getPinkyRequest());
                            Logging.logMessage(Logging.LEVEL_TRACE, this, "command: "
                                + request.getPinkyRequest().requestURI);
                            Logging.logMessage(Logging.LEVEL_TRACE, this, "args: " + args);
                        }
                        
                        executeCommand(request, args);
                        
                    } catch (Exception exc) {
                        MessageUtils.marshallException(request, exc);
                        brain.notifyRequestListener(request);
                    }
                    
                }

                // subsequent request
                else {
                    
                    try {
                        
                        String subsequentMethod = (String) request.details.context
                                .get("nextMethod");
                        
                        Method m = brain.getClass().getMethod(subsequentMethod,
                            new Class[] { MRCRequest.class });
                        m.invoke(brain, request);
                        
                    } catch (InvocationTargetException exc) {
                        // BrainHelper.submitException(brain, request,
                        // exc.getCause());
                        MessageUtils.marshallException(request, exc.getCause());
                        brain.notifyRequestListener(request);
                    } catch (Exception exc) {
                        // BrainHelper.submitException(brain, request, exc);
                        MessageUtils.marshallException(request, exc);
                        brain.notifyRequestListener(request);
                        Logging.logMessage(Logging.LEVEL_ERROR, this, exc);
                    }
                    
                }
                
            }
            
        } catch (Throwable th) {
            notifyCrashed(th instanceof Exception ? (Exception) th : new Exception(th));
            return;
        }
        
        Logging.logMessage(Logging.LEVEL_INFO, this, "shudtown complete");
        notifyStopped();
    }
    
    public void block() {
        blocked = true;
    }
    
    public void unblock() {
        blocked = false;
    }
    
    public void replayLogEntry(String cmd, String uid, String gid, Object args) throws Exception {
        
        // a 'move' command has to be handled explicitly; in case a remote MRC
        // is involved, only local activity has to be replayed
        // if(cmd.equals("move"))
        // cmd = "replayMove";
        
        PinkyRequest pr = new PinkyRequest(null, cmd, null, null);
        MRCRequest request = new MRCRequest(pr);
        request.details.userId = uid;
        request.details.groupIds = new ArrayList<String>(1);
        request.details.groupIds.add(gid);
        request.details.authorized = true; // important: override access control
        
        executeCommandSync(request, args);
    }
    
    public long getTotalDBSize() throws BrainException {
        return brain.getTotalDBSize();
    }
    
    public long getTotalNumberOfFiles() throws BrainException {
        return brain.getTotalNumberOfFiles();
    }
    
    public long getTotalNumberOfDirs() throws BrainException {
        return brain.getTotalNumberOfDirs();
    }
    
    public void shutdown() throws Exception {
        interrupt();
        brain.shutdown();
    }
    
    public void processRequest(MRCRequest request) {
        queue.add(request);
    }
    
    public void checkpointDB() throws BrainException {
        brain.checkpointDB();
    }
    
    public void completeDBCheckpoint() throws BrainException {
        brain.completeDBCheckpoint();
    }
    
    public void restoreDB() throws BrainException {
        brain.restoreDB();
    }
    
    public void setRequestListener(BrainRequestListener listener) {
        assert (listener != null);
        brain.setRequestListener(listener);
    }
    
    private void executeCommand(MRCRequest request, Object args) {
        
        try {
            
            // convert the arguments to a corresponding object array
            Object[] argArray = null;
            if (args != null)
                try {
                    List<Object> argList = (List<Object>) args;
                    argList.add(0, request);
                    argArray = argList.toArray();
                } catch (ClassCastException exc) {
                    argArray = new Object[] { request, args };
                }
            
            // find the appropriate brain method
            Method m = findMethod(request.getPinkyRequest().requestURI, argArray);
            
            // invoke the brain method
            if (args == null)
                m.invoke(brain, request);
            else
                m.invoke(brain, argArray);
            
            if (Logging.tracingEnabled())
                Logging.logMessage(Logging.LEVEL_TRACE, this, "exec: "
                    + request.getPinkyRequest().requestURI + " with " + argArray);
            
            Long count = _statMap.get(request.getPinkyRequest().requestURI);
            long newCount = count == null ? 1 : count + 1;
            _statMap.put(request.getPinkyRequest().requestURI, newCount);
            
            // check whether the operation needs to be logged
            
        } catch (InvocationTargetException exc) {
            request.details.persistentOperation = false;
            MessageUtils.marshallException(request, exc.getCause());
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_ERROR, this, exc.getCause());
            brain.notifyRequestListener(request);
        } catch (Exception exc) {
            request.details.persistentOperation = false;
            MessageUtils.marshallException(request, exc);
            brain.notifyRequestListener(request);
            Logging.logMessage(Logging.LEVEL_ERROR, this, exc);
        }
    }
    
    private void executeCommandSync(MRCRequest request, Object args) throws Exception {
        
        try {
            request.syncPseudoRequest = true;
            // convert the arguments to a corresponding object array
            Object[] argArray = null;
            if (args != null)
                try {
                    List<Object> argList = (List<Object>) args;
                    argList.add(0, request);
                    argArray = argList.toArray();
                } catch (ClassCastException exc) {
                    argArray = new Object[] { request, args };
                }
            
            else
                args = new Object[] { request };
            
            // find the appropriate brain method
            Method m = findMethod(request.getPinkyRequest().requestURI, argArray);
            
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "executing "
                + request.getPinkyRequest().requestURI + " with " + args);
            
            // invoke the brain method
            if (args == null)
                m.invoke(brain, request);
            else
                m.invoke(brain, argArray);
            
        } catch (InvocationTargetException exc) {
            request.details.persistentOperation = false;
            throw exc;
        } catch (Exception exc) {
            request.details.persistentOperation = false;
            throw exc;
        }
    }
    
    private Method findMethod(String name, Object[] args) throws NoSuchMethodException {
        
        Method[] methods = brain.getClass().getMethods();
        Method m = null;
        
        for (Method method : methods) {
            
            if (method.getName().equals(name)) {
                
                Class[] paramTypes = method.getParameterTypes();
                if (args.length > 1 && args.length != paramTypes.length)
                    continue;
                
                boolean ok = true;
                // TODO: check params
                // for (int i = 0; i < paramTypes.length; i++) {
                //
                // if (argsArray[i] != null
                // && !paramTypes[i].isInstance(argsArray[i])) {
                // ok = false;
                // }
                // }
                
                if (ok) {
                    m = method;
                    break;
                }
            }
        }
        
        if (m == null) {
            
            List<Object> list = new ArrayList<Object>(args.length);
            for (Object arg : args)
                if (!(arg instanceof MRCRequest))
                    list.add(arg);
            
            String argList = null;
            try {
                argList = JSONParser.writeJSON(list);
            } catch (JSONException exc) {
                Logging.logMessage(Logging.LEVEL_ERROR, this, exc);
            }
            
            throw new NoSuchMethodException("could not find appropriate method '" + name
                + "' for arguments " + argList);
        }
        
        return m;
    }
    
    public void dumpDB(String dumpFilePath) throws Exception {
        brain.dumpDB(dumpFilePath);
    }
    
    public void restoreDBFromDump(String dumpFilePath) throws Exception {
        brain.restoreDBFromDump(dumpFilePath);
    }
    
    public int getQLength() {
        return this.queue.size();
    }
    
}
