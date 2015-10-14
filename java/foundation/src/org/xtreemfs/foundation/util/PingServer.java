/*
 * Copyright (c) 2010-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.util;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.Ping;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC;
import org.xtreemfs.foundation.pbrpc.server.RPCNIOSocketServer;
import org.xtreemfs.foundation.pbrpc.server.RPCServerRequest;
import org.xtreemfs.foundation.pbrpc.server.RPCServerRequestListener;
import org.xtreemfs.foundation.pbrpc.utils.ReusableBufferInputStream;

/**
 *
 * @author bjko
 */
public class PingServer {

    public static final String     CERT_DIR         = "../../tests/certs/";

    public static void main(String[] args) {
        
        try {
            Logging.start(Logging.LEVEL_DEBUG, Category.all);

            SSLOptions ssl = null;
            if (true) {
                ssl = new SSLOptions(PingServer.CERT_DIR + "Client.p12", "passphrase", SSLOptions.PKCS12_CONTAINER,
                        PingServer.CERT_DIR + "trusted.jks", "passphrase", SSLOptions.JKS_CONTAINER, false, true, null,
                        null);
            }

            RPCNIOSocketServer server = new RPCNIOSocketServer(12345, null, new RPCServerRequestListener() {
                int cnt = 0;

                @Override
                public void receiveRecord(RPCServerRequest rq) {
                    try {
                        ReusableBufferInputStream is = new ReusableBufferInputStream(rq.getMessage());
                        Ping.PingRequest pingRq = Ping.PingRequest.parseFrom(is);

                        Ping.PingResponse resp = null;
                        if (pingRq.getSendError()) {
                            resp = Ping.PingResponse.newBuilder().setError(Ping.PingResponse.PingError.newBuilder().setErrorMessage("error message")).build();
                        } else {
                            Ping.PingResponse.PingResult result = Ping.PingResponse.PingResult.newBuilder().setText(pingRq.getText()).build();
                            resp = Ping.PingResponse.newBuilder().setResult(result).build();
                        }
                        ReusableBuffer data = null;
                        if (rq.getData() != null) {
                            data = rq.getData().createViewBuffer();
                            data.limit(data.capacity());
                            data.position(data.capacity());
                        }
                        rq.sendResponse(resp,data);
                        cnt++;
                        if (cnt%1000 == 0) {
                            System.out.println(BufferPool.getStatus());
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        rq.sendError(RPC.RPCHeader.ErrorResponse.newBuilder().setErrorType(RPC.ErrorType.GARBAGE_ARGS).setErrorMessage(ex.getMessage()).setDebugInfo(OutputUtils.stackTraceToString(ex)).build());
                    } finally {
                        rq.freeBuffers();
                    }
                }
            }, ssl);
            server.start();
            server.waitForStartup();
            System.out.println("PING server running");
        } catch (Exception ex) {
            Logger.getLogger(PingServer.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}
