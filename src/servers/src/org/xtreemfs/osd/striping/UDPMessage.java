/*  Copyright (c) 2008,2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin,.

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

package org.xtreemfs.osd.striping;

import java.net.InetSocketAddress;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.interfaces.OSDInterface.OSDInterface;
import org.xtreemfs.interfaces.utils.ONCRPCRecordFragmentHeader;
import org.xtreemfs.interfaces.utils.ONCRPCRequestHeader;
import org.xtreemfs.interfaces.utils.ONCRPCResponseHeader;
import org.xtreemfs.interfaces.utils.Request;
import org.xtreemfs.interfaces.utils.Response;
import org.xtreemfs.interfaces.utils.Serializable;
import org.xtreemfs.interfaces.utils.XDRUtils;

public class UDPMessage {

    /**
     * @return the address
     */
    public InetSocketAddress getAddress() {
        return address;
    }

    /**
     * @return the payload
     */
    public ReusableBuffer getPayload() {
        return payload;
    }

    private final ONCRPCRequestHeader requestHeader;

    private final ONCRPCResponseHeader responseHeader;

    private final Response             responseData;
    
    private final Request              requestData;

    private final InetSocketAddress address;

    private final ReusableBuffer    payload;

    public UDPMessage(InetSocketAddress address, int xid, int proc, Request payload) {
        requestHeader = new ONCRPCRequestHeader(payload.getTag(),  0 , OSDInterface.getVersion(), payload.getTag());
        responseHeader = null;
        int fragHdr = ONCRPCRecordFragmentHeader.getFragmentHeader(responseHeader.calculateSize()+payload.calculateSize(), true);
        this.address = address;
        ONCRPCBufferWriter wr = new ONCRPCBufferWriter(1024);
        wr.putInt(fragHdr);
        requestHeader.serialize(wr);
        payload.serialize(wr);
        wr.flip();
        this.payload = wr.getBuffers().get(0);
        requestData = null;
        responseData = null;
    }

    private UDPMessage(UDPMessage request, Response payload) {
        requestHeader = null;
        responseHeader = new ONCRPCResponseHeader(request.getRequestHeader().getXID(),
                ONCRPCResponseHeader.REPLY_STAT_MSG_ACCEPTED,
                ONCRPCResponseHeader.ACCEPT_STAT_SUCCESS);

        int fragHdr = ONCRPCRecordFragmentHeader.getFragmentHeader(responseHeader.calculateSize()+payload.calculateSize(), true);
        this.address = request.getAddress();
        ONCRPCBufferWriter wr = new ONCRPCBufferWriter(1024);
        wr.putInt(fragHdr);
        responseHeader.serialize(wr);
        payload.serialize(wr);
        wr.flip();
        this.payload = wr.getBuffers().get(0);
        requestData = null;
        responseData = null;
    }

    public UDPMessage(InetSocketAddress address, ReusableBuffer payload) throws Exception {
        this.address = address;
        this.payload = payload;
        payload.position(Integer.SIZE/8*2);
        int callType = payload.getInt();
        payload.position(Integer.SIZE/8);
        if (callType == XDRUtils.TYPE_CALL) {
            requestHeader = new ONCRPCRequestHeader();
            requestHeader.deserialize(payload);
            responseHeader = null;

            requestData = OSDInterface.createRequest(requestHeader);
            responseData = null;


        } else {
            responseHeader = new ONCRPCResponseHeader();
            responseHeader.deserialize(payload);
            requestHeader = null;

            responseData = OSDInterface.createResponse(responseHeader);
            requestData = null;
        }
    }

    public UDPMessage createResponse(Response response) {
        return new UDPMessage(this, response);
    }

    public boolean isRequest() {
        return requestData != null;
    }

    public boolean isResponse() {
        return responseData != null;
    }

    /**
     * @return the requestHeader
     */
    public ONCRPCRequestHeader getRequestHeader() {
        return requestHeader;
    }

    /**
     * @return the responseHeader
     */
    public ONCRPCResponseHeader getResponseHeader() {
        return responseHeader;
    }

    /**
     * @return the responseData
     */
    public Response getResponseData() {
        return responseData;
    }

    /**
     * @return the requestData
     */
    public Request getRequestData() {
        return requestData;
    }

}