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
 * AUTHORS: Björn Kolbeck (ZIB), Jan Stender (ZIB)
 */

package org.xtreemfs.new_osd.storage;

import java.net.InetSocketAddress;

import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.foundation.speedy.SpeedyRequest;

/**
 * A helper class providing methods related to the OSD striping logic.
 */
public final class Striping {

    /**
     * wait up to a minute for delete to return
     *
     */
    public static final int DELETE_TO = 60 * 1000;

    private final ServiceUUID localOSDId;

    private MetadataCache   storageCache;

    public Striping(ServiceUUID localId, MetadataCache storageCache) {
        this.localOSDId = localId;
        this.storageCache = storageCache;
    }

    /*public List<RPCMessage> createGmaxRequests(RequestDetails req) throws JSONException, UnknownUUIDException {

        assert (req.getCapability() != null);
        assert (req.getFileId() != null);
        assert (req.getLocationList() != null);

        final List<RPCMessage> osdMessages = new LinkedList<RPCMessage>();

        for (ServiceUUID osd : req.getCurrentReplica().getOSDs()) {

            if (osd.equals(localOSDId))
                continue;

            HTTPHeaders headers = new HTTPHeaders();
            headers.addHeader(HTTPHeaders.HDR_XCAPABILITY, req.getCapability().toString());
            headers.addHeader(HTTPHeaders.HDR_XLOCATIONS, req.getLocationList().asJSONString()
                    .asString());
            headers.addHeader(HTTPHeaders.HDR_XFILEID, req.getFileId());

            SpeedyRequest sr = new SpeedyRequest(HTTPUtils.POST_TOKEN, headers,
                RPCTokens.fetchGlobalMaxToken);
            InetSocketAddress addr = osd.getAddress();

            osdMessages.add(new RPCMessage(addr, sr));
        }

        return osdMessages;
    }*/

    /*public void processGmaxResponses(OSDRequest req) throws ONCRPCException,IOException, InterruptedException {

        final RequestDetails details = req.getDetails();
        final String fileId = details.getFileId();
        final FileInfo fi = storageCache.getFileInfo(fileId);

        for (RPCResponse<InternalGmax> r : req.getPendingRequests()) {

            InternalGmax result;
            try {
                result = r.get();
            } finally {
                r.freeBuffers();
            }

            // if a larger file size or a newer epoch has been
            // received, replace gmax w/ received globalMax
            if ((result.getEpoch() == fi.getTruncateEpoch() && result.getFile_size() > fi.getFilesize())
                || result.getEpoch() > fi.getTruncateEpoch()) {

                fi.setLastObjectNumber(result.getLast_object_id());
                fi.setFilesize(result.getFile_size());

                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, this,
                        "received more up-to-date (fs/epoch)=(" + result.getFile_size() + "/"
                            + result.getEpoch() + ") for " + fileId + ", replacing former one on "
                            + localOSDId);
            }

            
        }
    }

    public List<UDPMessage> createGmaxMessages(ASCIIString fileId, long newFS, long newLastObjNo,
        long newEpoch, Location stripes) throws UnknownUUIDException {

        if (Logging.isDebug()) {
            final FileInfo fi = storageCache.getFileInfo(fileId.toString());
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "create GMAX message: " + newFS
                + ", former fs=" + fi.getFilesize());
        }

        List<UDPMessage> msgs = new LinkedList<UDPMessage>();
        ReusableBuffer data = BufferPool.allocate(128);
        data.put((byte) UDPMessageType.Striping.ordinal());
        data.putBufferBackedASCIIString(fileId);
        data.putLong(newEpoch);
        data.putLong(newLastObjNo);
        data.putLong(newFS);

        for (ServiceUUID osd : stripes.getOSDs()) {
            if (osd.equals(localOSDId))
                continue;
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "sending UDP GMAX to " + osd + ": "
                    + newLastObjNo + " for " + fileId);
            msgs.add(new UDPMessage(osd.getAddress(), data));
        }

        if (msgs.size() == 0)
            BufferPool.free(data);

        return msgs;
    }

    public void processGmaxMessage(OSDRequest rq, MetadataCache cache) {
        // parse request
        ReusableBuffer data = null;
        try {
            data = rq.getUdpData();
            final ASCIIString fileId = data.getBufferBackedASCIIString();
            final long epoch = data.getLong();
            final long newLastObjNo = data.getLong();
            final long newFS = data.getLong();

            // check if FileInfo is in cache
            FileInfo fi = cache.getFileInfo(fileId.toString());

            if (fi == null) {
                // file is not open, discard GMAX
                return;
            }

            Logging.logMessage(Logging.LEVEL_DEBUG, this, "received new GMAX: " + newLastObjNo
                + "/" + newFS + "/" + epoch + " for " + fileId);

            if ((epoch == fi.getTruncateEpoch() && fi.getFilesize() < newFS)
                || epoch > fi.getTruncateEpoch()) {

                // valid file size update
                fi.setFilesize(newFS);

                Logging.logMessage(Logging.LEVEL_DEBUG, this, "received GMAX is valid; for "
                    + fileId + ", current (fs, epoch) = (" + fi.getFilesize() + ", "
                    + fi.getTruncateEpoch() + ")");

            } else {

                // outdated file size udpate

                Logging.logMessage(Logging.LEVEL_DEBUG, this, "received GMAX is outdated; for "
                    + fileId + ", current (fs, epoch) = (" + fi.getFilesize() + ", "
                    + fi.getTruncateEpoch() + ")");
            }
        } finally {
            BufferPool.free(data);
        }
    }

    /*public List<RPCMessage> createTruncateRequests(RequestDetails details, long lastObject)
        throws IOException, JSONException {

        List<RPCMessage> osdMessages = new LinkedList<RPCMessage>();

        for (ServiceUUID osd : details.getCurrentReplica().getOSDs()) {

            if (osd.equals(localOSDId))
                continue;

            HTTPHeaders headers = new HTTPHeaders();
            headers.addHeader(HTTPHeaders.HDR_XCAPABILITY, details.getCapability().toString());
            headers.addHeader(HTTPHeaders.HDR_XLOCATIONS, details.getLocationList().asJSONString()
                    .asString());
            headers.addHeader(HTTPHeaders.HDR_XFILEID, details.getFileId());

            ReusableBuffer data = ReusableBuffer.wrap(JSONParser.toJSON(details.getFileId(),
                Long.valueOf(details.getTruncateFileSize())).getBytes(HTTPUtils.ENC_UTF8));

            SpeedyRequest sr = new SpeedyRequest(HTTPUtils.POST_TOKEN,
                RPCTokens.truncateLocalTOKEN, null, null, data, HTTPUtils.DATA_TYPE.JSON, headers);

            InetSocketAddress addr = osd.getAddress();

            osdMessages.add(new RPCMessage(addr, sr));
        }

        return osdMessages;
    }*/


    public static class RPCMessage {

        public InetSocketAddress addr;

        public SpeedyRequest     req;

        public RPCMessage(InetSocketAddress addr, SpeedyRequest req) {
            this.addr = addr;
            this.req = req;
        }

    }

    public static class UDPMessage {

        public InetSocketAddress addr;

        public ReusableBuffer    buf;

        public UDPMessage(InetSocketAddress addr, ReusableBuffer buf) {
            this.addr = addr;
            this.buf = buf;
        }

    }

}
