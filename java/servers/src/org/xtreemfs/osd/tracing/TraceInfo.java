/*
 * Copyright (c) 2008-2015 by Christoph Kleineweber,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.tracing;

import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.operations.ReadOperation;
import org.xtreemfs.osd.operations.TruncateOperation;
import org.xtreemfs.osd.operations.WriteOperation;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD;

/**
* @author Christoph Kleineweber <kleineweber@zib.de>
*/
public class TraceInfo {
    private String operation;
    private String fileId;
    private long reqId;
    private String client;
    private String osd;
    private long timeStamp;
    private long offset;
    private long length;

    public TraceInfo(OSDRequestDispatcher master, OSDRequest req) {
        operation = req.getOperation().getClass().toString();
        fileId = req.getFileId();
        reqId = req.getRequestId();
        client = req.getCapability().getClientIdentity();
        osd = master.getConfig().getUUID().toString();
        timeStamp = TimeSync.getGlobalTime();

        if(req.getOperation() instanceof WriteOperation) {
            OSD.writeRequest args = (OSD.writeRequest) req.getRequestArgs();
            offset = args.getOffset();
            length = req.getRPCRequest().getData().capacity();
        } else if (req.getOperation() instanceof ReadOperation) {
            OSD.readRequest args = (OSD.readRequest) req.getRequestArgs();
            offset = args.getOffset();
            length = req.getRPCRequest().getData().capacity();
        } else if (req.getOperation() instanceof TruncateOperation) {
            OSD.truncateRequest args = (OSD.truncateRequest) req.getRequestArgs();
            offset = args.getNewFileSize();
            length = 0L;
        } else {
            offset = 0L;
            length = 0L;
        }
    }

    public TraceInfo() {
        operation = "";
        fileId = "";
        reqId = 0L;
        client = "";
        osd = "";
        timeStamp = 0L;
        offset = 0L;
        length = 0L;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public long getReqId() {
        return reqId;
    }

    public void setReqId(long reqId) {
        this.reqId = reqId;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public String getOsd() {
        return osd;
    }

    public void setOsd(String osd) {
        this.osd = osd;
    }

    @Override
    public String toString() {
        return operation + "," + fileId + "," + reqId + "," + client + "," + osd + "," + timeStamp + "," + offset +
                ","  + length;
    }
}
