/*
 * Copyright (c) 2016 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.ec;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.Replica;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.intervals.Interval;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.ec.LocalRPCResponseHandler.LocalRPCResponseListener;
import org.xtreemfs.osd.ec.LocalRPCResponseHandler.ResponseResult;
import org.xtreemfs.osd.operations.ECReadOperation;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.IntervalMsg;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_ec_readResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;

import com.backblaze.erasure.ReedSolomon;

public class StripeReconstructor {
    // Given as arguments
    final String                      fileId;
    final long                        stripeNo;
    final StripingPolicyImpl          sp;
    final Interval                    reqInterval;
    final List<IntervalMsg>           commitIntervalMsgs;
    final OSDServiceClient            osdClient;
    final OSDRequestDispatcher        master;
    final FileCredentials             fileCredentials;
    final XLocations                  xloc;
    final StripeReconstructorCallback callback;

    // Calculated for convenience
    final Replica                     replica;
    final int                         localOsdNo;
    final int                         dataWidth;         // k
    final int                         parityWidth;       // m
    final int                         stripeWidth;       // n
    final int                         chunkSize;

    // State variables
    final Chunk[]                     chunks;
    int                               chunksFailed;
    int                               chunksComplete;

    boolean                           aborted;
    boolean                           reconstructed;
    boolean                           complete;


    public StripeReconstructor(OSDRequestDispatcher master, FileCredentials fileCredentials, XLocations xloc,
            String fileId, long stripeNo, StripingPolicyImpl sp, Interval reqInterval,
            List<IntervalMsg> commitIntervalMsgs, OSDServiceClient osdClient, StripeReconstructorCallback callback) {

        this.master = master;
        this.fileCredentials = fileCredentials;
        this.xloc = xloc;
        this.replica = xloc.getLocalReplica();
        this.fileId = fileId;
        this.sp = sp;
        this.reqInterval = reqInterval;
        this.commitIntervalMsgs = commitIntervalMsgs;
        this.osdClient = osdClient;
        this.stripeNo = stripeNo;
        this.callback = callback;

        dataWidth = sp.getWidth();
        parityWidth = sp.getParityWidth();
        stripeWidth = sp.getWidth() + sp.getParityWidth();
        chunkSize = sp.getPolicy().getStripeSize() * 1024;
        localOsdNo = sp.getRelativeOSDPosition();

        long firstObjNo = stripeWidth * stripeNo;

        chunks = new Chunk[stripeWidth];
        for (int osdNo = 0; osdNo < stripeWidth; osdNo++) {
            // FIXME (jdillmann): Maybe send stripeNo to parity devices
            long objNo = osdNo < dataWidth ? firstObjNo + osdNo : stripeNo;
            Chunk chunk = new Chunk(osdNo, objNo, ChunkState.NONE);
            chunks[osdNo] = chunk;
        }

        chunksFailed = 0;
        chunksComplete = 0;

        aborted = false;
        reconstructed = false;
        complete = false;
    }

    synchronized boolean hasFinished() {
        return aborted || complete;
    }

    synchronized boolean isComplete() {
        return complete;
    }

    synchronized boolean hasFailed() {
        return aborted;
    }

    synchronized private boolean markFailed(Chunk chunk) {
        if (aborted) {
            return true;
        }

        if (reconstructed) {
            return false;
        }

        chunksFailed++;
        chunk.state = ChunkState.FAILED;

        if (chunksFailed > parityWidth) {
            aborted = true;
            callback.failed(stripeNo);
            return true;
        }

        return false;
    }

    public boolean markFailed(int osdNo) {
        return markFailed(chunks[osdNo]);
    }

    synchronized private boolean addResult(Chunk chunk, ReusableBuffer buffer) {
        if (aborted || reconstructed) {
            BufferPool.free(buffer);
            return false;
        }

        chunksComplete++;
        chunk.state = ChunkState.COMPLETE;
        chunk.buffer = buffer;

        if (chunksComplete >= dataWidth) {
            complete = true;
            callback.success(stripeNo);
            return true;
        }

        return false;
    }

    public boolean addResult(int osdNo, ReusableBuffer buffer) {
        return addResult(chunks[osdNo], buffer);
    }

    synchronized public void markExternalRequest(int osdNo) {
        chunks[osdNo].state = ChunkState.EXTERNAL;
    }

    public void abort() {
        synchronized (this) {
            aborted = true;
        }
        freeBuffers();
    }

    public void freeBuffers() {
        for (Chunk chunk : chunks) {
            BufferPool.free(chunk.buffer);
        }
    }

    public void start() {
        int numResponses = 0;
        for (Chunk chunk : chunks) {
            if (chunk.state == ChunkState.NONE) {
                numResponses++;
            }
        }

        LocalRPCResponseHandler<xtreemfs_ec_readResponse, Chunk> handler = new LocalRPCResponseHandler<xtreemfs_ec_readResponse, Chunk>(
                numResponses, new LocalRPCResponseListener<xtreemfs_ec_readResponse, Chunk>() {
                    @Override
                    public void responseAvailable(ResponseResult<xtreemfs_ec_readResponse, Chunk> result,
                            int numResponses, int numErrors, int numQuickFail) {
                        handleResponse(result, numResponses, numErrors, numQuickFail);
                    }
                });

        int objOffset = 0;
        int objLength = chunkSize;

        for (Chunk chunk : chunks) {
            if (chunk.state == ChunkState.NONE) {
                chunk.state = ChunkState.REQUESTED;

                if (chunk.osdNo == localOsdNo) {
                    // make local request
                    handler.addLocal(chunk);
                    ECReadOperation readOp = (ECReadOperation) master.getOperation(ECReadOperation.PROC_ID);
                    readOp.startLocalRequest(fileId, sp, chunk.osdNo, objOffset, objLength, commitIntervalMsgs, true,
                            handler);
                } else {
                    try {
                        ServiceUUID server = replica.getOSDByPos(chunk.osdNo);
                        RPCResponse<xtreemfs_ec_readResponse> rpcResponse = osdClient.xtreemfs_ec_read(
                                server.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService,
                                fileCredentials, fileId, chunk.objNo, objOffset, objLength, commitIntervalMsgs, true);
                        handler.addRemote(rpcResponse, chunk);

                    } catch (IOException ex) {
                        Logging.logError(Logging.LEVEL_WARN, this, ex);

                        if (markFailed(chunk)) {
                            return;
                        }
                    }
                }
            }
        }
    }

    synchronized private void handleResponse(ResponseResult<xtreemfs_ec_readResponse, Chunk> result, int numResponses,
            int numErrors, int numQuickFail) {

        if (aborted || reconstructed) {
            BufferPool.free(result.getData());
            return;
        }

        Chunk chunk = result.getMappedObject();

        boolean objFailed = result.hasFailed();
        boolean needsReconstruction = !objFailed && result.getResult().getNeedsReconstruction();


        if (objFailed || needsReconstruction) {
            if (markFailed(chunk)) {
                return;
            }
        } else {
            ReusableBuffer buffer = result.getData();
            if (addResult(chunk, buffer)) {
                return;
            }

        }
    }

    public void decode() {
        boolean needsReconstruction;
        synchronized (this) {
            aborted = true;
            needsReconstruction = !reconstructed;
            reconstructed = true;
        }

        // VORSICHT MIT ABORT

        if (needsReconstruction) {
            ReedSolomon codec = ReedSolomon.create(dataWidth, parityWidth);
            boolean[] present = new boolean[stripeWidth];
            ByteBuffer[] shards = new ByteBuffer[stripeWidth];

            for (Chunk chunk : chunks) {
                if (chunk.state == ChunkState.COMPLETE) {
                    present[chunk.osdNo] = true;
                    chunk.buffer = ECHelper.zeroPad(chunk.buffer, chunkSize);
                } else {
                    present[chunk.osdNo] = false;
                    chunk.state = ChunkState.RECONSTRUCTED;
                    chunk.buffer = BufferPool.allocate(chunkSize);
                }
                shards[chunk.osdNo] = chunk.buffer.getBuffer().slice();
            }

            // FIXME (jdillmann): optimize
            codec.decodeMissing(shards, present, 0, chunkSize, false);


            // boolean[] present = new boolean[stripeWidth];
            // byte[][] shards = new byte[stripeWidth][chunkSize];
            // for (int i = 0; i < stripeWidth; i++) {
            // ChunkState chunkState = stripeState.chunkStates[i];
            // if (chunkState != null && chunkState.state == ResponseState.SUCCESS) {
            // present[i] = true;
            // ReusableBuffer viewBuf = chunkState.buffer.createViewBuffer();
            // viewBuf.get(shards[i]);
            // BufferPool.free(viewBuf);
            // } else {
            // present[i] = false;
            // }
            // }
            //
            // // FIXME (jdillmann): optimize / use buffer
            // codec.decodeMissing(shards, present, 0, chunkSize);
            // for (int i = 0; i < dataWidth; i++) {
            // ChunkState chunkState = stripeState.chunkStates[i];
            // if (chunkState != null && chunkState.state == ResponseState.FAILED
            // || chunkState.state == ResponseState.NEEDS_RECONSTRUCTION) {
            // ReusableBuffer recoveredBuf = ReusableBuffer.wrap(shards[i]);
            // chunkState.state = ResponseState.RECONSTRUCTED;
            // chunkState.buffer = recoveredBuf;
            // }
            // }
        }
    }

    public ReusableBuffer getObject(int osdNum) {
        if (!reconstructed) {
            // error!
        }

        return chunks[osdNum].buffer.createViewBuffer();
    }


    enum ChunkState {
        NONE, FAILED, COMPLETE, REQUESTED, EXTERNAL, RECONSTRUCTED
    }

    final private static class Chunk {
        final int      osdNo;
        final long     objNo;
        ChunkState     state;
        ReusableBuffer buffer;

        public Chunk(int osdNo, long objNo, ChunkState state) {
            this.osdNo = osdNo;
            this.objNo = objNo;
            this.state = state;
        }
    }

    public static interface StripeReconstructorCallback {
        public void success(long stripeNo);

        public void failed(long stripeNo);
    }
}
