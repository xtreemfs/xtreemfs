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
            LASTRQDATE("<!-- $LASTRQDATE -->"),
            TOTALNUMRQ("<!-- $TOTALNUMRQ -->"),
            RQSTATS("<!-- $RQSTATS -->"),
            VOLUMES("<!-- $VOLUMES -->"),
            UUID("<!-- $UUID -->"),
            AVAILPROCS("<!-- $AVAILPROCS -->"),
            BPSTATS("<!-- $BPSTATS -->"),
            PORT("<!-- $PORT -->"),
            DIRURL("<!-- $DIRURL -->"),
            DEBUG("<!-- $DEBUG -->"),
            NUMCON("<!-- $NUMCON -->"),
            PINKYQ("<!-- $PINKYQ -->"),
            PROCQ("<!-- $PROCQ -->"),
            GLOBALTIME("<!-- $GLOBALTIME -->"),
            GLOBALRESYNC("<!-- $GLOBALRESYNC -->"),
            LOCALTIME("<!-- $LOCALTIME -->"),
            LOCALRESYNC("<!-- $LOCALRESYNC -->"),
            MEMSTAT("<!-- $MEMSTAT -->"),
            UUIDCACHE("<!-- $UUIDCACHE -->"),
            DISKFREE("<!-- $DISKFREE -->"),
            PROTOVERSION("<!-- $PROTOVERSION -->"),
            VERSION("<!-- $VERSION -->"),
            DBVERSION("<!-- $DBVERSION -->");
        
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
            InputStream is = this.getClass().getClassLoader().getResourceAsStream(
                "/org/xtreemfs/mrc/templates/status.html");
            if (is == null)
                is = this.getClass().getClassLoader().getResourceAsStream(
                    "org/xtreemfs/mrc/templates/status.html");
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
        
        // TODO
        
        // rq.setData(ReusableBuffer.wrap(getStatusPage().getBytes()));
        // rq.setDataType(DATA_TYPE.HTML);
        // finishRequest(rq);
    }
    
    public String getStatusPage() {
        
        Map<Vars, String> vars = master.getStatusInformation();
        String tmp = statusPageTemplate;
        for (Vars key : vars.keySet()) {
            tmp = tmp.replace(key.toString(), vars.get(key));
        }
        return tmp;
    }
    
    public static String getOpName(int opId) {
        String name = opNames.get(opId);
        return name == null ? null : name;
    }
    
}
