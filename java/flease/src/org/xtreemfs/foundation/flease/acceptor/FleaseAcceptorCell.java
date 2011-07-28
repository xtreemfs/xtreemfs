/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.foundation.flease.acceptor;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.flease.comm.FleaseMessage;

/**
 * A coordination cell is used to separate concurrent lease nogitiations
 * for individual objects (e.g. volumes on the MRC).
 * @author bjko
 */
public class FleaseAcceptorCell implements Serializable {

    FleaseMessage prepared;

    FleaseMessage accepted;

    AtomicReference<FleaseMessage> latestLearn;

    
    /** timestamp for last access
     */
    public long lastAccess;

    private int viewId;
            
    /**
     * Creates a new instance of CoordinationCell
     */
    public FleaseAcceptorCell() {
        prepared = null;
        accepted = null;
        lastAccess = TimeSync.getLocalSystemTime();
        latestLearn = new AtomicReference<FleaseMessage>();
    }

    public FleaseMessage getLatestLearn() {
        return latestLearn.get();
    }

    public void setLatestLearn(FleaseMessage msg) {
        latestLearn.set(msg);
    }

    /**
     * Getter for property lastPrep.
     * @return Value of property lastPrep.
     */
    public FleaseMessage getPrepared() {
        return this.prepared;
    }

    /**
     * Setter for property lastPrep.
     * @param lastPrep New value of property lastPrep.
     */
    public void setPrepared(FleaseMessage prepared) {
        this.prepared = prepared;
    }

    /**
     * Getter for property accepted.
     * @return Value of property accepted.
     */
    public FleaseMessage getAccepted() {
        return this.accepted;
    }

    /**
     * Setter for property accepted.
     * @param accepted New value of property accepted.
     */
    public void setAccepted(FleaseMessage accepted) {
        assert(accepted.getLeaseHolder() != null);
        assert(accepted.getLeaseTimeout() > 0);
        this.accepted = accepted;
    }

    public boolean isLearned() {
        if (accepted == null)
            return false;

        return (accepted.getMsgType() == FleaseMessage.MsgType.MSG_LEARN);
    }

    public void touch() {
        this.lastAccess = System.currentTimeMillis();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("\t");
        sb.append("prep   ");
        sb.append(getPrepared());
        sb.append("\n");
        sb.append("\t");
        sb.append("accept ");
        sb.append(getAccepted());
        sb.append("\n");
        sb.append("\t");
        sb.append("isLrnd ");
        sb.append(isLearned());
        sb.append("\n");
        
        return sb.toString();

    }

    /**
     * @return the viewId
     */
    public int getViewId() {
        return viewId;
    }

    /**
     * @param viewId the viewId to set
     */
    public void setViewId(int viewId) {
        this.viewId = viewId;
    }

}
