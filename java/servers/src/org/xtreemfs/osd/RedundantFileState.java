/*
 * Copyright (c) 2015 by Jan Fajerski,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd;

import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.osd.stages.Stage.StageRequest;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Jan Fajerski
 */
public abstract class RedundantFileState {

    List<StageRequest> pendingRequests;

    public RedundantFileState() {
        this.pendingRequests = new LinkedList();
    }

    /**
     * Appends the request to the end of the list of pending requests.
     */
    public void addPendingRequest(StageRequest request) {
        pendingRequests.add(request);
    }

    /**
     * Removes all of the requests from this list of pending requests and sends them an error response, if a
     * callback of type FileOperationCallback exists.
     *
     * @param error
     *            to respond with or null
     */
    public void clearPendingRequests(ErrorResponse error) {
        if (error != null) {
            // Respond with the error, if a callback of type FileOperationCallback exists.
            for (StageRequest rq : pendingRequests) {
                Object callback = rq.getCallback();
                if (callback != null && callback instanceof FileOperationCallback) {
                    ((FileOperationCallback) callback).failed(error);
                }
            }
        }

        pendingRequests.clear();
    }

    /**
     * Retrieves and removes the head (first element) of the list of pending requests.
     *
     * @return request or null if none exists
     */
    public StageRequest removePendingRequest() {
        StageRequest req = null;
        if (hasPendingRequests()) {
            req = pendingRequests.remove(0);
        }

        return req;
    }

    /**
     * Returns true if there are pending requests.
     */
    public boolean hasPendingRequests() {
        return (!pendingRequests.isEmpty());
    }

    /**
     * Returns the number of pending requests.
     */
    public int sizeOfPendingRequests() {
        return pendingRequests.size();
    }
}
