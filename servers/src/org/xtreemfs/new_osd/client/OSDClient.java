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

package org.xtreemfs.new_osd.client;

import java.net.InetSocketAddress;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.foundation.oncrpc.client.ONCRPCClient;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.foundation.oncrpc.client.RPCResponseDecoder;
import org.xtreemfs.interfaces.FileCredentials;
import org.xtreemfs.interfaces.InternalGmax;
import org.xtreemfs.interfaces.InternalReadLocalResponse;
import org.xtreemfs.interfaces.OSDInterface.OSDInterface;
import org.xtreemfs.interfaces.OSDInterface.check_objectRequest;
import org.xtreemfs.interfaces.OSDInterface.check_objectResponse;
import org.xtreemfs.interfaces.OSDInterface.internal_get_gmaxRequest;
import org.xtreemfs.interfaces.OSDInterface.internal_get_gmaxResponse;
import org.xtreemfs.interfaces.OSDInterface.internal_read_localRequest;
import org.xtreemfs.interfaces.OSDInterface.internal_read_localResponse;
import org.xtreemfs.interfaces.OSDInterface.internal_truncateRequest;
import org.xtreemfs.interfaces.OSDInterface.internal_truncateResponse;
import org.xtreemfs.interfaces.OSDInterface.readRequest;
import org.xtreemfs.interfaces.OSDInterface.readResponse;
import org.xtreemfs.interfaces.OSDInterface.truncateRequest;
import org.xtreemfs.interfaces.OSDInterface.truncateResponse;
import org.xtreemfs.interfaces.OSDInterface.unlinkRequest;
import org.xtreemfs.interfaces.OSDInterface.unlinkResponse;
import org.xtreemfs.interfaces.OSDInterface.writeRequest;
import org.xtreemfs.interfaces.OSDWriteResponse;
import org.xtreemfs.interfaces.ObjectData;

/**
 *
 * @author bjko
 */
public class OSDClient extends ONCRPCClient {

    public OSDClient(RPCNIOSocketClient client) {
        super(client, null, 1, OSDInterface.getVersion());
    }

    public RPCResponse<ObjectData> read(InetSocketAddress server, String file_id,
            FileCredentials credentials, long object_number, long object_version, int offset,
            int length) {
        readRequest rq = new readRequest(file_id, credentials, object_number, object_version, offset, length);

        RPCResponse<ObjectData> r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder<ObjectData>() {

            @Override
            public ObjectData getResult(ReusableBuffer data) {
                readResponse resp = new readResponse();
                resp.deserialize(data);
                return resp.getReturnValue();
            }
        });
        return r;
    }

    public RPCResponse<OSDWriteResponse> truncate(InetSocketAddress server, String file_id,
            FileCredentials credentials, long new_file_size ) {
        truncateRequest rq = new truncateRequest(file_id, credentials, new_file_size);

        RPCResponse<OSDWriteResponse> r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder<OSDWriteResponse>() {

            @Override
            public OSDWriteResponse getResult(ReusableBuffer data) {
                truncateResponse resp = new truncateResponse();
                resp.deserialize(data);
                return resp.getOsd_response();
            }
        });
        return r;
    }

    public RPCResponse unlink(InetSocketAddress server, String file_id,
            FileCredentials credentials) {
        unlinkRequest rq = new unlinkRequest(file_id, credentials);

        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder() {

            @Override
            public Object getResult(ReusableBuffer data) {
                unlinkResponse resp = new unlinkResponse();
                resp.deserialize(data);
                return null;
            }
        });
        return r;
    }

    public RPCResponse<OSDWriteResponse> write(InetSocketAddress server, String file_id,
            FileCredentials credentials, long object_number, long object_version, int offset,
            long lease_timeout, ObjectData data) {
        assert((data.getData() == null) || (data.getData().position() == 0));
        writeRequest rq = new writeRequest(file_id, credentials, object_number, object_version, offset, lease_timeout, data);

        RPCResponse<OSDWriteResponse> r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder<OSDWriteResponse>() {

            @Override
            public OSDWriteResponse getResult(ReusableBuffer data) {
                truncateResponse resp = new truncateResponse();
                resp.deserialize(data);
                return resp.getOsd_response();
            }
        });
        return r;
    }



    public RPCResponse internal_truncate(InetSocketAddress server, String file_id,
            FileCredentials credentials, long new_file_size ) {
        internal_truncateRequest rq = new internal_truncateRequest(file_id, credentials, new_file_size);

        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder() {

            @Override
            public Object getResult(ReusableBuffer data) {
                internal_truncateResponse resp = new internal_truncateResponse();
                resp.deserialize(data);
                return null;
            }
        });
        return r;
    }

    public RPCResponse<InternalGmax> internal_get_gmax(InetSocketAddress server, String file_id,
            FileCredentials credentials) {
        internal_get_gmaxRequest rq = new internal_get_gmaxRequest(file_id, credentials);

        RPCResponse<InternalGmax> r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder<InternalGmax>() {

            @Override
            public InternalGmax getResult(ReusableBuffer data) {
                internal_get_gmaxResponse resp = new internal_get_gmaxResponse();
                resp.deserialize(data);
                return resp.getReturnValue();
            }
        });
        return r;
    }

    public RPCResponse<InternalReadLocalResponse> internal_read_local(InetSocketAddress server, String file_id,
            FileCredentials credentials, long object_number, long object_version,long offset, long length) {
        internal_read_localRequest rq = new internal_read_localRequest(file_id, credentials, object_number, object_version, offset, length);

        RPCResponse<InternalReadLocalResponse> r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder<InternalReadLocalResponse>() {

            @Override
            public InternalReadLocalResponse getResult(ReusableBuffer data) {
                internal_read_localResponse resp = new internal_read_localResponse();
                resp.deserialize(data);
                return resp.getReturnValue();
            }
        });
        return r;
    }

    public RPCResponse<ObjectData> check_object(InetSocketAddress server, String file_id,
            FileCredentials credentials, long object_number, long object_version) {

        check_objectRequest rq = new check_objectRequest(file_id, credentials, object_number, object_version);

        RPCResponse<ObjectData> r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder<ObjectData>() {

            @Override
            public ObjectData getResult(ReusableBuffer data) {
                check_objectResponse resp = new check_objectResponse();
                resp.deserialize(data);
                return resp.getReturnValue();
            }
        });
        return r;
    }

    

}
