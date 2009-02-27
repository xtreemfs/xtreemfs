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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.xtreemfs.common.auth.AuthenticationProvider;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.util.OutputUtils;
import org.xtreemfs.foundation.LifeCycleListener;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.pinky.HTTPUtils;
import org.xtreemfs.foundation.pinky.PinkyRequest;
import org.xtreemfs.foundation.pinky.PinkyRequestListener;
import org.xtreemfs.foundation.pinky.PipelinedPinky;
import org.xtreemfs.foundation.pinky.SSLOptions;

/**
 * This class comtains the workflow of the MRC server and directs the requestst
 * to the appropriate stages
 * 
 * @author bjko
 */
public class RequestController implements PinkyRequestListener, DIRRequestListener, LifeCycleListener {
    
    private PipelinedPinky  pinkyStage;
    
    private DirServiceStage dirServiceStage;
    
    private DIRConfig       config;
    
    private int             _stat_numRequests;
    
    private final String    statusPageTemplate;
    
    private enum Vars {
            MAXMEM("<!-- $MAXMEM -->"),
            FREEMEM("<!-- $FREEMEM -->"),
            AVAILPROCS("<!-- $AVAILPROCS -->"),
            BPSTATS("<!-- $BPSTATS -->"),
            PORT("<!-- $PORT -->"),
            DEBUG("<!-- $DEBUG -->"),
            NUMCON("<!-- $NUMCON -->"),
            PINKYQ("<!-- $PINKYQ -->"),
            NUMREQS("<!-- $NUMREQS -->"),
            TIME("<!-- $TIME -->"),
            TABLEDUMP("<!-- $TABLEDUMP -->");
        
        private String template;
        
        Vars(String template) {
            this.template = template;
        }
        
        public String toString() {
            return template;
        }
    }
    
    /** Creates a new instance of RequestController */
    public RequestController(DIRConfig config) throws Exception {
        
        try {
            this.config = config;
            
            final AuthenticationProvider auth = (AuthenticationProvider) Class.forName(
                config.getAuthenticationProvider()).newInstance();
            auth.initialize(config.isUsingSSL());
            
            /** set up all stages */
            
            dirServiceStage = new DirServiceStage(config.getDbDir(), auth);
            dirServiceStage.setRequestListener(this);
            dirServiceStage.setLifeCycleListener(this);
            
            pinkyStage = config.isUsingSSL() ? new PipelinedPinky(config.getPort(), config.getAddress(),
                this, new SSLOptions(new FileInputStream(config.getServiceCredsFile()), config
                        .getServiceCredsPassphrase(), config.getServiceCredsContainer(), new FileInputStream(
                    config.getTrustedCertsFile()), config.getTrustedCertsPassphrase(), config
                        .getTrustedCertsContainer(), false)) : new PipelinedPinky(config.getPort(), config
                    .getAddress(), this);
            pinkyStage.setLifeCycleListener(this);
            
            /** load status page template */
            
            StringBuffer sb = null;
            try {
                InputStream is = this.getClass().getClassLoader().getResourceAsStream(
                    "org/xtreemfs/dir/templates/status.html");
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
                Logging.logMessage(Logging.LEVEL_DEBUG, this, ex);
            }
            if (sb == null) {
                statusPageTemplate = "<H1>Template was not found, unable to show status page!</h1>";
            } else {
                statusPageTemplate = sb.toString();
            }
            
            Logging.logMessage(Logging.LEVEL_INFO, this, "[ I | DIR ] operational, listening on port "
                + config.getPort());
            
        } catch (Exception exc) {
            Logging.logMessage(Logging.LEVEL_ERROR, this, exc);
            shutdown();
            throw exc;
        }
    }
    
    public void startup() throws Exception {
        
        try {
            dirServiceStage.start();
            pinkyStage.start();
            
            pinkyStage.waitForStartup();
            dirServiceStage.waitForStartup();
            
        } catch (Exception exc) {
            shutdown();
            throw exc;
        }
    }
    
    public void shutdown() throws Exception {
        
        if (pinkyStage != null)
            pinkyStage.shutdown();
        if (dirServiceStage != null)
            dirServiceStage.shutdown();
        
        if (pinkyStage != null)
            pinkyStage.waitForShutdown();
        if (dirServiceStage != null)
            dirServiceStage.waitForShutdown();
    }
    
    // --------------------- LISTENERS ------------------------------
    
    public void receiveRequest(PinkyRequest theRequest) {
        
        DIRRequest rq = new DIRRequest(theRequest);
        
        try {
            if (theRequest.requestURI.charAt(0) == '/') {
                
                if (theRequest.requestURI.length() == 1) {
                    
                    // generate status HTTP page
                    String statusPage = getStatusPage();
                    
                    ReusableBuffer bbuf = ReusableBuffer.wrap(statusPage.getBytes(HTTPUtils.ENC_ASCII));
                    theRequest.setResponse(HTTPUtils.SC_OKAY, bbuf, HTTPUtils.DATA_TYPE.HTML);
                    pinkyStage.sendResponse(rq.getPinkyRequest());
                    return;
                    
                } else
                    theRequest.requestURI = theRequest.requestURI.substring(1);
            }
            
            if (theRequest.requestURI.length() > 0) {
                if (theRequest.requestURI.charAt(0) == '.') {
                    // system command
                    handleSystemCall(rq);
                } else {
                    try {
                        // delegate to the Directory Service Stage
                        dirServiceStage.processRequest(rq);
                    } catch (IllegalStateException e) {
                        // queue is full
                        theRequest.setClose(true);
                        theRequest.setResponse(HTTPUtils.SC_SERV_UNAVAIL);
                        pinkyStage.sendResponse(theRequest);
                    }
                    
                }
            } else {
                theRequest.setClose(true);
                theRequest.setResponse(HTTPUtils.SC_BAD_REQUEST);
                pinkyStage.sendResponse(theRequest);
            }
            
        } catch (IndexOutOfBoundsException e) {
            theRequest.setClose(true);
            theRequest.setResponse(HTTPUtils.SC_BAD_REQUEST);
            pinkyStage.sendResponse(theRequest);
        } catch (Exception e) {
            theRequest.setClose(true);
            theRequest.setResponse(HTTPUtils.SC_SERVER_ERROR);
            pinkyStage.sendResponse(theRequest);
        }
        
    }
    
    public void handleSystemCall(DIRRequest rq) {
        try {
            if (rq.getPinkyRequest().requestURI.equals(".shutdown")) {
                // shutdown the whole Directory Service
                rq.getPinkyRequest().setResponse(HTTPUtils.SC_OKAY);
                pinkyStage.sendResponse(rq.getPinkyRequest());
                shutdown();
            } else {
                rq.getPinkyRequest().setResponse(HTTPUtils.SC_NOT_IMPLEMENTED);
                pinkyStage.sendResponse(rq.getPinkyRequest());
            }
            
        } catch (IOException ex) {
            Logging.logMessage(Logging.LEVEL_ERROR, this, ex);
        } catch (Exception ex) {
            Logging.logMessage(Logging.LEVEL_ERROR, this, ex);
        }
    }
    
    public void dsRequestDone(DIRRequest rq) {
        if (rq.getPinkyRequest().isReady()) {
            pinkyStage.sendResponse(rq.getPinkyRequest());
            _stat_numRequests++;
        }
    }
    
    private String getStatusPage() throws SQLException, JSONException {
        
        assert (statusPageTemplate != null);
        
        long time = System.currentTimeMillis();
        
        Object[] dbDump = getDBDump();
        
        Map<String, Map<String, String>> entities = (Map<String, Map<String, String>>) dbDump[0];
        Map<String, Object[]> mappings = (Map<String, Object[]>) dbDump[1];
        
        StringBuilder dump = new StringBuilder();
        dump
                .append("<br><table width=\"100%\" frame=\"box\"><td colspan=\"2\" class=\"heading\">Address Mapping</td>");
        dump.append("<tr><td class=\"dumpTitle\">UUID</td><td class=\"dumpTitle\">mapping</td></tr>");
        for (String uuid : mappings.keySet()) {
            Object[] entry = mappings.get(uuid);
            List<Map<String, Object>> mapping = (List<Map<String, Object>>) entry[1];
            
            dump.append("<tr><td class=\"uuid\">");
            dump.append(uuid);
            dump.append("</td><td class=\"dump\"><table width=\"100%\"><tr>");
            dump.append("<tr><td><table width=\"100%\">");
            for (int i = 0; i < mapping.size(); i++) {
                dump.append("<tr><td class=\"mapping\">");
                Map<String, Object> map = mapping.get(i);
                String endpoint = map.get("protocol") + "://" + map.get("address") + ":" + map.get("port");
                dump.append("<a href=\"" + endpoint + "\">");
                dump.append(endpoint);
                dump.append("</a></td><td class=\"mapping\">");
                dump.append(map.get("match_network"));
                dump.append("</td><td class=\"mapping\">");
                dump.append(map.get("ttl"));
                dump.append("</td></tr>");
            }
            dump.append("</table></td></tr>");
            dump.append("<td class=\"version\">version: <b>");
            dump.append(entry[2]);
            dump.append("</b></td></tr></table>");
        }
        dump.append("</td></tr></table>");
        
        dump
                .append("<br><table width=\"100%\" frame=\"box\"><td colspan=\"2\" class=\"heading\">Data Mapping</td>");
        dump.append("<tr><td class=\"dumpTitle\">UUID</td><td class=\"dumpTitle\">mapping</td></tr>");
        for (String uuid : entities.keySet()) {
            Map<String, String> entry = entities.get(uuid);
            dump.append("<tr><td class=\"uuid\">");
            dump.append(uuid);
            dump.append("</td><td class=\"dump\"><table width=\"100%\">");
            List<String> keys = new LinkedList<String>(entry.keySet());
            Collections.sort(keys);
            for (String key : keys) {
                if (key.equals("version"))
                    continue;
                dump.append("<tr><td width=\"30%\">");
                dump.append(key);
                dump.append("</td><td><b>");
                dump.append(entry.get(key));
                if (key.equals("lastUpdated")) {
                    dump.append(" (");
                    dump.append(new Date(Long.parseLong(entry.get(key)) * 1000));
                    dump.append(")");
                } else if (key.equals("free") || key.equals("total") || key.endsWith("RAM")) {
                    dump.append(" bytes (");
                    dump.append(OutputUtils.formatBytes(Long.parseLong(entry.get(key))));
                    dump.append(")");
                } else if (key.equals("load")) {
                    dump.append("%");
                }
                dump.append("</b></td></tr>");
            }
            dump.append("<td></td><td class=\"version\">version: <b>");
            dump.append(entry.get("version"));
            dump.append("</b></td></table></td></tr>");
        }
        dump.append("</table>");
        
        String tmp = null;
        try {
            tmp = statusPageTemplate.replace(Vars.AVAILPROCS.toString(), Runtime.getRuntime()
                    .availableProcessors()
                + " bytes");
        } catch (Exception e) {
            tmp = statusPageTemplate;
        }
        tmp = tmp.replace(Vars.FREEMEM.toString(), Runtime.getRuntime().freeMemory() + " bytes");
        tmp = tmp.replace(Vars.MAXMEM.toString(), Runtime.getRuntime().maxMemory() + " bytes");
        tmp = tmp.replace(Vars.BPSTATS.toString(), BufferPool.getStatus());
        tmp = tmp.replace(Vars.PORT.toString(), Integer.toString(config.getPort()));
        tmp = tmp.replace(Vars.DEBUG.toString(), Integer.toString(config.getDebugLevel()));
        tmp = tmp.replace(Vars.NUMCON.toString(), Integer.toString(pinkyStage.getNumConnections()));
        tmp = tmp.replace(Vars.PINKYQ.toString(), Integer.toString(pinkyStage.getTotalQLength()));
        tmp = tmp.replace(Vars.NUMREQS.toString(), Integer.toString(_stat_numRequests));
        tmp = tmp.replace(Vars.TIME.toString(), new Date(time).toString() + " (" + time + ")");
        tmp = tmp.replace(Vars.TABLEDUMP.toString(), dump.toString());
        
        return tmp;
        
    }
    
    public void crashPerformed() {
        try {
            shutdown();
        } catch (Exception e) {
            Logging.logMessage(Logging.LEVEL_ERROR, this, e);
        }
    }
    
    public void shutdownPerformed() {
        // ignore
    }
    
    public void startupPerformed() {
        // ignore
    }
    
    private Object[] getDBDump() throws SQLException, JSONException {
        return dirServiceStage.getDBDump();
    }
    
}
