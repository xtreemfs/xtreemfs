package org.xtreemfs.contrib.provisioning;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xtreemfs.common.libxtreemfs.Client;
import org.xtreemfs.contrib.provisioning.JsonRPC.METHOD;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.AuthPassword;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.AuthType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;
import com.thetransactioncompany.jsonrpc2.server.RequestHandler;


public abstract class AbstractRequestHandler implements RequestHandler {

    String[] methodNames;
    Client client;

    public AbstractRequestHandler(Client c, METHOD[] methodNames) {
        this.client = c;
        this.methodNames = new String[methodNames.length];
        for (int i = 0; i < methodNames.length; i++) {
            this.methodNames[i] = methodNames[i].toString();
        }
    }

    /**
     * Reports the method names of the handled requests
     */
    public String[] handledRequests() {
        return this.methodNames;
    }


    /**
     * this method has to be implemented
     * @param req
     * @param ctx
     * @return
     */
    public abstract JSONRPC2Response doProcess(JSONRPC2Request req, MessageContext ctx) throws Exception;

    // Processes the requests
    @Override
    public JSONRPC2Response process(JSONRPC2Request req, MessageContext ctx) {
        try {
            return doProcess(req, ctx);
        } catch (NumberFormatException e) {
            Logger.getLogger(JsonRPC.class.getName()).log(Level.WARNING, e.getMessage());
            return new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), e.getMessage()), req.getID());
        } catch (IOException e) {
            Logger.getLogger(JsonRPC.class.getName()).log(Level.WARNING, e.getMessage());
            return new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INTERNAL_ERROR.getCode(), e.getMessage()), req.getID());
        } catch (Exception e) {
            Logger.getLogger(JsonRPC.class.getName()).log(Level.WARNING, e.getMessage());
            return new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INTERNAL_ERROR.getCode(), e.getMessage()), req.getID());
        }

    }

    public static Integer parseIntegerSafe(Object value) {
        if (value instanceof String) {
            if (value == null || ((String)value).trim().equals("")) {
                return 0;
            }
            return Integer.valueOf((String)value);
        }
        else if (value instanceof Number) {
            return (Integer) value;
        }
        throw new NumberFormatException();
    }

    public static Auth getAuth(String password) {
        Auth auth = Auth.newBuilder().setAuthType(AuthType.AUTH_PASSWORD)
                .setAuthPasswd(AuthPassword.newBuilder().setPassword(password))
                .build();
        return auth;
    }


    public static UserCredentials getGroups() {
        List<String> groups = new LinkedList<String>();
        groups.add("groupname");
        final UserCredentials uc = UserCredentials.newBuilder()
                .setUsername("root").addAllGroups(groups).build();
        return uc;
    }
}
