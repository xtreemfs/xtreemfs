/*
 * Copyright (c) 2008-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.operations;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceConstants;

/**
 * 
 * @author bjko
 */
public class StatusPageOperation extends MRCOperation {
    
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
    
    protected final String statusPageTemplate;
    
    public StatusPageOperation(MRCRequestDispatcher master) {
        super(master);
        
        StringBuffer sb = null;
        try {
            InputStream is = this.getClass().getClassLoader()
                    .getResourceAsStream("/org/xtreemfs/mrc/templates/status.html");
            if (is == null)
                is = this.getClass().getClassLoader().getResourceAsStream("org/xtreemfs/mrc/templates/status.html");
            if (is == null)
                is = this.getClass().getResourceAsStream("../templates/status.html");
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            sb = new StringBuffer();
            String line = br.readLine();
            while (line != null) {
                sb.append(line + "\n");
                line = br.readLine();
            }
            br.close();
        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        }
        if (sb == null) {
            statusPageTemplate = "<H1>Template was not found, unable to show status page!</h1>";
        } else {
            statusPageTemplate = sb.toString();
        }
    }
    
    @Override
    public void startRequest(MRCRequest rq) {
        // ignore
    }
    
    public String getStatusPage() {
        
        Map<Vars, String> vars = master.getStatusInformation();
        String tmp = statusPageTemplate;
        for (Vars key : vars.keySet()) {
            tmp = tmp.replace(key.toString(), vars.get(key));
        }
        return tmp;
    }
    
    public String getDBInfo() {
        
        StringBuilder sb = new StringBuilder();
        sb.append("<HTML><BODY><H1>BABUDB STATE</H1>");
        
        Map<String, Object> dbStatus = master.getDBStatus();
        if (dbStatus == null) {
            sb.append("BabuDB has not yet been initialized.");
        }
        
        else {
            sb.append("<TABLE>");
            Map<String, Object> status = new TreeMap<String, Object>(dbStatus);
            for (Entry<String, Object> entry : status.entrySet()) {
                sb.append("<TR><TD STYLE=\"text-align:right; font-style:italic\">");
                sb.append(entry.getKey());
                sb.append(":</TD><TD STYLE=\"font-weight:bold\">");
                sb.append(entry.getValue());
                sb.append("</TD></TR>");
            }
            sb.append("</TABLE>");
        }
        
        sb.append("</BODY></HTML>");
        
        return sb.toString();
    }
    
    public static String getOpName(int opId) {
        String name = opNames.get(opId);
        return name == null ? null : name;
    }
    
}
