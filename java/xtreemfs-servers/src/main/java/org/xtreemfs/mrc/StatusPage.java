/**
 * Copyright (c) 2013 Johannes Dillmann, Zuse Institute Berlin 
 *
 * Licensed under the BSD License, see LICENSE file for details.
 * 
 */

package org.xtreemfs.mrc;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.xtreemfs.common.statusserver.StatusServerHelper;
import org.xtreemfs.common.statusserver.StatusServerModule;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceType;
import org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceConstants;

import com.sun.net.httpserver.HttpExchange;

/**
 * Serves a simple HTML status page with MRC stats.
 */
public class StatusPage extends StatusServerModule {

    public enum Vars {
        LASTRQDATE("<!-- $LASTRQDATE -->"), TOTALNUMRQ("<!-- $TOTALNUMRQ -->"), RQSTATS("<!-- $RQSTATS -->"), VOLUMES(
                "<!-- $VOLUMES -->"), UUID("<!-- $UUID -->"), AVAILPROCS("<!-- $AVAILPROCS -->"), BPSTATS(
                "<!-- $BPSTATS -->"), PORT("<!-- $PORT -->"), DIRURL("<!-- $DIRURL -->"), DEBUG("<!-- $DEBUG -->"), NUMCON(
                "<!-- $NUMCON -->"), PINKYQ("<!-- $PINKYQ -->"), PROCQ("<!-- $PROCQ -->"), GLOBALTIME(
                "<!-- $GLOBALTIME -->"), GLOBALRESYNC("<!-- $GLOBALRESYNC -->"), LOCALTIME("<!-- $LOCALTIME -->"), LOCALRESYNC(
                "<!-- $LOCALRESYNC -->"), MEMSTAT("<!-- $MEMSTAT -->"), UUIDCACHE("<!-- $UUIDCACHE -->"), DISKFREE(
                "<!-- $DISKFREE -->"), PROTOVERSION("<!-- $PROTOVERSION -->"), VERSION("<!-- $VERSION -->"), DBVERSION(
                "<!-- $DBVERSION -->");

        private String template;

        Vars(String template) {
            this.template = template;
        }

        public String toString() {
            return template;
        }
    }

    private final String                      statusPageTemplate;
    private MRCRequestDispatcher              master;

    /**
     * opNames contains a mapping from numerical operation ids to their textual representation. 
     * It is used by the static method {@link #getOpName} and has therefore to be statically initialized.
     */
    private static final Map<Integer, String> opNames;
    static {
        opNames = new HashMap<Integer, String>();
        for (Field field : MRCServiceConstants.class.getDeclaredFields()) {
            if (field.getName().startsWith("PROC_ID"))
                try {
                    opNames.put(field.getInt(null), field.getName().substring("PROC_ID_".length()).toLowerCase());
                } catch (IllegalArgumentException e) {
                    Logging.logError(Logging.LEVEL_ERROR, null, e);
                } catch (IllegalAccessException e) {
                    Logging.logError(Logging.LEVEL_ERROR, null, e);
                }
        }
    }

    public StatusPage() {
        StringBuffer sb = StatusServerHelper.readTemplate("org/xtreemfs/mrc/templates/status.html");
        if (sb == null) {
            statusPageTemplate = "<h1>Template was not found, unable to show status page!</h1>";
        } else {
            statusPageTemplate = sb.toString();
        }
    }

    @Override
    public void initialize(ServiceType service, Object serviceRequestDispatcher) {
        assert (service == ServiceType.SERVICE_TYPE_MRC);
        master = (MRCRequestDispatcher) serviceRequestDispatcher;
    }

    @Override
    public String getDisplayName() {
        return "MRC Status Page";
    }

    @Override
    public String getUriPath() {
        return "/";
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        Map<Vars, String> vars = master.getStatusInformation();
        String tmp = statusPageTemplate;
        for (Vars key : vars.keySet()) {
            tmp = tmp.replace(key.toString(), vars.get(key));
        }

        sendResponse(httpExchange, tmp);
        httpExchange.close();
    }

    @Override
    public boolean isAvailableForService(ServiceType service) {
        return service == ServiceType.SERVICE_TYPE_MRC;
    }

    @Override
    public void shutdown() {
    }

    /**
     * Returns the textual representation of an operation defined in {@link MRCServiceConstants}.
     * 
     * @param opId
     *            numeric if of an operation.
     * @return The textual representation of the operation or null if the opId does not exist.
     */
    public static String getOpName(int opId) {
        String name = opNames.get(opId);
        return name == null ? null : name;
    }

}
