/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin and
 Consiglio Nazionale delle Ricerche.

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
 * AUTHORS: Jan Stender (ZIB), Björn Kolbeck (ZIB), Eugenio Cesario (CNR), Christian Lorenz (ZIB), Felix Langner (ZIB)
 */

package org.xtreemfs.osd.stages;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xtreemfs.common.auth.NullAuthProvider;
import org.xtreemfs.common.buffer.ASCIIString;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.clients.RPCClient;
import org.xtreemfs.common.clients.RPCResponse;
import org.xtreemfs.common.clients.RPCResponseListener;
import org.xtreemfs.common.clients.dir.DIRClient;
import org.xtreemfs.common.clients.mrc.MRCClient;
import org.xtreemfs.common.clients.osd.ConcurrentFileMap;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.striping.StripeInfo;
import org.xtreemfs.common.striping.StripingPolicy;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.pinky.HTTPHeaders;
import org.xtreemfs.foundation.pinky.HTTPUtils;
import org.xtreemfs.foundation.pinky.PinkyRequest;
import org.xtreemfs.foundation.pinky.HTTPUtils.DATA_TYPE;
import org.xtreemfs.foundation.speedy.SpeedyRequest;
import org.xtreemfs.foundation.speedy.SpeedyResponseListener;
import org.xtreemfs.osd.ErrorRecord;
import org.xtreemfs.osd.OSDException;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.RequestDetails;
import org.xtreemfs.osd.RequestDispatcher;
import org.xtreemfs.osd.ErrorRecord.ErrorClass;
import org.xtreemfs.osd.RequestDispatcher.Stages;
import org.xtreemfs.osd.stages.Stage.StageMethod;
import org.xtreemfs.osd.storage.FileInfo;
import org.xtreemfs.osd.storage.MetadataCache;
import org.xtreemfs.osd.storage.StorageLayout;
import org.xtreemfs.osd.storage.Striping;
import org.xtreemfs.osd.storage.Striping.RPCMessage;
import org.xtreemfs.osd.storage.Striping.UDPMessage;

public class StorageThread extends Stage {

    public static final int   STAGEOP_READ_OBJECT              = 1;

    public static final int   STAGEOP_READ_OBJECT_GMAX_FETCHED = 2;

    public static final int   STAGEOP_WRITE_OBJECT             = 3;

    public static final int   STAGEOP_FETCH_GMAX               = 4;

    public static final int   STAGEOP_PROCESS_GMAX_EVENT       = 5;

    public static final int   STAGEOP_TRUNCATE                 = 6;

    public static final int   STAGEOP_TRUNCATE_ACKS_RECEIVED   = 7;

    public static final int   STAGEOP_TRUNCATE_LOCAL           = 8;

    public static final int   STAGEOP_CLEAN_UP                 = 9;
    
    public static final int   STAGEOP_CLEAN_UP2                = 10;

    public static final int   STAGEOP_LOCAL_READ_OBJECT        = 11;

    private MetadataCache     cache;

    private StorageLayout     layout;

    private Striping          striping;

    private RequestDispatcher master;


    public StorageThread(int id, RequestDispatcher dispatcher, Striping striping,
        MetadataCache cache, StorageLayout layout) {

        super("OSD Storage Thread " + id);

        this.cache = cache;
        this.layout = layout;
        this.master = dispatcher;
        this.striping = striping;
    }

    @Override
    protected void processMethod(StageMethod method) {

        try {

            switch (method.getStageMethod()) {
            case STAGEOP_READ_OBJECT:
                if (processRead(method))
                    methodExecutionSuccess(method, StageResponseCode.OK);
                break;
            case STAGEOP_READ_OBJECT_GMAX_FETCHED:
                processReadGmaxFetched(method);
                methodExecutionSuccess(method, StageResponseCode.OK);
                break;
            case STAGEOP_WRITE_OBJECT:
                processWrite(method);
                methodExecutionSuccess(method, StageResponseCode.OK);
                break;
            case STAGEOP_FETCH_GMAX:
                processFetchGmax(method);
                methodExecutionSuccess(method, StageResponseCode.OK);
                break;
            case STAGEOP_PROCESS_GMAX_EVENT:
                processGmaxEvent(method);
                methodExecutionSuccess(method, StageResponseCode.OK);
                break;
            case STAGEOP_TRUNCATE:
                if (processTruncate(method, false))
                    methodExecutionSuccess(method, StageResponseCode.OK);
                break;
            case STAGEOP_TRUNCATE_ACKS_RECEIVED:
                processTruncateAcksReceived(method);
                methodExecutionSuccess(method, StageResponseCode.OK);
                break;
            case STAGEOP_TRUNCATE_LOCAL:
                processTruncate(method, true);
                methodExecutionSuccess(method, StageResponseCode.OK);
                break;
            case STAGEOP_CLEAN_UP:
                processCleanUp1(method); 
                break;
            case STAGEOP_CLEAN_UP2:
                processCleanUp2(method); 
                break;
            case STAGEOP_LOCAL_READ_OBJECT:
                processLocalReadObject(method); 
                methodExecutionSuccess(method, StageResponseCode.OK);
                break;
            }

        } catch (OSDException exc) {
            methodExecutionFailed(method, exc.getErrorRecord());
        } catch (Throwable th) {
            methodExecutionFailed(method, new ErrorRecord(
                ErrorRecord.ErrorClass.INTERNAL_SERVER_ERROR, "an internal error has occurred", th));
        }
    }

    private boolean processRead(StageMethod rq) throws IOException, JSONException {

        final RequestDetails details = rq.getRq().getDetails();

        final String fileId = details.getFileId();
        final long objNo = details.getObjectNumber();
        final StripingPolicy sp = details.getCurrentReplica().getStripingPolicy();
        final long stripeSize = sp.getStripeSize(objNo);
        final long[] range = { details.getByteRangeStart(), details.getByteRangeEnd() };
        final FileInfo fi = layout.getFileInfo(sp, fileId);

        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "READ: " + fileId + "-" + objNo + ".");

        int objVer = fi.getObjectVersion(objNo);
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "getting objVer " + objVer);

        String objChksm = fi.getObjectChecksum(objNo);
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "checksum is " + objChksm);

        // retrieve the object from the storage layout
        ReusableBuffer data = layout.readObject(fileId, objNo, objVer, objChksm, sp, sp.getOSDByObject(objNo));

        // test the checksum
        if (!layout.checkObject(data, objChksm)) {
            Logging.logMessage(Logging.LEVEL_WARN, this, "invalid checksum: file=" + fileId
                + ", objNo=" + objNo);
            details.setInvalidChecksum(true);
        }

        // object exists locally ...
        if (data.capacity() > 0) {
            if(!checkReadObject(rq, data)){
                rq.getRq().setData(data, HTTPUtils.DATA_TYPE.BINARY);
                sendGmaxRequests(rq);
                return false;
            }
            return true;
        }

        // object does not exist locally
        else {
            // check if the object is a 'hole' or an EOF

            if (Logging.tracingEnabled())
                Logging.logMessage(Logging.LEVEL_TRACE, this, fileId + "-" + objNo
                    + " not found locally");

            if (objNo < fi.getLastObjectNumber()) {
                // hole
                data = padWithZeros(data, (int) stripeSize);
                if (details.isRangeRequested())
                    data.range((int) range[0], (int) (range[1] - range[0]) + 1);

                setReadResponse(rq, data);
                return true;

            } else {

                // in the non-striped case, it must be an EOF
                if (sp.getWidth() == 1) {
                    setReadResponse(rq, data);
                    return true;
                }

                // in the striped case, it may be either a hole or an EOF
                // fetch globalMax
                else {
                    rq.getRq().setData(data, HTTPUtils.DATA_TYPE.BINARY);
                    sendGmaxRequests(rq);
                    return false;
                }
            }

        }

    }

    /**
     * @param rq
     * @param data
     * @return
     * @throws IOException
     * @throws JSONException
     */
    private boolean checkReadObject(StageMethod rq, ReusableBuffer data)
            throws IOException, JSONException {
        final RequestDetails details = rq.getRq().getDetails();
    
        final String fileId = details.getFileId();
        final long objNo = details.getObjectNumber();
        final StripingPolicy sp = details.getCurrentReplica().getStripingPolicy();
        final long stripeSize = sp.getStripeSize(objNo);
        final long[] range = { details.getByteRangeStart(), details.getByteRangeEnd() };
        final FileInfo fi = layout.getFileInfo(sp, fileId);
    
        if (Logging.tracingEnabled())
            Logging.logMessage(Logging.LEVEL_TRACE, this, fileId + "-" + objNo
                + " found locally");
    
        // check whether the object is complete
        if (data.capacity() < stripeSize) {
            // object incomplete
    
            if (Logging.tracingEnabled())
                Logging.logMessage(Logging.LEVEL_TRACE, this, fileId + "-" + objNo
                    + " incomplete");
    
            if (objNo < fi.getLastObjectNumber()) {
                // object known to be incomplete: fill object with
                // padding zeros
                data = padWithZeros(data, (int) sp.getStripeSize(objNo));
                if (details.isRangeRequested())
                    data.range((int) range[0], (int) (range[1] - range[0]) + 1);
    
                setReadResponse(rq, data);
                return true;
            }
    
            else {
                // not sure in striped case whether object is complete
                // or not
    
                // if the read does not go beyond the object size,
                // return the data immediately
                if (details.isRangeRequested() && data.capacity() >= range[1]) {
                    data.range((int) range[0], (int) (range[1] - range[0]) + 1);
                    setReadResponse(rq, data);
                    return true;
                }
    
                // otherwise, fetch globalMax if necessary
                else {
    
                    // return the data in the non-striped case
                    if (sp.getWidth() == 1) {
    
                        if (details.isRangeRequested()) {
                            final int rangeSize = (int) (range[1] - range[0]) + 1;
                            if (data.capacity() >= rangeSize)
                                data.range((int) range[0], rangeSize);
                            else {
                                if (Logging.isDebug())
                                    Logging.logMessage(Logging.LEVEL_DEBUG, this,
                                        "read beyond EOF: " + rq.getRq().getPinkyRequest());
                                final int eofLength = data.capacity() - ((int) range[0]);
                                data.range((int) range[0], eofLength);
                            }
                        }
    
                        setReadResponse(rq, data);
                        return true;
                    }
    
                    // fetch globalMax otherwise
                    else {
                        // rq.getRq().setData(data, HTTPUtils.DATA_TYPE.BINARY);
                        // sendGmaxRequests(rq);
                        return false;
                    }
                }
            }
    
        } else {
            // object is complete
    
            if (Logging.tracingEnabled())
                Logging.logMessage(Logging.LEVEL_TRACE, this, fileId + "-" + objNo
                    + " complete");
    
            if (details.isRangeRequested())
                data.range((int) range[0], (int) (range[1] - range[0]) + 1);
    
            setReadResponse(rq, data);
            return true;
        }
    }

    private void processReadGmaxFetched(StageMethod rq) throws IOException {

        // update globalMax from all "globalMax" responses
        striping.processGmaxResponses(rq.getRq());

        final RequestDetails details = rq.getRq().getDetails();
        final String fileId = details.getFileId();
        final long objNo = details.getObjectNumber();
        final StripingPolicy sp = details.getCurrentReplica().getStripingPolicy();
        final ReusableBuffer data = rq.getRq().getData();
        final FileInfo fi = layout.getFileInfo(sp, fileId);

        // object exists: padding or no padding
        if (data.capacity() > 0) {

            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "object " + objNo
                    + " exists locally...");

            // if last object, send partial object
            if (objNo == fi.getLastObjectNumber()) {

                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, this, "object " + objNo
                        + " is the last object");

                setReadResponse(rq, data);
            }

            // if not last object, send padded object
            else {
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "object " + objNo
                    + " not the last object");

                rq.getRq().setData(padWithZeros(data, (int) sp.getStripeSize(objNo)),
                    HTTPUtils.DATA_TYPE.BINARY);

                if (details.isRangeRequested())
                    data.range((int) details.getByteRangeStart(),
                        (int) (details.getByteRangeEnd() - details.getByteRangeStart()) + 1);

                setReadResponse(rq, data);
            }

        }

        // object does not exist: padding or EOF
        else {

            Logging.logMessage(Logging.LEVEL_DEBUG, this, "object " + objNo
                + " does not exist locally");

            // if hole, send padding object
            if (objNo <= fi.getLastObjectNumber()) {

                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, this, "object " + objNo
                        + " is a hole, return zero-padded object");

                ReusableBuffer paddedData = padWithZeros(data, (int) sp.getStripeSize(objNo));
                long bytes = paddedData.capacity();
                setReadResponse(rq, paddedData);
                master.getStatistics().bytesTX.addAndGet(bytes);
            }

            // otherwise, send EOF
            else {
                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, this, "object " + objNo
                        + " is out of range, return null object (EOF)");

                setReadResponse(rq, data);
            }

        }
    }

    private void processWrite(StageMethod rq) throws IOException {

        final RequestDetails details = rq.getRq().getDetails();

        final String fileId = details.getFileId();
        final StripingPolicy sp = details.getCurrentReplica().getStripingPolicy();
        final long objNo = details.getObjectNumber();
        final FileInfo fi = layout.getFileInfo(sp, fileId);
        final String checksum = fi.getObjectChecksum(objNo);
        final PinkyRequest pr = rq.getRq().getPinkyRequest();

        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "WRITE: " + fileId + "-" + objNo + "."
                + " last objNo=" + fi.getLastObjectNumber());



        try {

            // determine obj version to write
            int currentV = Math.max(fi.getObjectVersion(objNo), details
                    .isObjectVersionNumberRequested() ? details.getObjectVersionNumber() : 0);
            if (currentV == 0)
                currentV++;
            int nextV = currentV;
            int offset = 0;

            if (details.isRangeRequested())
                offset = (int) details.getByteRangeStart();

            ReusableBuffer writeData = pr.requestBody;
            assert(writeData != null);
            if (StatisticsStage.collect_statistics) {
                master.getStatistics().bytesRX.addAndGet(writeData.capacity());
                master.getStatistics().numWrites.incrementAndGet();
            }

            if (details.getCowPolicy().isCOW((int)objNo)) {
                nextV++;

                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, this, "incremented version: " + fileId
                        + "-" + objNo + "." + nextV);
                // increment version number and copy old object, if only a range
                // is written
                // otherwise simply write data to new object version
                if (details.isRangeRequested()) {

                    // load old object and overwrite with range
                    ReusableBuffer oldObj = layout.readObject(fileId, objNo, currentV, checksum,
                        sp, 0l);

                    // test the checksum
                    if (!layout.checkObject(oldObj, checksum)) {
                        Logging.logMessage(Logging.LEVEL_WARN, this, "invalid checksum: file="
                            + fileId + ", objNo=" + objNo);
                        BufferPool.free(oldObj);
                        throw new OSDException(ErrorClass.INTERNAL_SERVER_ERROR, "invalid checksum");
                    }

                    // if the old objct does not have sufficient capacity,
                    // enlarge it
                    if (oldObj.capacity() < details.getByteRangeEnd() + 1) {

                        if (Logging.isDebug())
                            Logging.logMessage(Logging.LEVEL_DEBUG, this,
                                "extend object buffer and copy");

                        // allocate a new buffer and copy the old object into
                        // the buffer
                        ReusableBuffer tmp = BufferPool
                                .allocate((int) details.getByteRangeEnd() + 1);
                        writeData.position(0);
                        oldObj.position(0);
                        tmp.put(oldObj);

                        // pad the space between the old object and the written
                        // byte range with zeros
                        while (tmp.position() < details.getByteRangeStart())
                            tmp.put((byte) 0);
                        tmp.position((int) details.getByteRangeStart());

                        tmp.put(writeData);
                        BufferPool.free(oldObj);
                        writeData = tmp;
                    } else {
                        if (Logging.isDebug())
                            Logging.logMessage(Logging.LEVEL_DEBUG, this, "overwrite old data");
                        oldObj.position((int) details.getByteRangeStart());
                        writeData.position(0);
                        oldObj.put(writeData);
                        writeData = oldObj;
                        writeData.position(0);
                    }
                    // we generated the whole object...
                    offset = 0;
                }
                details.getCowPolicy().objectChanged((int)objNo);
            }

            if (details.isObjectVersionNumberRequested()) {
                // update push with version number
                assert (details.getObjectVersionNumber() >= currentV) : "local version no: "
                    + currentV + ", latest version no:" + details.getObjectVersionNumber()
                    + ", current version no:" + fi.getObjectVersion(objNo) + ", req="
                    + rq.getRq().getPinkyRequest();
                nextV = details.getObjectVersionNumber();
            }

            writeData.position(0);
            layout.writeObject(fileId, objNo, writeData, nextV, offset, checksum, sp, sp.getOSDByObject(objNo));
            String newChecksum = layout.createChecksum(fileId, objNo, writeData.capacity() == sp
                    .getStripeSize(objNo) ? writeData : null, nextV, checksum);

            // if a new buffer had to be allocated for writing the object, free
            // it now (the request body will be freed automatically)
            if (writeData != pr.requestBody)
                BufferPool.free(writeData);

            fi.getObjVersions().put(objNo, nextV);
            fi.getObjChecksums().put(objNo, newChecksum);

            details.setObjectVersionNumber(nextV);

            // if the write refers to the last known object or to an object
            // beyond, i.e. the file size and globalMax are potentially
            // affected:
            if (objNo >= fi.getLastObjectNumber()) {

                long newObjSize = pr.requestBody.capacity();
                if (details.isRangeRequested())
                    newObjSize += details.getByteRangeStart();

                // calculate new filesize...
                long newFS = 0;
                if (objNo > 0) {
                    newFS = sp.getLastByte(objNo - 1) + 1 + newObjSize;
                } else {
                    newFS = newObjSize;
                }

                // check whether the file size might have changed; in this case,
                // ensure that the X-New-Filesize header will be set
                if (newFS > fi.getFilesize() && objNo >= fi.getLastObjectNumber()) {
                    // Metadata meta = info.getMetadata();
                    // meta.putKnownSize(newFS);
                    Logging.logMessage(Logging.LEVEL_DEBUG, this, "new filesize: " + newFS);
                    details.setNewFSandEpoch(JSONParser.toJSON(new Object[] { newFS,
                        fi.getTruncateEpoch() }));
                } else {
                    Logging.logMessage(Logging.LEVEL_DEBUG, this, "no new filesize: " + newFS + "/"
                        + fi.getFilesize() + ", " + fi.getLastObjectNumber() + "/" + objNo);
                }

                // update file size and last object number
                fi.setFilesize(newFS);
                fi.setLastObjectNumber(objNo);

                // if the written object has a larger ID than the largest
                // locally-known object of the file, send 'globalMax' messages
                // to all other OSDs and update local globalMax
                if (objNo > fi.getLastObjectNumber()) {

                    fi.setLastObjectNumber(objNo);

                    List<UDPMessage> msgs = striping.createGmaxMessages(new ASCIIString(fileId),
                        newFS, objNo, fi.getTruncateEpoch(), details.getCurrentReplica());

                    for (UDPMessage msg : msgs)
                        master.sendUDP(msg.buf.createViewBuffer(), msg.addr);

                    // one buffer has been allocated, which will not be freed
                    // automatically; this has to be done here
                    BufferPool.free(msgs.get(0).buf);
                }

            }

        } catch (IOException ex) {
            ex.printStackTrace();
            throw ex;
        } catch (JSONException ex) {
            throw new IOException(ex);
        }
    }

    private boolean processTruncate(StageMethod rq, boolean intraOSD) throws IOException,
        JSONException {

        final RequestDetails details = rq.getRq().getDetails();

        // for the sake of robustness, check if contacted OSD is head OSD if the
        // truncate operation is not local
        if (!intraOSD && !master.isHeadOSD(details.getCurrentReplica()))
            throw new OSDException(ErrorClass.REDIRECT, details.getCurrentReplica().getOSDs()
                    .get(0).toString());

        final String fileId = details.getFileId();
        final long fileSize = details.getTruncateFileSize();
        final StripingPolicy sp = details.getCurrentReplica().getStripingPolicy();
        final long epochNumber = details.getCapability().getEpochNo();

        final FileInfo fi = layout.getFileInfo(sp, fileId);

        if (fi.getTruncateEpoch() >= epochNumber)
            throw new OSDException(ErrorClass.USER_EXCEPTION, "invalid truncate epoch for file "
                + fileId + ": " + epochNumber + ", current one is " + fi.getTruncateEpoch());

        // find the offset of the local OSD in the current replica's locations
        // list
        // FIXME: unify OSD IDs
        final int relativeOSDNumber = details.getCurrentReplica().indexOf(
            master.getConfig().getUUID()) + 1;

        long newLastObject = -1;

        if (fileSize == 0) {
            // truncate to zero: remove all objects
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "truncate to 0");
            layout.deleteAllObjects(fileId);
            fi.getObjChecksums().clear();
            fi.getObjVersions().clear();
        }

        else if (fi.getFilesize() > fileSize) {
            // shrink file
            newLastObject = truncateShrink(fileId, fileSize, epochNumber, sp, fi, relativeOSDNumber);
        }

        else if (fi.getFilesize() < fileSize) {
            // extend file
            newLastObject = truncateExtend(fileId, fileSize, epochNumber, sp, fi, relativeOSDNumber);
        }

        else if (fi.getFilesize() == fileSize) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "truncate to local size: " + fileSize);
            newLastObject = fi.getLastObjectNumber();
        }

        // set the new file size and last object number
        fi.setFilesize(fileSize);
        fi.setLastObjectNumber(newLastObject);
        fi.setTruncateEpoch(epochNumber);

        // store the truncate epoch persistently
        layout.setTruncateEpoch(fileId, epochNumber);

        // append the new file size and epoch number to the response
        details.setNewFSandEpoch(JSONParser.toJSON(new Object[] { fileSize, epochNumber }));

        // relay the truncate operation to all remote OSDs
        if (!intraOSD && details.getCurrentReplica().getWidth() > 1) {
            sendTruncateRequests(rq, newLastObject);
            return false;
        }

        return true;
    }

    private void processTruncateAcksReceived(StageMethod rq) throws IOException {

        for (SpeedyRequest sr : rq.getRq().getHttpRequests()) {

            // check for exception
            if (sr.statusCode != HTTPUtils.SC_OKAY) {
                IOException exc = new IOException("truncate on remote OSD failed: " + sr.statusCode
                    + " (" + new String(sr.getResponseBody()) + ")");
                Logging.logMessage(Logging.LEVEL_ERROR, this, "error " + exc);
                throw exc;
            }

            sr.freeBuffer();
        }

        Logging.logMessage(Logging.LEVEL_DEBUG, this, "truncate successful on all "
            + rq.getRq().getHttpRequests().length + " OSDs");
    }

    // private boolean processFetchVersionNumber(StageMethod rq)
    // throws IOException, JSONException {
    //
    // String fileId = rq.getFileId();
    // StripingPolicy sp = rq.getPolicy();
    // long objId = rq.getObjectNo();
    //
    // FileInfo info = getFileInfo(sp, fileId);
    //
    // int vNum = info.getObjectVersion(objId);
    //
    // byte[] data = JSONParser.writeJSON(vNum).getBytes(HTTPUtils.ENC_UTF8);
    //
    // rq.setData(ReusableBuffer.wrap(data), HTTPUtils.DATA_TYPE.JSON);
    //
    // Logging.logMessage(Logging.LEVEL_DEBUG, this, "fetchVersionNumber rq: "
    // + fileId + "-" + objId + "." + vNum);
    // return true;
    // }// processFetchVersionNumber
    //
    // private boolean processClose(StageMethod rq) {
    // // this does not work since we do not have client reference counts for
    // // files!
    //
    // // oft.close(rq.getFileId());
    // // cache.fileClosedEvent(rq.getFileId());
    // // striping.closeFile(rq);
    //
    // return true;
    // }// processClose

    private void processFetchGmax(StageMethod rq) throws IOException {

        final String fileId = rq.getRq().getDetails().getCapability().getFileId();
        final StripingPolicy sp = rq.getRq().getDetails().getCurrentReplica().getStripingPolicy();
        final FileInfo fi = layout.getFileInfo(sp, fileId);

        final List<Long> list = new ArrayList<Long>(3);
        list.add(fi.getTruncateEpoch());
        list.add(fi.getLastObjectNumber());
        list.add(fi.getFilesize());

        try {
            rq.getRq().setData(
                ReusableBuffer.wrap(JSONParser.writeJSON(list).getBytes(HTTPUtils.ENC_UTF8)),
                HTTPUtils.DATA_TYPE.JSON);

        } catch (JSONException ex) {
            throw new RuntimeException("There was a problem with the JSONParser :"
                + ex.getMessage());
        }

        if (StatisticsStage.collect_statistics) {
            master.getStatistics().numGmaxReceived.incrementAndGet();
        }
    }

    private void processGmaxEvent(StageMethod req) throws IOException {
        striping.processGmaxMessage(req.getRq(), cache);
    }

    /**
     * Checks the complete file tree on the OSD for zombies.
     * 
     * @param rq
     * @throws IOException
     * @throws JSONException
     */
    private void processCleanUp1(StageMethod rq) throws IOException,JSONException{
        final ConcurrentFileMap fileList = layout.getAllFiles();
        Logging.logMessage(Logging.LEVEL_TRACE, this, "CleanUp: all files listed!");
                    
        final StorageThread thisStage = this;
        final StageMethod req = rq;

        String authString = NullAuthProvider.createAuthString(master.getConfig().getUUID().toString(), master.getConfig().getUUID().toString());

        // get the volume-locations from the directory service (DIR)        
        RPCResponse<Map<String, Map<String, Object>>> dirResponse = null;     
        
        // for counting the answers
        Set<String> volumeIDs = fileList.unresolvedVolumeIDSet();
        final int volumesToRequest = volumeIDs.size();  
        rq.getRq().setAttachment(0L);
        
        if (volumesToRequest == 0){
            rq.getRq().setData(null,DATA_TYPE.JSON);
            methodExecutionSuccess(rq, StageResponseCode.FINISH);
        }
        
        for(final String volumeID: volumeIDs){
            try{
                // ask the DIR for the UUID
                dirResponse = master.getDIRClient().getEntities(RPCClient.generateMap("uuid", volumeID), 
                                                                DIRClient.generateStringList("mrc"), 
                                                                authString);   
             
                // get the responses asynchronous
                dirResponse.setResponseListener(new RPCResponseListener() {                  
                    @Override
                    public void responseAvailable(RPCResponse response) {
                        try{
                            long count = (Long) req.getRq().getAttachment();
                            count++;
                            req.getRq().setAttachment(count);
                        
                            ServiceUUID uuidService;
                            
                            Map<String, Map<String, Object>> answer = (Map<String, Map<String, Object>>) response.get();                           
                            if (answer==null) throw new IOException("Answer of the request was 'null'");
                            
                            // volume is not registered at the DIR
                            if (answer.get(volumeID) == null || answer.get(volumeID).get("mrc") == null){
                                
                                // mark all files of that volume as zombies 
                                fileList.saveAddress(volumeID, null);                           
                            }else{
                                InetSocketAddress address = null;
                                // parse answer
                                try{
                                    URL url = new URL(((String) answer.get(volumeID).get("mrc")));
                                    address = new InetSocketAddress(url.getHost(),url.getPort());
                                }catch (MalformedURLException mf){
                                    // resolve the UUID
                                    uuidService = new ServiceUUID(((String) answer.get(volumeID).get("mrc")));       
                                    uuidService.resolve();
                                    address = uuidService.getAddress();
                                }
                                
                                // save the address
                                fileList.saveAddress(volumeID,address);
                            }  
    
                            // check if all responses have been received
                            if (volumesToRequest == count){
                                Logging.logMessage(Logging.LEVEL_TRACE, this, "CleanUp: all volumes identified!");
                                req.getRq().setAttachment(fileList);
                                thisStage.enqueueOperation(req.getRq(), STAGEOP_CLEAN_UP2, req.getCallback());
                            }
                        }catch(UnknownUUIDException ue){
                            Logging.logMessage(Logging.LEVEL_ERROR, this, "UUID could not be resolved for: '"+volumeID+"': "+ue.getMessage());
                        }catch(JSONException je){
                            Logging.logMessage(Logging.LEVEL_ERROR, this, "JSON Parser could not get response: "+je.getMessage());
                        }catch (IOException io) {
                            Logging.logMessage(Logging.LEVEL_ERROR, this, "Parser could not get response: "+io.getMessage());
                        }catch (InterruptedException ie){
                            Logging.logMessage(Logging.LEVEL_WARN, this, "CleanUp was interrupted: "+ie.getMessage());
                        }finally{
                            response.freeBuffers();
                        } 
                    }
                });
            }catch (InterruptedException ie) {
                throw new IOException("DIRClient was interrupted while working on a CleanUp request.");
            }   
        }
    }
    
    /**
     * Checks the complete file tree on the OSD for zombies.
     * 
     * @param rq
     * @throws IOException
     * @throws JSONException
     */
    private void processCleanUp2(StageMethod rq) throws IOException,JSONException{
        final List<Long> noZombieSize = new LinkedList<Long>();
        
        final ConcurrentFileMap fileList = (ConcurrentFileMap) rq.getRq().getAttachment();
        final StageMethod req = rq;
        
        // check the files at the metaData & replica catalog (MRC)
        RPCResponse<String> mrcResponse = null;
        String authString = NullAuthProvider.createAuthString(master.getConfig().getUUID().toString(), master.getConfig().getUUID().toString());
        List<String> fileIDs;
        
        // for counting the answers
        Set<String> volumeIDs = fileList.volumeIDSetForRequest();
        final int volumesToRequest = volumeIDs.size();   
        rq.getRq().setAttachment(0L);
        
        
        for(final String volumeID: volumeIDs){            
            fileIDs = fileList.getFileNumbers(volumeID);
            
            // ask the MRC whether the files in the list exist, or not
            mrcResponse = new MRCClient(master.getDIRClient().getSpeedy(),60000)
                                        .checkFileList(fileList.getAddress(volumeID), 
                                                       volumeID,
                                                       fileIDs,
                                                       authString);  
            
            mrcResponse.setAttachment(fileIDs);            
            
            mrcResponse.setResponseListener(new RPCResponseListener() {                  
                @Override
                public void responseAvailable(RPCResponse response) {
                    List<String> fileIDs = (List<String>) response.getAttachment();
                        
                    try{
                        long count = (Long) req.getRq().getAttachment();
                        count++;
                        req.getRq().setAttachment(count);
                        
                        // analyze the answer of the MRC
                        String resp = (String) response.get();
                        
                        for (int i=0;i<resp.length();i++){
                            // volume does not exist anymore
                            if (resp.charAt(i)=='2'){
                                fileList.saveAddress(volumeID, null);
                                break;
                            }
                            // file still exists
                            else if (resp.charAt(i)=='1'){
                                noZombieSize.add(fileList.getFileSize(volumeID, volumeID+":"+fileIDs.get(i)));
                                fileList.remove(volumeID,volumeID+":"+fileIDs.get(i));
                            }
                        }
                        
                        // check if all responses have been received
                        if (volumesToRequest == count){
                            long osdCleanFilesSize = 0L;
                            for (long size : noZombieSize)
                                osdCleanFilesSize += size;
                            
                            Logging.logMessage(Logging.LEVEL_INFO, this, "\nCleanUp: There are "+osdCleanFilesSize+" bytes of clean data on this OSD.");
                                
                            if (!fileList.isEmpty()){
                                Logging.logMessage(Logging.LEVEL_TRACE, this, "CleanUp: all files checked at the MRC!");
                                req.getRq().setAttachment(fileList);
                                master.getStage(Stages.AUTH).enqueueOperation(req.getRq(), AuthenticationStage.STAGEOP_VERIFIY_CLEANUP, req.getCallback());                               
                            }else{
                                req.getRq().setData(null,DATA_TYPE.JSON);
                                methodExecutionSuccess(req, StageResponseCode.FINISH);
                                Logging.logMessage(Logging.LEVEL_INFO, this, "\nCleanUp: There are no Zombies on this OSD.");
                            }
                        }
                    }catch (InterruptedException ie){
                        Logging.logMessage(Logging.LEVEL_WARN, this, "CleanUp was interrupted: "+ie.getMessage());
                    }catch (JSONException je){
                        Logging.logMessage(Logging.LEVEL_ERROR, this, "JSON Parser could not get response: "+je.getMessage());
                    }catch (IOException io){
                        Logging.logMessage(Logging.LEVEL_ERROR, this, "Parser could not get response: "+io.getMessage());
                    }finally{
                        response.freeBuffers();
                    }
                }
            });
        } 
    }

    private ReusableBuffer padWithZeros(ReusableBuffer data, int stripeSize) {
        int oldSize = data.capacity();
        if (!data.enlarge(stripeSize)) {
            ReusableBuffer tmp = BufferPool.allocate(stripeSize);
            data.position(0);
            tmp.put(data);
            while (tmp.hasRemaining())
                tmp.put((byte) 0);
            BufferPool.free(data);
            return tmp;

        } else {
            data.position(oldSize);
            while (data.hasRemaining())
                data.put((byte) 0);
            return data;
        }
    }

    private long truncateShrink(String fileId, long fileSize, long epoch, StripingPolicy sp,
        FileInfo fi, long relOsdId) throws IOException {
        // first find out which is the new "last object"
        final long newLastObject = sp.calculateLastObject(fileSize);
        final long oldLastObject = fi.getLastObjectNumber();
        assert (newLastObject <= oldLastObject);

        Logging.logMessage(Logging.LEVEL_DEBUG, this, "truncate shrink to: " + fileSize
            + " old last: " + fi.getLastObjectNumber() + "   new last: " + newLastObject);

        // remove all unnecessary objects
        final long oldRow = sp.getRow(oldLastObject);
        final long lastRow = sp.getRow(newLastObject);

        for (long r = oldRow; r >= lastRow; r--) {
            final long rowObj = r * sp.getWidth() + relOsdId - 1;
            if (rowObj == newLastObject) {
                // is local and needs to be shrunk
                final long newObjSize = fileSize - sp.getFirstByte(newLastObject);
                truncateObject(fileId, newLastObject, sp, newObjSize, relOsdId);
            } else if (rowObj > newLastObject) {
                // delete objects
                final int v = fi.getObjectVersion(rowObj);
                layout.deleteObject(fileId, rowObj, v);
                fi.deleteObject(rowObj);
            }
        }

        // make sure that last objects exist
        for (long obj = newLastObject - 1; obj > newLastObject - sp.getWidth(); obj--) {
            if (obj > 0 && sp.isLocalObject(obj, relOsdId)) {
                int v = fi.getObjectVersion(obj);
                if (v == 0) {
                    // does not exist
                    createPaddingObject(fileId, obj, sp, 1, sp.getStripeSize(obj), fi);
                }
            }
        }

        return newLastObject;
    }

    private long truncateExtend(String fileId, long fileSize, long epoch, StripingPolicy sp,
        FileInfo fi, long relOsdId) throws IOException {
        // first find out which is the new "last object"
        final long newLastObject = sp.calculateLastObject(fileSize);
        final long oldLastObject = fi.getLastObjectNumber();
        assert (newLastObject >= oldLastObject);

        Logging.logMessage(Logging.LEVEL_DEBUG, this, "truncate extend to: " + fileSize
            + " old last: " + oldLastObject + "   new last: " + newLastObject);

        if (sp.isLocalObject(newLastObject, relOsdId) && newLastObject == oldLastObject) {
            // simply extend the old one
            truncateObject(fileId, newLastObject, sp, fileSize - sp.getFirstByte(newLastObject),
                relOsdId);
        } else {
            if ((oldLastObject > -1) && (sp.isLocalObject(oldLastObject, relOsdId))) {
                truncateObject(fileId, oldLastObject, sp, sp.getStripeSize(oldLastObject), relOsdId);
            }

            // create padding objects
            if (sp.isLocalObject(newLastObject, relOsdId)) {
                long objSize = fileSize - sp.getFirstByte(newLastObject);
                createPaddingObject(fileId, newLastObject, sp, 1, objSize, fi);
            }

            // make sure that last objects exist
            for (long obj = newLastObject - 1; obj > newLastObject - sp.getWidth(); obj--) {
                if (obj > 0 && sp.isLocalObject(obj, relOsdId)) {
                    int v = fi.getObjectVersion(obj);
                    if (v == 0) {
                        // does not exist
                        createPaddingObject(fileId, obj, sp, 1, sp.getStripeSize(obj), fi);
                    }
                }
            }
        }

        return newLastObject;
    }

    private void truncateObject(String fileId, long objNo, StripingPolicy sp, long newSize,
        long relOsdId) throws IOException {

        assert (newSize > 0) : "new size is " + newSize + " but should be > 0";
        assert (newSize <= sp.getStripeSize(objNo));
        assert (objNo >= 0) : "objNo is " + objNo;
        assert (sp.isLocalObject(objNo, relOsdId));

        final FileInfo fi = layout.getFileInfo(sp, fileId);
        final int version = fi.getObjectVersion(objNo);
        final String checksum = fi.getObjectChecksum(objNo);

        ReusableBuffer oldData = layout.readObject(fileId, objNo, version, checksum, sp, 0);

        // test the checksum
        if (!layout.checkObject(oldData, checksum)) {
            Logging.logMessage(Logging.LEVEL_WARN, this, "invalid checksum: file=" + fileId
                + ", objNo=" + objNo);
            BufferPool.free(oldData);
            throw new OSDException(ErrorClass.INTERNAL_SERVER_ERROR, "invalid checksum");
        }

        // no extension necessary when size is correct
        if (oldData.capacity() == newSize) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "truncate not necessary, object " + objNo
                + " is " + newSize);
            BufferPool.free(oldData);
            return;
        }

        Logging
                .logMessage(Logging.LEVEL_DEBUG, this, "truncate object " + objNo + " to "
                    + newSize);

        ReusableBuffer newData = BufferPool.allocate((int) newSize);
        if (newSize < oldData.capacity()) {
            oldData.shrink((int) newSize);
        }
        newData.put(oldData);
        BufferPool.free(oldData);

        // fill the remaining buffer with zeros
        while (newData.position() < newData.capacity())
            newData.put((byte) 0);

        layout.writeObject(fileId, objNo, newData, version + 1, 0, checksum, sp, 0);
        String newChecksum = layout.createChecksum(fileId, objNo, newData, version + 1, checksum);

        BufferPool.free(newData);

        fi.getObjVersions().put(objNo, version + 1);
        fi.getObjChecksums().put(objNo, newChecksum);
    }

    /**
     * @param method
     * @throws JSONException 
     */
    private void processLocalReadObject(StageMethod method) throws IOException, JSONException {
        final RequestDetails details = method.getRq().getDetails();

        final String fileId = details.getFileId();
        final long objNo = details.getObjectNumber();
        final StripingPolicy sp = details.getCurrentReplica().getStripingPolicy();
        final FileInfo fi = layout.getFileInfo(sp, fileId);

        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "LOCAL READ: " + fileId + "-" + objNo + ".");

        int objVer = fi.getObjectVersion(objNo);
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "getting objVer " + objVer);

        String objChksm = fi.getObjectChecksum(objNo);
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "checksum is " + objChksm);

        // retrieve the object from the storage layout
        ReusableBuffer data = layout.readObjectNotPOSIX(fileId, objNo, objVer, objChksm, sp, sp.getOSDByObject(objNo));
        
        // object exists locally ...
        if (data != null && data.capacity() > 0) {
            checkReadObject(method, data);
        }

        // object does not exist locally
        else {
            if (Logging.tracingEnabled())
                Logging.logMessage(Logging.LEVEL_TRACE, this, fileId + "-" + objNo
                    + " not found locally");

            Map<String, Long> jsonResponse = new HashMap<String, Long>();
            // add necessary information
            // currently no additional information are necessary (an empty map implies a not available object)

            String response = JSONParser.writeJSON(jsonResponse);
            data = ReusableBuffer.wrap(response.getBytes());
            method.getRq().setData(data, HTTPUtils.DATA_TYPE.JSON);
            
            // TODO: add statistics
        }

        // add X-New-File-Size header
        method.getRq().addAdditionalResponseHTTPHeader(HTTPHeaders.HDR_XNEWFILESIZE, Long.toString(fi.getFilesize()));
    }

    private void createPaddingObject(String fileId, long objNo, StripingPolicy sp, int version,
        long size, FileInfo fi) throws IOException {
        String checksum = layout.createPaddingObject(fileId, objNo, sp, version, size);
        fi.getObjVersions().put(objNo, version);
        fi.getObjChecksums().put(objNo, checksum);
    }

    private void sendGmaxRequests(StageMethod rq) throws IOException, JSONException {

        List<RPCMessage> gMaxReqs = striping.createGmaxRequests(rq.getRq().getDetails());

        final StageMethod req = rq;

        SpeedyRequest[] reqs = new SpeedyRequest[gMaxReqs.size()];
        int i = 0;
        for (RPCMessage msg : gMaxReqs) {
            SpeedyRequest sr = msg.req;
            sr.listener = new SpeedyResponseListener() {

                public void receiveRequest(SpeedyRequest theRequest) {

                    // count received responses
                    OSDRequest osdReq = (OSDRequest) theRequest.getOriginalRequest();
                    long count = (Long) osdReq.getAttachment();
                    count++;
                    osdReq.setAttachment(count);

                    // check if all responses have been received;
                    // if so, enqueue an operation for the next step
                    if (count == osdReq.getHttpRequests().length)
                        enqueueOperation(osdReq, STAGEOP_READ_OBJECT_GMAX_FETCHED, req
                                .getCallback());
                }

            };
            reqs[i++] = sr;
        }

        rq.getRq().setHttpRequests(reqs);
        rq.getRq().setAttachment(0L);

        Logging.logMessage(Logging.LEVEL_DEBUG, this, "sending gmax requests to remote OSDs ...");

        for (RPCMessage msg : gMaxReqs) {
            master.sendSpeedyRequest(rq.getRq(), msg.req, msg.addr);
            master.getStatistics().numGmaxRPCs.incrementAndGet();
        }
    }

    private void sendTruncateRequests(StageMethod rq, long newLastObject) throws IOException,
        JSONException {

        List<RPCMessage> truncateReqs = striping.createTruncateRequests(rq.getRq().getDetails(),
            newLastObject);

        final StorageThread thisStage = this;
        final StageMethod req = rq;

        SpeedyRequest[] reqs = new SpeedyRequest[truncateReqs.size()];
        int i = 0;
        for (RPCMessage msg : truncateReqs) {
            SpeedyRequest sr = msg.req;
            sr.listener = new SpeedyResponseListener() {

                public void receiveRequest(SpeedyRequest theRequest) {

                    // count received responses
                    OSDRequest osdReq = (OSDRequest) theRequest.getOriginalRequest();
                    long count = (Long) osdReq.getAttachment();
                    count++;
                    osdReq.setAttachment(count);

                    // check if all responses have been received;
                    // if so, enqueue an operation for the next step TODO
                    if (count == osdReq.getHttpRequests().length)
                        thisStage.enqueueOperation(osdReq, STAGEOP_TRUNCATE_ACKS_RECEIVED, req
                                .getCallback());
                }

            };
            reqs[i++] = sr;
        }

        rq.getRq().setHttpRequests(reqs);
        rq.getRq().setAttachment(0L);

        Logging.logMessage(Logging.LEVEL_DEBUG, this, "sending gmax requests to remote OSDs ...");

        for (RPCMessage msg : truncateReqs) {
            master.sendSpeedyRequest(rq.getRq(), msg.req, msg.addr);
            master.getStatistics().numTruncateRPCs.incrementAndGet();
        }
    }

    private void setReadResponse(StageMethod rq, ReusableBuffer data) {

        if (rq.getRq().getDetails().isCheckOnly()) {
            rq.getRq().setData(ReusableBuffer.wrap(String.valueOf(data.capacity()).getBytes()),
                HTTPUtils.DATA_TYPE.JSON);
            BufferPool.free(data);
        } else {
            rq.getRq().setData(data, HTTPUtils.DATA_TYPE.BINARY);
            if (StatisticsStage.collect_statistics) {
                master.getStatistics().bytesTX.addAndGet(data.capacity());
                master.getStatistics().numReads.incrementAndGet();
            }
        }
    }

    protected void methodExecutionSuccess(StageMethod m, StageResponseCode code) {
        if (StatisticsStage.measure_request_times) {
            if (m.getRq() != null)
                master.getStage(Stages.STORAGE).calcRequestDuration(m.getRq());
        }
        super.methodExecutionSuccess(m, code);
    }

    protected void methodExecutionFailed(StageMethod m, ErrorRecord err) {
        if (StatisticsStage.measure_request_times) {
            if (m.getRq() != null)
                master.getStage(Stages.STORAGE).calcRequestDuration(m.getRq());
        }
        super.methodExecutionFailed(m, err);
    }
}
