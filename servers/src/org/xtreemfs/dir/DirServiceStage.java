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
 * AUTHORS: Jan Stender (ZIB)
 */

package org.xtreemfs.dir;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import org.xtreemfs.common.auth.AuthenticationException;
import org.xtreemfs.common.auth.AuthenticationProvider;
import org.xtreemfs.common.auth.UserCredentials;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.pinky.HTTPHeaders;
import org.xtreemfs.new_mrc.ErrNo;

public class DirServiceStage extends LifeCycleThread {

    private final LinkedBlockingQueue<DIRRequest> queue;

    private final DirService                     dirService;

    private final AuthenticationProvider         auth;

    public DirServiceStage(String dbDir, AuthenticationProvider auth) throws SQLException {

        super("Directory Service");
        this.auth = auth;

        dirService = new DirService(dbDir);
        queue = new LinkedBlockingQueue<DIRRequest>();
    }

    public void processRequest(DIRRequest request) {
        queue.add(request);
    }

    public void shutdown() throws SQLException {
        interrupt();
        dirService.shutdown();
    }

    public void setRequestListener(DIRRequestListener listener) {
        assert (listener != null);
        dirService.setRequestListener(listener);
    }

    public void run() {

        try {

            notifyStarted();

            for (;;) {

                DIRRequest request = null;
                try {
                    if (isInterrupted())
                        break;
                    request = queue.take();
                } catch (InterruptedException e1) {
                    break;
                }

                try {

                    Object args = MessageUtils.unmarshallRequest(request);

                    // parse the user Id from the "AUTHORIZATION" header
                    if (request.details.userId == null) {
                        String authHeader = request.getPinkyRequest().requestHeaders
                                .getHeader(HTTPHeaders.HDR_AUTHORIZATION);

                        if (authHeader == null)
                            throw new UserException(ErrNo.EPERM, "authorization mechanism required");

                        UserCredentials cred = null;
                        try {
                            cred = auth.getEffectiveCredentials(authHeader, request.getPinkyRequest()
                                    .getChannelIO());
                            request.details.superUser = cred.isSuperUser();
                            request.details.userId = cred.getUserID();
                        } catch (AuthenticationException ex) {
                            throw new UserException(ErrNo.EPERM, ex.getMessage());
                        }
                    }

                    executeCommand(request, args);

                } catch (Exception exc) {
                    MessageUtils.marshallException(request, exc);
                    dirService.notifyRequestListener(request);
                }

            }

        } catch (Throwable th) {
            notifyCrashed(th instanceof Exception ? (Exception) th : new Exception(th));
            return;
        }

        notifyStopped();
    }

    private void executeCommand(DIRRequest request, Object args) {

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
                m.invoke(dirService, request);
            else
                m.invoke(dirService, argArray);

        } catch (InvocationTargetException exc) {
            MessageUtils.marshallException(request, exc.getCause());
            dirService.notifyRequestListener(request);
        } catch (Exception exc) {
            MessageUtils.marshallException(request, exc);
            dirService.notifyRequestListener(request);
            Logging.logMessage(Logging.LEVEL_ERROR, this, exc);
        }
    }

    private Method findMethod(String name, Object[] args) throws NoSuchMethodException {

        Method[] methods = dirService.getClass().getMethods();
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
                if (!(arg instanceof DIRRequest))
                    list.add(arg);

            String argList = null;
            try {
                argList = JSONParser.writeJSON(list);
            } catch (JSONException exc) {
                exc.printStackTrace();
            }

            throw new NoSuchMethodException("could not find appropriate method '" + name
                + "' for arguments " + argList);
        }

        return m;
    }

    protected Object[] getDBDump() throws SQLException, JSONException {
        return new Object[] { dirService.getEntityDBDump(), dirService.getMappingDBDump() };
    }

}
