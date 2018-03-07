/*
 * Copyright (c) 2009 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.clients.internal;

import java.util.LinkedList;
import java.util.List;
import org.xtreemfs.common.xloc.Replica;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;

/**
 *
 * @author bjko
 */
public class RAID0ObjectMapper extends ObjectMapper {

    private final int stripeSize;

    protected RAID0ObjectMapper(StripingPolicy fileSP) {
        super(fileSP);
        stripeSize = fileSP.getStripeSize()*1024;
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
