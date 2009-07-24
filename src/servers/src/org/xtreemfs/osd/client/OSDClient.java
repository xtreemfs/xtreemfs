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

package org.xtreemfs.osd.client;

import java.net.InetSocketAddress;

import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.foundation.oncrpc.client.ONCRPCClient;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.foundation.oncrpc.client.RPCResponseDecoder;
import org.xtreemfs.interfaces.FileCredentials;
import org.xtreemfs.interfaces.InternalGmax;
import org.xtreemfs.interfaces.InternalReadLocalResponse;
import org.xtreemfs.interfaces.OSDWriteResponse;
import org.xtreemfs.interfaces.ObjectData;
import org.xtreemfs.interfaces.ObjectList;
import org.xtreemfs.interfaces.ObjectListSet;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.UserCredentials;
import org.xtreemfs.interfaces.OSDInterface.OSDInterface;
import org.xtreemfs.interfaces.OSDInterface.readRequest;
import org.xtreemfs.interfaces.OSDInterface.readResponse;
import org.xtreemfs.interfaces.OSDInterface.truncateRequest;
import org.xtreemfs.interfaces.OSDInterface.truncateResponse;
import org.xtreemfs.interfaces.OSDInterface.unlinkRequest;
import org.xtreemfs.interfaces.OSDInterface.unlinkResponse;
import org.xtreemfs.interfaces.OSDInterface.writeRequest;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_check_objectRequest;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_check_objectResponse;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_cleanup_get_resultsRequest;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_cleanup_get_resultsResponse;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_cleanup_is_runningRequest;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_cleanup_is_runningResponse;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_cleanup_startRequest;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_cleanup_startResponse;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_cleanup_statusRequest;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_cleanup_statusResponse;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_cleanup_stopRequest;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_cleanup_stopResponse;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_internal_get_file_sizeRequest;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_internal_get_file_sizeResponse;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_internal_get_gmaxRequest;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_internal_get_gmaxResponse;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_internal_get_object_listRequest;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_internal_get_object_listResponse;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_internal_read_localRequest;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_internal_read_localResponse;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_internal_truncateRequest;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_internal_truncateResponse;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_shutdownRequest;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_shutdownResponse;

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
        readRequest rq = new readRequest(credentials, file_id, object_number, object_version, offset, length);

        RPCResponse<ObjectData> r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder<ObjectData>() {

            @Override
            public ObjectData getResult(ReusableBuffer data) {
                readResponse resp = new readResponse();
                resp.deserialize(data);
                return resp.getObject_data();
            }
        });
        return r;
    }

    public RPCResponse<OSDWriteResponse> truncate(InetSocketAddress server, String file_id,
            FileCredentials credentials, long new_file_size ) {
        truncateRequest rq = new truncateRequest(credentials, file_id, new_file_size);

        RPCResponse<OSDWriteResponse> r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder<OSDWriteResponse>() {

            @Override
            public OSDWriteResponse getResult(ReusableBuffer data) {
                truncateResponse resp = new truncateResponse();
                resp.deserialize(data);
                return resp.getOsd_write_response();
            }
        });
        return r;
    }

    public RPCResponse unlink(InetSocketAddress server, String file_id,
            FileCredentials credentials) {
        unlinkRequest rq = new unlinkRequest(credentials, file_id);

        RPCResponse r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder() {

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
        writeRequest rq = new writeRequest(credentials, file_id, object_number, object_version, offset, lease_timeout, data);

        RPCResponse<OSDWriteResponse> r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder<OSDWriteResponse>() {

            @Override
            public OSDWriteResponse getResult(ReusableBuffer data) {
                truncateResponse resp = new truncateResponse();
                resp.deserialize(data);
                return resp.getOsd_write_response();
            }
        });
        return r;
    }



    public RPCResponse internal_truncate(InetSocketAddress server, String file_id,
            FileCredentials credentials, long new_file_size ) {
        xtreemfs_internal_truncateRequest rq = new xtreemfs_internal_truncateRequest(credentials, file_id, new_file_size);

        RPCResponse r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder() {

            @Override
            public Object getResult(ReusableBuffer data) {
                xtreemfs_internal_truncateResponse resp = new xtreemfs_internal_truncateResponse();
                resp.deserialize(data);
                return null;
            }
        });
        return r;
    }

    public RPCResponse<InternalGmax> internal_get_gmax(InetSocketAddress server, String file_id,
            FileCredentials credentials) {
        xtreemfs_internal_get_gmaxRequest rq = new xtreemfs_internal_get_gmaxRequest(credentials, file_id);

        RPCResponse<InternalGmax> r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder<InternalGmax>() {

            @Override
            public InternalGmax getResult(ReusableBuffer data) {
                xtreemfs_internal_get_gmaxResponse resp = new xtreemfs_internal_get_gmaxResponse();
                resp.deserialize(data);
                return resp.getReturnValue();
            }
        });
        return r;
    }

    public RPCResponse<Long> internal_get_file_size(InetSocketAddress server, String file_id,
            FileCredentials credentials) {
        xtreemfs_internal_get_file_sizeRequest rq = new xtreemfs_internal_get_file_sizeRequest(credentials, file_id);

        RPCResponse<Long> r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder<Long>() {

            @Override
            public Long getResult(ReusableBuffer data) {
                xtreemfs_internal_get_file_sizeResponse resp = new xtreemfs_internal_get_file_sizeResponse();
                resp.deserialize(data);
                return resp.getReturnValue();
            }
        });
        return r;
    }

    public RPCResponse<InternalReadLocalResponse> internal_read_local(InetSocketAddress server,
            String file_id, FileCredentials credentials, long object_number, long object_version,
            long offset, long length, boolean attachObjectList, ObjectListSet requiredObjects) {
        if (requiredObjects == null)
            requiredObjects = new ObjectListSet();

        xtreemfs_internal_read_localRequest rq = new xtreemfs_internal_read_localRequest(credentials,
                file_id, object_number, object_version, offset, length, attachObjectList, requiredObjects);

        RPCResponse<InternalReadLocalResponse> r = sendRequest(server, rq.getTag(), rq,
                new RPCResponseDecoder<InternalReadLocalResponse>() {

                    @Override
                    public InternalReadLocalResponse getResult(ReusableBuffer data) {
                        xtreemfs_internal_read_localResponse resp = new xtreemfs_internal_read_localResponse();
                        resp.deserialize(data);
                        return resp.getReturnValue();
                    }
                });
        return r;
    }

    public RPCResponse<ObjectData> check_object(InetSocketAddress server, String file_id,
            FileCredentials credentials, long object_number, long object_version) {

        xtreemfs_check_objectRequest rq = new xtreemfs_check_objectRequest(credentials, file_id, object_number, object_version);

        RPCResponse<ObjectData> r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder<ObjectData>() {

            @Override
            public ObjectData getResult(ReusableBuffer data) {
                xtreemfs_check_objectResponse resp = new xtreemfs_check_objectResponse();
                resp.deserialize(data);
                return resp.getReturnValue();
            }
        });
        return r;
    }

    public RPCResponse internal_shutdown(InetSocketAddress server, String password) {

        xtreemfs_shutdownRequest rq = new xtreemfs_shutdownRequest();

        UserCredentials creds = new UserCredentials("", new StringSet(), password);

        RPCResponse r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder() {

            @Override
            public Object getResult(ReusableBuffer data) {
                xtreemfs_shutdownResponse resp = new xtreemfs_shutdownResponse();
                resp.deserialize(data);
                return null;
            }
        },creds);
        return r;
    }

    public RPCResponse internal_cleanup_start(InetSocketAddress server, boolean removeZombies,
            boolean removeDeadVolumes, boolean lostAndFound, String password) {

        xtreemfs_cleanup_startRequest rq = new xtreemfs_cleanup_startRequest(removeZombies, removeDeadVolumes, lostAndFound);

        UserCredentials creds = new UserCredentials("", new StringSet(new String[]{"cleanUp"}), password);

        RPCResponse r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder() {

            @Override
            public Object getResult(ReusableBuffer data) {
                xtreemfs_cleanup_startResponse resp = new xtreemfs_cleanup_startResponse();
                resp.deserialize(data);
                return null;
            }
        },creds);
        return r;
    }

    public RPCResponse internal_cleanup_stop(InetSocketAddress server, String password) {

        xtreemfs_cleanup_stopRequest rq = new xtreemfs_cleanup_stopRequest();

        UserCredentials creds = new UserCredentials("", new StringSet(new String[]{"cleanUp"}), password);

        RPCResponse r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder() {

            @Override
            public Object getResult(ReusableBuffer data) {
                xtreemfs_cleanup_stopResponse resp = new xtreemfs_cleanup_stopResponse();
                resp.deserialize(data);
                return null;
            }
        },creds);
        return r;
    }

    public RPCResponse<Boolean> internal_cleanup_is_running(InetSocketAddress server, String password) {

        xtreemfs_cleanup_is_runningRequest rq = new xtreemfs_cleanup_is_runningRequest();

        UserCredentials creds = new UserCredentials("", new StringSet(new String[]{"cleanUp"}), password);

        RPCResponse r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder() {

            @Override
            public Object getResult(ReusableBuffer data) {
                xtreemfs_cleanup_is_runningResponse resp = new xtreemfs_cleanup_is_runningResponse();
                resp.deserialize(data);
                return resp.getIs_running();
            }
        },creds);
        return r;
    }

    public RPCResponse<String> internal_cleanup_status(InetSocketAddress server, String password) {

        xtreemfs_cleanup_statusRequest rq = new xtreemfs_cleanup_statusRequest();

        UserCredentials creds = new UserCredentials("", new StringSet(new String[]{"cleanUp"}), password);

        RPCResponse r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder() {

            @Override
            public Object getResult(ReusableBuffer data) {
                xtreemfs_cleanup_statusResponse resp = new xtreemfs_cleanup_statusResponse();
                resp.deserialize(data);
                return resp.getStatus();
            }
        },creds);
        return r;
    }

    public RPCResponse<StringSet> internal_cleanup_get_result(InetSocketAddress server, String password) {

        xtreemfs_cleanup_get_resultsRequest rq = new xtreemfs_cleanup_get_resultsRequest();

        UserCredentials creds = new UserCredentials("", new StringSet(new String[]{"cleanUp"}), password);

        RPCResponse r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder() {

            @Override
            public Object getResult(ReusableBuffer data) {
                xtreemfs_cleanup_get_resultsResponse resp = new xtreemfs_cleanup_get_resultsResponse();
                resp.deserialize(data);
                return resp.getResults();
            }
        },creds);
        return r;
    }

    public RPCResponse<ObjectList> internal_getObjectList(InetSocketAddress server,
            String file_id, FileCredentials credentials) {
        xtreemfs_internal_get_object_listRequest rq = new xtreemfs_internal_get_object_listRequest(credentials,
                file_id);

        RPCResponse<ObjectList> r = sendRequest(server, rq.getTag(), rq,
                new RPCResponseDecoder<ObjectList>() {
                    @Override
                    public ObjectList getResult(ReusableBuffer data) {
                        xtreemfs_internal_get_object_listResponse resp = new xtreemfs_internal_get_object_listResponse();
                        resp.deserialize(data);
                        return resp.getReturnValue();
                    }
                });
        return r;
    }
}
