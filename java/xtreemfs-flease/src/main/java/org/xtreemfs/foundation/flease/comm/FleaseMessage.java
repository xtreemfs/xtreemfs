/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.foundation.flease.comm;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Date;
import org.xtreemfs.foundation.buffer.ASCIIString;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.flease.FleaseConfig;

/**
 * A message for multipaxos lease negotiation.
 * @author bjko
 */
public class FleaseMessage implements Serializable, Cloneable {
    
    /**
     * Message types
     */
    public static enum MsgType {
        /**
         * paxos prepare message
         */
        MSG_PREPARE,
        /**
         * acknowledgment for prepare
         */
        MSG_PREPARE_ACK,
        /**
         * reject of prepare
         */
        MSG_PREPARE_NACK,
        /**
         * paxos vote message
         */
        MSG_ACCEPT,
        /**
         * acceptance of vote (voted)
         */
        MSG_ACCEPT_ACK,
        /**
         * reject of vote message
         */
        MSG_ACCEPT_NACK,
        /**
         * paxos learn message
         */
        MSG_LEARN,
        /**
         * for future use (volontary lease return by master, revocation)
         */
        MSG_LEASE_RETURN,

        /**
         * internal timeout event during prepare (timer)
         */
        EVENT_TIMEOUT_PREPARE,

        /**
         * internal timeout event during prepare (timer)
         */
        EVENT_TIMEOUT_ACCEPT,

        /**
         * internal restart event (timer)
         */
        EVENT_RESTART,

        /**
         * event to renew the lease
         */
        EVENT_RENEW,

        /**
         * message (response) indicating that the viewId has changed
         */
        MSG_WRONG_VIEW

    };
    
    private static final long serialVersionUID = 3187504351237849866L;

    public static final int VIEW_ID_INVALIDATED = -1;

    public static final long IGNORE_MASTER_EPOCH = -1;

    public static final long REQUEST_MASTER_EPOCH = 0;
    
    /**
     * message type
     * @see MsgType
     */
    private final MsgType   msgType;
    
    /**
     * cellId this message belongs to
     */
    private ASCIIString    cellId;
    
    /**
     * proposal number for this message
     */
    private ProposalNumber    proposalNo;
    
    
    /**
     * timestamp when the message was sent to discard messages arriving after
     * timeout+dmax.
     */
    private long      sendTimestamp;
    
    /**
     * the socket address of the current lease holder
     * part of the proposal value
     * @attention necessary for correct operation of leases negotiation
     */
    private ASCIIString     leaseHolder;
    
    /**
     * (absolute) timestamp of lease timeout in ms since epoch
     * part of the proposal value
     * @attention necessary for correct operation of leases negotiation
     */
    private long      leaseTimeout;

    private ProposalNumber      prevProposalNo;
    

    private InetSocketAddress address;

    private int                 viewId;

    private long                masterEpochNumber;

    
    /**
     * Creates a new instance of PxMessage
     * @param msgType message type
     */
    public FleaseMessage(MsgType msgType) {
        this.msgType = msgType;
        this.proposalNo = ProposalNumber.EMPTY_PROPOSAL_NUMBER;
        this.prevProposalNo = ProposalNumber.EMPTY_PROPOSAL_NUMBER;
        this.masterEpochNumber = IGNORE_MASTER_EPOCH;
    }
    
    /**
     * Creates a new instance of PxMessage
     * @param msgType message type
     * @param template a message to take instanceId and cellId from
     */
    public FleaseMessage(MsgType msgType, FleaseMessage template) {
        this.msgType = msgType;
        this.proposalNo = template.proposalNo;
        this.cellId = template.cellId;
        this.leaseHolder = template.leaseHolder;
        this.leaseTimeout = template.leaseTimeout;
        this.prevProposalNo = template.prevProposalNo;
        this.viewId = template.viewId;
        this.masterEpochNumber = template.masterEpochNumber;
    }

    public FleaseMessage(FleaseMessage other) {
        this.msgType = other.msgType;
        this.address = other.address;
        this.proposalNo = other.proposalNo;
        this.cellId = other.cellId;
        this.leaseHolder = other.leaseHolder;
        this.leaseTimeout = other.leaseTimeout;
        this.sendTimestamp = other.sendTimestamp;
        this.prevProposalNo = other.prevProposalNo;
        this.viewId = other.viewId;
        this.masterEpochNumber = other.masterEpochNumber;
    }
    
    public void validateMessage() {
        assert(msgType != null);
        assert(proposalNo != null);
        assert(cellId != null);
    }
    
    /**
     * @return the prevAcceptedBallotNo
     */
    public ProposalNumber getPrevProposalNo() {
        return prevProposalNo;
    }

    /**
     * @param prevAcceptedBallotNo the prevAcceptedBallotNo to set
     */
    public void setPrevProposalNo(ProposalNumber prevAcceptedBallotNo) {
        this.prevProposalNo = prevAcceptedBallotNo;
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

    /**
     * @return the masterEpochNumber
     */
    public long getMasterEpochNumber() {
        return masterEpochNumber;
    }

    /**
     * @param masterEpochNumber the masterEpochNumber to set
     */
    public void setMasterEpochNumber(long masterEpochNumber) {
        this.masterEpochNumber = masterEpochNumber;
    }

     
    
    /**
     * Getter for property proposalNo.
     * @return Value of property proposalNo.
     */
    public ProposalNumber getProposalNo() {
        return this.proposalNo;
    }
    
    /**
     * Setter for property proposalNo.
     * @param proposalNo New value of property proposalNo.
     */
    public void setProposalNo(ProposalNumber proposalNo) {
        this.proposalNo = proposalNo;
    }
    
    /**
     * Getter for property msgType.
     * @return Value of property msgType.
     */
    public MsgType getMsgType() {
        return this.msgType;
    }
    
    /**
     * Setter for property msgType.
     * @param msgType New value of property msgType.
     */
    /*public void setMsgType(MsgType msgType) {
        this.msgType = msgType;
    }*/
    
    /**
     * Getter for property leaseHolder.
     * @return Value of property leaseHolder.
     */
    public ASCIIString getLeaseHolder() {
        return this.leaseHolder;
    }
    
    /**
     * Setter for property leaseHolder.
     * @param leaseHolder New value of property leaseHolder.
     */
    public void setLeaseHolder(ASCIIString leaseHolder) {
        this.leaseHolder = leaseHolder;
    }
    
    /**
     * Getter for property leaseTimeout.
     * @return Value of property leaseTimeout.
     */
    public long getLeaseTimeout() {
        return this.leaseTimeout;
    }
    
    /**
     * Setter for property leaseTimeout.
     * @param leaseTimeout New value of property leaseTimeout.
     */
    public void setLeaseTimeout(long leaseTimeout) {
        this.leaseTimeout = leaseTimeout;
    }
    
    /**
     * Getter for property cellId.
     * @return Value of property cellId.
     */
    public ASCIIString getCellId() {
        return this.cellId;
    }
    
    /**
     * Setter for property cellId.
     * @param cellId New value of property cellId.
     */
    public void setCellId(ASCIIString cellId) {
        this.cellId = cellId;
    }

    /**
     * @return the sendTimestamp
     */
    public long getSendTimestamp() {
        return sendTimestamp;
    }

    /**
     * @param sendTimestamp the sendTimestamp to set
     */
    public void setSendTimestamp(long sendTimestamp) {
        this.sendTimestamp = sendTimestamp;
    }

    /**
     * @return the sender
     */
    public InetSocketAddress getSender() {
        return address;
    }

    /**
     * @param sender the sender to set
     */
    public void setSender(InetSocketAddress sender) {
        this.address = sender;
    }
    
    /**
     * checks if this message is before other
     * @param other another message
     * @return true if this message is before other
     */
    public boolean before(FleaseMessage other) {
        return proposalNo.before(other.proposalNo);
    }
    
    /**
     * checks if this message is after other
     * @param other another message
     * @return true if this message is after (i.e. >) other
     */
    public boolean after(FleaseMessage other) {
        return proposalNo.after(other.proposalNo);
    }
    
    /**
     * checks if both messages have the same message id.
     * @param other another message
     * @return true if both messages have the same message id
     */
    public boolean sameMsgId(FleaseMessage other) {
        return proposalNo.sameNumber(other.proposalNo);
    }
    
    /**
     * toString
     * @return a string representation of the message
     */
    @Override
    public String toString() {
        assert(this.msgType != null);
        return String.format("FleaseMessage ( type=%s cell=%s v=%d b=%s lease=%s/%d(%s) prevb=%s ts=%d(%s) addr=%s mepoch=%d)",
                this.msgType.toString(),this.cellId,this.viewId,this.proposalNo,this.leaseHolder,
                this.leaseTimeout,new Date(this.leaseTimeout),this.prevProposalNo,this.sendTimestamp,new Date(this.sendTimestamp),
                (address != null) ? address.toString() : "n/a", this.masterEpochNumber);
    }

    public boolean isInternalEvent() {
        return msgType == MsgType.EVENT_RESTART ||
               msgType == MsgType.EVENT_TIMEOUT_ACCEPT ||
               msgType == MsgType.EVENT_TIMEOUT_PREPARE ||
               msgType == MsgType.EVENT_RENEW;
    }

    public boolean isAcceptorMessage() {
        return msgType == MsgType.MSG_ACCEPT ||
               msgType == MsgType.MSG_LEARN ||
               msgType == MsgType.MSG_PREPARE ||
               msgType == MsgType.MSG_LEASE_RETURN;
    }

    public boolean isProposerMessage() {
        return msgType == MsgType.MSG_ACCEPT_ACK ||
               msgType == MsgType.MSG_ACCEPT_NACK ||
               msgType == MsgType.MSG_PREPARE_ACK ||
               msgType == MsgType.MSG_PREPARE_NACK || 
               msgType == MsgType.MSG_WRONG_VIEW;
    }

    public int getSize() {
        return 1+cellId.getSerializedSize()+
                (leaseHolder == null ? 4 : leaseHolder.getSerializedSize())+
                8+8+8+8+8+8+4+8;
    }
    
    public void serialize(ReusableBuffer buffer) {
        assert(buffer != null);
        buffer.put((byte)this.msgType.ordinal());
        cellId.marshall(buffer);
        proposalNo.serialize(buffer);
        prevProposalNo.serialize(buffer);
        buffer.putLong(this.getSendTimestamp());
        buffer.putLong(this.leaseTimeout);
        if (leaseHolder != null)
            this.leaseHolder.marshall(buffer);
        else
            buffer.putInt(0);
        buffer.putInt(this.viewId);
        buffer.putLong(this.masterEpochNumber);
    }

    public FleaseMessage(ReusableBuffer buffer) {
        assert(buffer != null);
        this.msgType = MsgType.values()[buffer.get()];
        this.cellId = ASCIIString.unmarshall(buffer);
        this.proposalNo = new ProposalNumber(buffer);
        this.prevProposalNo = new ProposalNumber(buffer);
        this.setSendTimestamp(buffer.getLong());
        this.leaseTimeout = buffer.getLong();
        this.leaseHolder = ASCIIString.unmarshall(buffer);
        this.viewId = buffer.getInt();
        this.masterEpochNumber = buffer.getLong();
    }

    public boolean hasTimedOut(FleaseConfig cfg, long currentGlobalTimeout) {
        assert(this.leaseTimeout > 0);
        assert(this.leaseHolder != null);
        return (this.leaseTimeout+cfg.getDMax() < currentGlobalTimeout);
    }

    public boolean hasNotTimedOut(FleaseConfig cfg, long currentGlobalTimeout) {
        assert(this.leaseTimeout > 0);
        assert(this.leaseHolder != null);
        return (this.leaseTimeout-cfg.getDMax() > currentGlobalTimeout);
    }

    @Override
    public FleaseMessage clone() {
        FleaseMessage myClone = new FleaseMessage(this);
        myClone.address = this.address;
        myClone.cellId = this.cellId;
        myClone.leaseHolder = this.leaseHolder;
        myClone.leaseTimeout = this.leaseTimeout;
        myClone.prevProposalNo = this.prevProposalNo;
        myClone.proposalNo = this.proposalNo;
        myClone.sendTimestamp = this.sendTimestamp;
        myClone.viewId = this.viewId;
        myClone.masterEpochNumber = this.masterEpochNumber;
        return myClone;
    }
}
