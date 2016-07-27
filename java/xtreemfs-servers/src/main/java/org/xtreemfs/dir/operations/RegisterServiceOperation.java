/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.dir.operations;

import java.util.ConcurrentModificationException;
import java.util.Map;

import org.xtreemfs.babudb.api.database.Database;
import org.xtreemfs.babudb.api.exception.BabuDBException;
import org.xtreemfs.common.HeartbeatThread;
import org.xtreemfs.dir.DIRRequest;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.dir.data.ServiceRecord;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.serviceRegisterRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.serviceRegisterResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceConstants;

import com.google.protobuf.Message;

/**
 * 
 * @author bjko
 */
public class RegisterServiceOperation extends DIROperation {

    private final Database database;

    public RegisterServiceOperation(DIRRequestDispatcher master) throws BabuDBException {
        super(master);
        database = master.getDirDatabase();
    }

    @Override
    public int getProcedureId() {
        return DIRServiceConstants.PROC_ID_XTREEMFS_SERVICE_REGISTER;
    }

    @Override
    public void startRequest(DIRRequest rq) {
        final serviceRegisterRequest request = (serviceRegisterRequest) rq.getRequestMessage();

        final Service.Builder reg = request.getService().toBuilder();

        database.lookup(DIRRequestDispatcher.INDEX_ID_SERVREG, reg.getUuid().getBytes(), rq)
                .registerListener(new DBRequestListener<byte[], Long>(false) {

                    @Override
                    Long execute(byte[] result, DIRRequest rq) throws Exception {
                        long currentVersion = 0;
                        ServiceRecord sRec = new ServiceRecord(request.getService().toBuilder().build());

                        // If this request comes from a tool like xtfs_chstatus, this value will be set to
                        // "true" and the last updated time must not be set to the current time. If it does
                        // not exist, it will be set to false.
                        boolean doNotSetLastUpdated = Boolean.parseBoolean(sRec.getData().get(
                                HeartbeatThread.DO_NOT_SET_LAST_UPDATED));

                        if (result != null) {
                            ReusableBuffer buf = ReusableBuffer.wrap(result);
                            ServiceRecord dbData = new ServiceRecord(buf);
                            currentVersion = dbData.getVersion();
                        } else {
                            // The registered service wasn't registered before.
                            // Collect data from the request and inform all
                            // listeners about this registration
                            String uuid, name, type, pageUrl, geoCoordinates;
                            long totalRam, usedRam, lastUpdated;
                            int status, load, protoVersion;

                            uuid = sRec.getUuid();
                            name = sRec.getName();
                            type = sRec.getType().toString();

                            pageUrl = sRec.getData().get("status_page_url") == null ? "" : sRec.getData()
                                    .get("status_page_url");
                            geoCoordinates = sRec.getData().get("vivaldi_coordinates") == null ? "" : sRec
                                    .getData().get("vivaldi_coordinates");
                            try {
                                totalRam = Long.parseLong(sRec.getData().get("totalRAM"));
                            } catch (NumberFormatException nfe) {
                                totalRam = -1;
                            }
                            try {
                                usedRam = Long.parseLong(sRec.getData().get("usedRAM"));
                            } catch (NumberFormatException nfe) {
                                usedRam = -1;
                            }
                            lastUpdated = System.currentTimeMillis() / 1000l;
                            try {
                                status = Integer.parseInt(sRec.getData().get(HeartbeatThread.STATUS_ATTR));
                            } catch (NumberFormatException nfe) {
                                status = -1;
                            }
                            try {
                                load = Integer.parseInt(sRec.getData().get("load"));
                            } catch (NumberFormatException nfe) {
                                load = -1;
                            }
                            try {
                                protoVersion = Integer.parseInt(sRec.getData().get("proto_version"));
                            } catch (NumberFormatException nfe) {
                                protoVersion = -1;
                            }

                            master.notifyServiceRegistred(uuid, name, type, pageUrl, geoCoordinates,
                                    totalRam, usedRam, lastUpdated, status, load, protoVersion);
                        }

                        if (reg.getVersion() != currentVersion) {
                            throw new ConcurrentModificationException("The requested version number ("
                                    + reg.getVersion() + ") did not match the " + "expected version ("
                                    + currentVersion + ")!");
                        }

                        final long version = ++currentVersion;

                        reg.setVersion(currentVersion);
                        if (!doNotSetLastUpdated) {
                            reg.setLastUpdatedS(System.currentTimeMillis() / 1000l);
                        }

                        ServiceRecord newRec = new ServiceRecord(reg.build());

                        Map<String, String> newRecData = newRec.getData();
                        // Remove attributes which must not be stored.
                        newRecData.remove(HeartbeatThread.DO_NOT_SET_LAST_UPDATED);
                        newRec.setData(newRecData);

                        byte[] newData = new byte[newRec.getSize()];
                        newRec.serialize(ReusableBuffer.wrap(newData));
                        database.singleInsert(DIRRequestDispatcher.INDEX_ID_SERVREG,
                                newRec.getUuid().getBytes(), newData, rq).registerListener(
                                new DBRequestListener<Object, Long>(true) {

                                    @Override
                                    Long execute(Object result, DIRRequest rq) throws Exception {

                                        return version;
                                    }
                                });
                        return null;
                    }
                });
    }

    @Override
    public boolean isAuthRequired() {
        return false;
    }

    @Override
    protected Message getRequestMessagePrototype() {
        return serviceRegisterRequest.getDefaultInstance();
    }

    @Override
    void requestFinished(Object result, DIRRequest rq) {
        serviceRegisterResponse resp = serviceRegisterResponse.newBuilder().setNewVersion((Long) result)
                .build();
        rq.sendSuccess(resp);
    }

}
