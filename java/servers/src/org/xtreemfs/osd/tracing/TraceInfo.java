/*
 * Copyright (c) 2008-2015 by Christoph Kleineweber,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.tracing;

import com.sun.jdmk.tools.mibgen.Trace;
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
    private String policyConfig;

    public TraceInfo(OSDRequestDispatcher master, OSDRequest req) {
        operation = req.getOperation().getClass().toString();
        if(req.getOperation() instanceof WriteOperation) {
            operation = "w";
        } else if (req.getOperation() instanceof ReadOperation) {
            operation = "r";
        } else if (req.getOperation() instanceof TruncateOperation) {
            operation = "t";
        } else {
            operation = req.getOperation().getClass().getName();
        }
        fileId = req.getFileId();
        reqId = req.getRequestId();
        client = req.getCapability().getClientIdentity();
        osd = master.getConfig().getUUID().toString();
        timeStamp = TimeSync.getGlobalTime();
        policyConfig = req.getCapability().getTraceConfig().getTracingPolicyConfig();

        if(req.getOperation() instanceof WriteOperation) {
            OSD.writeRequest args = (OSD.writeRequest) req.getRequestArgs();
            offset = args.getObjectNumber() *
                    (long)args.getFileCredentials().getXlocs().getReplicas(0).getStripingPolicy().getStripeSize() +
                    (long) args.getOffset();
            length = req.getRPCRequest().getData().capacity();
        } else if (req.getOperation() instanceof ReadOperation) {
            OSD.readRequest args = (OSD.readRequest) req.getRequestArgs();
            offset = args.getObjectNumber() *
                    (long)args.getFileCredentials().getXlocs().getReplicas(0).getStripingPolicy().getStripeSize() +
                    (long) args.getOffset();
            length = args.getLength();
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

    public TraceInfo(String traceInfoString) throws IllegalArgumentException {
        String[] args = traceInfoString.split(",");
        if(args.length != 8) {
            throw new IllegalArgumentException("Invalid number of arguments");
        } else {
            operation = args[0];
            fileId = args[1];
            reqId = Long.parseLong(args[2]);
            client = args[3];
            osd = args[4];
            timeStamp = Long.parseLong(args[5]);
            offset = Long.parseLong(args[6]);
            length = Long.parseLong(args[7]);
        }
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TraceInfo traceInfo = (TraceInfo) o;

        if (reqId != traceInfo.reqId) return false;
        if (timeStamp != traceInfo.timeStamp) return false;
        if (offset != traceInfo.offset) return false;
        if (length != traceInfo.length) return false;
        if (!operation.equals(traceInfo.operation)) return false;
        if (!fileId.equals(traceInfo.fileId)) return false;
        if (!client.equals(traceInfo.client)) return false;
        return osd.equals(traceInfo.osd);

    }

    @Override
    public int hashCode() {
        int result = operation.hashCode();
        result = 31 * result + fileId.hashCode();
        result = 31 * result + (int) (reqId ^ (reqId >>> 32));
        result = 31 * result + client.hashCode();
        result = 31 * result + osd.hashCode();
        result = 31 * result + (int) (timeStamp ^ (timeStamp >>> 32));
        result = 31 * result + (int) (offset ^ (offset >>> 32));
        result = 31 * result + (int) (length ^ (length >>> 32));
        return result;
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

    public String getPolicyConfig() {
        return policyConfig;
    }

    public void setPolicyConfig(String policyConfig) {
        this.policyConfig = policyConfig;
    }
}
