/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.foundation.flease.comm;

import org.xtreemfs.foundation.buffer.ReusableBuffer;


/**
 *
 * @author bjko
 */
public class ProposalNumber implements Cloneable {

    private final long  proposalNo;
    private final long  senderId;

    public static final ProposalNumber EMPTY_PROPOSAL_NUMBER = new ProposalNumber(0, 0);

    public ProposalNumber(long proposalNo, long senderId) {
        this.proposalNo = proposalNo;
        this.senderId = senderId;
    }

    public ProposalNumber(ReusableBuffer buffer) {
        this.proposalNo = buffer.getLong();
        this.senderId = buffer.getLong();
    }

    /**
     * checks if this message is before other
     * @param other another message
     * @return true if this message is before other
     */
    public boolean before(ProposalNumber other) {
        if (this.proposalNo < other.proposalNo) {
            return true;
        } else if (this.proposalNo > other.proposalNo) {
            return false;
        } else {
            return (this.senderId < other.senderId);
        }
    }

    /**
     * checks if this message is after other
     * @param other another message
     * @return true if this message is after (i.e. >) other
     */
    public boolean after(ProposalNumber other) {
        if (this.proposalNo > other.proposalNo) {
            return true;
        } else if (this.proposalNo < other.proposalNo) {
            return false;
        } else {
            return (this.senderId > other.senderId);
        }
    }

    public boolean sameNumber(ProposalNumber other) {
        return (senderId == other.senderId)&& (proposalNo == other.proposalNo);
    }

    public boolean isEmpty() {
        if (this == EMPTY_PROPOSAL_NUMBER)
            return true;
        return this.sameNumber(EMPTY_PROPOSAL_NUMBER);
    }

    public void serialize(ReusableBuffer buffer) {
        buffer.putLong(this.proposalNo);
        buffer.putLong(this.senderId);

    }

    /**
     * @return the proposalNo
     */
    public long getProposalNo() {
        return proposalNo;
    }

    /**
     * @return the senderId
     */
    public long getSenderId() {
        return senderId;
    }

    public String toString() {
        return "("+proposalNo+";"+senderId+")";
    }


}
