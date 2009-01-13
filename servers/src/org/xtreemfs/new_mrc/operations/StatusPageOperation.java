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
 * AUTHORS: Björn Kolbeck (ZIB)
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
    public boolean hasArguments() {
        return false;
    }
    
    @Override
    public boolean isAuthRequired() {
        return false;
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
