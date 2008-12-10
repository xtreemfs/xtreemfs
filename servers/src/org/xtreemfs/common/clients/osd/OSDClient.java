/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin and
 Barcelona Supercomputing Center - Centro Nacional de Supercomputacion.

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
 * AUTHORS: Björn Kolbeck (ZIB), Jesús Malo (BSC), Jan Stender (ZIB)
 */

package org.xtreemfs.common.clients.osd;

import java.io.IOException;
import java.net.InetSocketAddress;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.ClientLease;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.clients.RPCClient;
import org.xtreemfs.common.clients.RPCResponse;
import org.xtreemfs.common.striping.Locations;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.pinky.HTTPHeaders;
import org.xtreemfs.foundation.pinky.HTTPUtils;
import org.xtreemfs.foundation.pinky.SSLOptions;
import org.xtreemfs.foundation.speedy.MultiSpeedy;
import org.xtreemfs.osd.RPCTokens;

/**
 *
 * @author bjko
 */
public class OSDClient extends RPCClient {

    /** Creates a new instance of NewOSDClient */
    public OSDClient() throws IOException {
        super();
    }
    
    public OSDClient(MultiSpeedy sharedSpeedy) throws IOException {
        super(sharedSpeedy);
    }

    public OSDClient(MultiSpeedy sharedSpeedy, int timeout) throws IOException {
        super(sharedSpeedy, timeout);
    }

    public OSDClient(int timeout, SSLOptions sslOptions) throws IOException {
        super(timeout, sslOptions);
    }

    public RPCResponse get(InetSocketAddress osd, Locations loc, Capability cap, String file)
        throws IOException, JSONException {
        HTTPHeaders headers = new HTTPHeaders();
        headers.addHeader(HTTPHeaders.HDR_XCAPABILITY, cap.toString());
        headers.addHeader(HTTPHeaders.HDR_XLOCATIONS, loc.asJSONString().asString());

        return send(osd, file, null, headers, null, HTTPUtils.DATA_TYPE.BINARY, HTTPUtils.GET_TOKEN);
    }

    /**
     * performs a GET of a range of bytes on an OSD
     *
     * @param loc
     *            Location of the files.
     * @param cap
     *            Capability of the request
     * @param file
     *            File to use
     * @param objectNumber
     *            Number of the object to use
     * @param firstByte
     *            Offset relative to the object of the first requested byte
     * @param lastByte
     *            Offset relative to the object of the last requested byte
     * @return The response of the OSD
     */
    public RPCResponse get(InetSocketAddress osd, Locations loc, Capability cap, String file,
        long objectNumber, long firstByte, long lastByte) throws IOException, JSONException {
        HTTPHeaders headers = new HTTPHeaders();
        headers.addHeader(HTTPHeaders.HDR_XCAPABILITY, cap.toString());
        headers.addHeader(HTTPHeaders.HDR_XLOCATIONS, loc.asJSONString().asString());
        headers.addHeader(HTTPHeaders.HDR_XOBJECTNUMBER, Long.toString(objectNumber));
        headers.addHeader(HTTPHeaders.HDR_CONTENT_RANGE, "bytes " + Long.toString(firstByte) + "-"
            + Long.toString(lastByte) + "/*");

        return send(osd, file, null, headers, null, HTTPUtils.DATA_TYPE.BINARY, HTTPUtils.GET_TOKEN);
    }
    
    /**
     * performs a GET of a range of bytes on an OSD
     *
     * @param loc
     *            Location of the files.
     * @param cap
     *            Capability of the request
     * @param file
     *            File to use
     * @param objectNumber
     *            Number of the object to use
     * @param firstByte
     *            Offset relative to the object of the first requested byte
     * @param lastByte
     *            Offset relative to the object of the last requested byte
     * @return The response of the OSD
     */
    public RPCResponse get(InetSocketAddress osd, Locations loc, Capability cap, String file,
        long objectNumber, ClientLease lease) throws IOException, JSONException {
        HTTPHeaders headers = new HTTPHeaders();
        headers.addHeader(HTTPHeaders.HDR_XCAPABILITY, cap.toString());
        headers.addHeader(HTTPHeaders.HDR_XLOCATIONS, loc.asJSONString().asString());
        headers.addHeader(HTTPHeaders.HDR_XOBJECTNUMBER, Long.toString(objectNumber));
        headers.addHeader(HTTPHeaders.HDR_XLEASETO, Long.toString(lease.getExpires()));

        return send(osd, file, null, headers, null, HTTPUtils.DATA_TYPE.BINARY, HTTPUtils.GET_TOKEN);
    }

    /**
     * performs a GET for an entire object on an OSD
     *
     * @param loc
     *            Location of the files.
     * @param cap
     *            Capability of the request
     * @param file
     *            File to use
     * @param objectNumber
     *            Number of the object to use
     * @return The response of the OSD
     */
    public RPCResponse get(InetSocketAddress osd, Locations loc, Capability cap, String file,
        long objectNumber) throws IOException, JSONException {
        HTTPHeaders headers = new HTTPHeaders();
        headers.addHeader(HTTPHeaders.HDR_XCAPABILITY, cap.toString());
        headers.addHeader(HTTPHeaders.HDR_XLOCATIONS, loc.asJSONString().asString());
        headers.addHeader(HTTPHeaders.HDR_XOBJECTNUMBER, Long.toString(objectNumber));

        return send(osd, file, null, headers, null, HTTPUtils.DATA_TYPE.BINARY, HTTPUtils.GET_TOKEN);
    }

    /**
     * It requests to the OSD to perform a PUT of a range of bytes
     *
     * @param loc
     *            Location of the files.
     * @param cap
     *            Capability of the request
     * @param file
     *            File to use
     * @param objectNumber
     *            Number of the object to use
     * @param firstByte
     *            Offset relative to the object of the first byte to write
     * @param data
     *            Data to write
     * @return The response of the OSD
     */
    public RPCResponse put(InetSocketAddress osd, Locations loc, Capability cap, String file,
        long objectNumber, long firstByte, ReusableBuffer data) throws IOException, JSONException {
        HTTPHeaders headers = new HTTPHeaders();
        headers.addHeader(HTTPHeaders.HDR_XCAPABILITY, cap.toString());
        headers.addHeader(HTTPHeaders.HDR_XLOCATIONS, loc.asJSONString().asString());
        headers.addHeader(HTTPHeaders.HDR_XOBJECTNUMBER, Long.toString(objectNumber));
        headers.addHeader(HTTPHeaders.HDR_CONTENT_RANGE, "bytes " + Long.toString(firstByte) + "-"
            + Long.toString(firstByte + data.capacity() - 1) + "/*");

        return send(osd, file, data, headers, null, HTTPUtils.DATA_TYPE.BINARY, HTTPUtils.PUT_TOKEN);
    }

    /**
     * It requests to the OSD to perform a PUT of a range of bytes
     *
     * @param loc
     *            Location of the files.
     * @param cap
     *            Capability of the request
     * @param file
     *            File to use
     * @param objectNumber
     *            Number of the object to use
     * @param firstByte
     *            Offset relative to the object of the first byte to write
     * @param data
     *            Data to write
     * @return The response of the OSD
     */
    public RPCResponse putWithForcedIncrement(InetSocketAddress osd, Locations loc, Capability cap,
        String file, long objectNumber, long firstByte, ReusableBuffer data) throws IOException,
        JSONException {
        HTTPHeaders headers = new HTTPHeaders();
        headers.addHeader(HTTPHeaders.HDR_XCAPABILITY, cap.toString());
        headers.addHeader(HTTPHeaders.HDR_XLOCATIONS, loc.asJSONString().asString());
        headers.addHeader(HTTPHeaders.HDR_XOBJECTNUMBER, Long.toString(objectNumber));
        headers.addHeader("X-Force-Increment", "yes");
        headers.addHeader(HTTPHeaders.HDR_CONTENT_RANGE, "bytes " + Long.toString(firstByte) + "-"
            + Long.toString(firstByte + data.capacity() - 1) + "/*");

        return send(osd, file, data, headers, null, HTTPUtils.DATA_TYPE.BINARY, HTTPUtils.PUT_TOKEN);
    }

    /**
     * It requests to the OSD to perform a PUT of a whole object
     *
     * @param loc
     *            Location of the files.
     * @param cap
     *            Capability of the request
     * @param file
     *            File to use
     * @param objectNumber
     *            Number of the object to use
     * @param data
     *            Data to write
     * @return The response of the OSD
     */
    public RPCResponse put(InetSocketAddress osd, Locations loc, Capability cap, String file,
        long objectNumber, ReusableBuffer data) throws IOException, JSONException {
        HTTPHeaders headers = new HTTPHeaders();
        headers.addHeader(HTTPHeaders.HDR_XCAPABILITY, cap.toString());
        headers.addHeader(HTTPHeaders.HDR_XLOCATIONS, loc.asJSONString().asString());
        headers.addHeader(HTTPHeaders.HDR_XOBJECTNUMBER, Long.toString(objectNumber));

        return send(osd, file, data, headers, null, HTTPUtils.DATA_TYPE.BINARY, HTTPUtils.PUT_TOKEN);
    }
    
    /**
     * writes a full object onto an OSD
     *
     * @param loc
     *            Location of the files.
     * @param cap
     *            Capability of the request
     * @param file
     *            File to use
     * @param objectNumber
     *            Number of the object to use
     * @param data
     *            Data to write
     * @return The response of the OSD
     */
    public RPCResponse put(InetSocketAddress osd, Locations loc, Capability cap, String file,
        long objectNumber, ReusableBuffer data, ClientLease lease) throws IOException, JSONException {
        HTTPHeaders headers = new HTTPHeaders();
        headers.addHeader(HTTPHeaders.HDR_XCAPABILITY, cap.toString());
        headers.addHeader(HTTPHeaders.HDR_XLOCATIONS, loc.asJSONString().asString());
        headers.addHeader(HTTPHeaders.HDR_XOBJECTNUMBER, Long.toString(objectNumber));
        headers.addHeader(HTTPHeaders.HDR_XLEASETO, Long.toString(lease.getExpires()));

        return send(osd, file, data, headers, null, HTTPUtils.DATA_TYPE.BINARY, HTTPUtils.PUT_TOKEN);
    }

    /**
     * It requests to the OSD to perform a DELETE of a file
     *
     * @param loc
     *            Location of the files. If null is given, only the data in the
     *            OSD will be deleted, otherwise, the deletion will be in every
     *            OSD in loc.
     * @todo This specification will be changed for the new OSD
     * @param cap
     *            Capability of the request
     * @param file
     *            File to use
     * @return The response of the OSD
     */
    public RPCResponse delete(InetSocketAddress osd, Locations loc, Capability cap, String file)
        throws IOException, JSONException {
        HTTPHeaders headers = new HTTPHeaders();
        headers.addHeader(HTTPHeaders.HDR_XCAPABILITY, cap.toString());

        // @todo In the new OSD, loc cannot be null. This has been changed to
        // deleteReplica
        if (loc != null)
            headers.addHeader(HTTPHeaders.HDR_XLOCATIONS, loc.asJSONString().asString());

        return send(osd, file, null, headers, null, HTTPUtils.DATA_TYPE.BINARY,
            HTTPUtils.DELETE_TOKEN);
    }

    /**
     * It requests to the OSD to perform a getFileSize of a file
     *
     * @param loc
     *            Location of the file.
     * @param cap
     *            Capability of the request
     * @param file
     *            File whose size is requested
     * @param knownSize
     *            Current known size of the file
     * @return The response of the OSD
     */
    public RPCResponse globalMax(InetSocketAddress osd, Locations loc, Capability cap, String file)
        throws IOException, JSONException {

        HTTPHeaders headers = new HTTPHeaders();
        headers.addHeader(HTTPHeaders.HDR_XCAPABILITY, cap.toString());
        headers.addHeader(HTTPHeaders.HDR_XLOCATIONS, loc.asJSONString().asString());
        headers.addHeader(HTTPHeaders.HDR_XFILEID, file);

        return sendRPC(osd, RPCTokens.fetchGlobalMaxToken, null, null, headers);
    }

    /**
     * It requests to the OSD to perform a truncate of a file
     *
     * @param loc
     *            Location of the file.
     * @param cap
     *            Capability of the request
     * @param file
     *            File whose size is requested
     * @param finalSize
     *            Size of the file after truncate
     * @param exclusion
     *            OSD for the X-Excluded-Location or null if no OSD is excluded
     * @return The response of the OSD
     */
    public RPCResponse truncate(InetSocketAddress osd, Locations loc, Capability cap, String file,
        long finalSize) throws JSONException, IOException {
        HTTPHeaders headers = new HTTPHeaders();
        headers.addHeader(HTTPHeaders.HDR_XCAPABILITY, cap.toString());
        headers.addHeader(HTTPHeaders.HDR_XLOCATIONS, loc.asJSONString().asString());

        ReusableBuffer data = ReusableBuffer.wrap(JSONParser.toJSON(file, Long.valueOf(finalSize))
                .getBytes(HTTPUtils.ENC_UTF8));

        return sendRPC(osd, RPCTokens.truncateTOKEN, data, null, headers);
    }

    /**
     * It requests to delete a certain replica from the specified location
     *
     * @param cap
     *            Capability of the request
     * @param fileID
     *            The fileID of the replica to be deleted.
     * @return The response of the OSD
     */
    public RPCResponse deleteReplica(InetSocketAddress osd, Capability cap, String fileID)
        throws IOException, JSONException {
        HTTPHeaders headers = new HTTPHeaders();
        headers.addHeader(HTTPHeaders.HDR_XCAPABILITY, cap.toString());
        headers.addHeader(HTTPHeaders.HDR_XFILEID, fileID);

        return sendRPC(osd, RPCTokens.deleteLocalTOKEN, null, null, headers);
    }

    public RPCResponse deleteReplica(InetSocketAddress osd, Capability cap, String fileID,
        int timeout) throws IOException, JSONException {
        RPCResponse r = deleteReplica(osd, cap, fileID);
        r.getSpeedyRequest().timeout = timeout;
        return r;
    }

    /**
     * It requests to delete a certain replica from the specified location
     *
     * @param cap
     *            Capability of the request
     * @param file
     *            The fileID of the replica to be deleted.
     * @param newFileSize
     *            Size of the file after truncate
     * @return The response of the OSD
     */
    public RPCResponse truncateReplica(InetSocketAddress osd, Locations loc, Capability cap,
        String file, Long newFileSize) throws IOException, JSONException {
        HTTPHeaders headers = new HTTPHeaders();
        headers.addHeader(HTTPHeaders.HDR_XCAPABILITY, cap.toString());
        headers.addHeader(HTTPHeaders.HDR_XLOCATIONS, loc.asJSONString().asString());

        ReusableBuffer data = ReusableBuffer.wrap(JSONParser.toJSON(file, newFileSize).getBytes(
            HTTPUtils.ENC_UTF8));

        return sendRPC(osd, RPCTokens.truncateLocalTOKEN, data, null, headers);
    }
    
    public RPCResponse<Map<String,Object>> getStatistics(InetSocketAddress osd) throws IOException, JSONException {
        return sendRPC(osd, RPCTokens.getstatsTOKEN, null, null, new HTTPHeaders());
    }

    /**
     * Checks consistency of a given object and returns the object's file size.
     *
     * @param osd
     *            the OSD holding the object
     * @param loc
     *            the X-Locations List of the file
     * @param cap
     *            the capability issued by the MRC
     * @param file
     *            the file ID
     * @param objectNumber
     *            the object number
     * @return the response of the OSD, which contains the size of the object in
     *         bytes if no error has occurred
     *
     * @throws IOException
     * @throws JSONException
     */
    public RPCResponse checkObject(InetSocketAddress osd, Locations loc, Capability cap,
        String file, long objectNumber) throws IOException, JSONException {

        HTTPHeaders headers = new HTTPHeaders();
        headers.addHeader(HTTPHeaders.HDR_XCAPABILITY, cap.toString());
        headers.addHeader(HTTPHeaders.HDR_XLOCATIONS, loc.asJSONString().asString());
        headers.addHeader(HTTPHeaders.HDR_XFILEID, file);
        headers.addHeader(HTTPHeaders.HDR_XOBJECTNUMBER, Long.toString(objectNumber));

        return sendRPC(osd, RPCTokens.checkObjectTOKEN, null, null, headers);
    }
    
    public RPCResponse<List<Boolean>> recordStageStats(InetSocketAddress osd, Boolean measureRqs, Boolean basicStats) throws IOException, JSONException {
        
        ReusableBuffer data = ReusableBuffer.wrap(JSONParser.toJSON(measureRqs,basicStats).getBytes(
            HTTPUtils.ENC_UTF8));
        
        return sendRPC(osd, RPCTokens.recordRqDurationTOKEN, data, null, new HTTPHeaders());
    }
    
    /**
     * Acquires or renews a client lease.
     * @param osd the osd from which the lease is requested
     * @param lease the lease object (must contain a lease id for renewal)
     * @return a list with a JSON-encoded client lease and a timestamp (see XtreemFS protocol for details)
     * @throws java.io.IOException
     * @throws org.xtreemfs.foundation.json.JSONException
     */
    public RPCResponse<List<Map<String,Object>>> acquireClientLease(InetSocketAddress osd, Locations loc, Capability cap, ClientLease lease) throws IOException, JSONException {
        
        HTTPHeaders headers = new HTTPHeaders();
        headers.addHeader(HTTPHeaders.HDR_XCAPABILITY, cap.toString());
        headers.addHeader(HTTPHeaders.HDR_XLOCATIONS, loc.asJSONString().asString());
        headers.addHeader(HTTPHeaders.HDR_XFILEID, lease.getFileId());
        
        List<Object> l = new ArrayList(1);
        l.add(lease.encodeAsMap());
        
        ReusableBuffer data = ReusableBuffer.wrap(JSONParser.writeJSON(l).getBytes(
            HTTPUtils.ENC_UTF8));
        
        return sendRPC(osd, RPCTokens.acquireLeaseTOKEN, data, null, headers);
    }
    
    public RPCResponse returnLease(InetSocketAddress osd, Locations loc, Capability cap, ClientLease lease) throws IOException, JSONException {
        HTTPHeaders headers = new HTTPHeaders();
        headers.addHeader(HTTPHeaders.HDR_XCAPABILITY, cap.toString());
        headers.addHeader(HTTPHeaders.HDR_XLOCATIONS, loc.asJSONString().asString());
        headers.addHeader(HTTPHeaders.HDR_XFILEID, lease.getFileId());
        
        List<Object> l = new ArrayList(1);
        l.add(lease.encodeAsMap());
        
        ReusableBuffer data = ReusableBuffer.wrap(JSONParser.writeJSON(l).getBytes(
            HTTPUtils.ENC_UTF8));
        
        return sendRPC(osd, RPCTokens.returnLeaseTOKEN, data, null, headers);
    }

    /**
     * TODO authenticate the user, to ensure that he has the right capabilities.
     * 
     * @param osd
     * @param authString
     * @return a List of fileIDs from potential zombies.
     * @throws IOException
     * @throws JSONException
     * @throws InterruptedException 
     */
    public RPCResponse<Map<String,Map<String,Map<String,String>>>> cleanUp(InetSocketAddress osd, String authString) throws IOException, JSONException, InterruptedException {
        return sendRPC(osd, RPCTokens.cleanUpTOKEN, null, authString, null);
    }
    
    /**
     * <p>If a file was located by the cleanUpOperation this command
     * deletes a file with the given fileID from the given OSD.</p>
     * 
     * @param osd
     * @param authString
     * @param fileID
     * @return
     * @throws IOException
     * @throws JSONException
     * @throws InterruptedException
     */
    public RPCResponse cleanUpDelete(InetSocketAddress osd, String authString, String fileID) throws IOException, JSONException, InterruptedException {
        HTTPHeaders headers = new HTTPHeaders();
        headers.addHeader(HTTPHeaders.HDR_XFILEID, fileID);
        return sendRPC(osd, RPCTokens.deleteLocalTOKEN, null, authString, headers);
    }
}
