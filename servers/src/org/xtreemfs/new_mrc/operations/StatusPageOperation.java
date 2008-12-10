/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.new_mrc.operations;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.pinky.HTTPUtils.DATA_TYPE;
import org.xtreemfs.new_mrc.MRCRequest;
import org.xtreemfs.new_mrc.MRCRequestDispatcher;

/**
 *
 * @author bjko
 */
public class StatusPageOperation extends MRCOperation {
    
    public static final String RPC_NAME = "";
    
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
            STATCOLLECT("<!-- $STATCOLLECT -->"),
            DISKFREE("<!-- $DISKFREE -->");

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
                "/org/xtreemfs/new_mrc/templates/status.html");
            if (is == null)
                is = this.getClass().getClassLoader().getResourceAsStream(
                    "org/xtreemfs/new_mrc/templates/status.html");
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
    }
    
    @Override
    public void startRequest(MRCRequest rq) {
        rq.setData(ReusableBuffer.wrap(getStatusPage().getBytes()));
        rq.setDataType(DATA_TYPE.HTML);
        finishRequest(rq);
    }
    
    public String getStatusPage() {
        
        Map<Vars,String> vars = master.getStatusInformation();
        String tmp = statusPageTemplate;
        for (Vars key : vars.keySet()) {
            tmp = tmp.replace(key.toString(), vars.get(key));
        }
        return tmp;
        
    }

}
