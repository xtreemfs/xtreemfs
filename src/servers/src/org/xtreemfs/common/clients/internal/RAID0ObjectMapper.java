/*  Copyright (c) 2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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

package org.xtreemfs.common.clients.internal;

import java.util.LinkedList;
import java.util.List;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.xloc.Replica;
import org.xtreemfs.interfaces.StripingPolicy;

/**
 *
 * @author bjko
 */
public class RAID0ObjectMapper extends ObjectMapper {

    private final int stripeSize;

    protected RAID0ObjectMapper(StripingPolicy fileSP) {
        super(fileSP);
        stripeSize = fileSP.getStripe_size()*1024;
    }

    @Override
    public List<ObjectRequest> readRequest(int length, long fileOffset, Replica replica) {
        List<ObjectRequest> reqs = new LinkedList();

        final long firstObj = fileOffset / stripeSize;
        final long lastObj = (fileOffset+length-1) / stripeSize;
        final int firstOffset = (int)fileOffset%stripeSize;


        if (firstObj == lastObj) {
            ObjectRequest rq = new ObjectRequest(firstObj, firstOffset, length,
                    getOSDForObject(replica, firstObj), null);
            reqs.add(rq);
            return reqs;
        }

        //first obj

        ObjectRequest rq = new ObjectRequest(firstObj, firstOffset, stripeSize-firstOffset,
                   getOSDForObject(replica, firstObj),null);
        reqs.add(rq);

        for (long o = firstObj+1; o < lastObj; o++) {
            rq = new ObjectRequest(o, 0, stripeSize,
                    getOSDForObject(replica, o), null);
            if (Logging.isDebug() && (rq.getLength() == 0)) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this,"warning: created empty read/write: "+rq);
            }
            reqs.add(rq);
        }

        //last obj
        final int lastSize = ((length+fileOffset)%stripeSize == 0) ? stripeSize : (int) (length+fileOffset)%stripeSize;
        if (lastSize > 0) {
            rq = new ObjectRequest(lastObj, 0, lastSize, getOSDForObject(replica, lastObj),
                    null);
            reqs.add(rq);
        }
        
        return reqs;
    }

    @Override
    public List<ObjectRequest> writeRequest(ReusableBuffer data, long fileOffset, Replica replica) {
        List<ObjectRequest> reqs = readRequest(data.remaining(), fileOffset, replica);

        int pCnt = 0;
        for (ObjectRequest rq : reqs) {
            ReusableBuffer viewBuf = data.createViewBuffer();
            viewBuf.range(pCnt, rq.getLength());
            pCnt += rq.getLength();
            rq.setData(viewBuf);
        }
        return reqs;
    }

    protected String getOSDForObject(Replica replica, long objNo) {
        return replica.getOSDForObject(objNo).toString();
    }


}
