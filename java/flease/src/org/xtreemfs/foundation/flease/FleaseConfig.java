/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.flease;

import java.net.InetSocketAddress;
import org.xtreemfs.foundation.buffer.ASCIIString;

/**
 *
 * @author bjko
 */
public class FleaseConfig {

    /**
     * maximum lease timeout used in the system
     */
    private final int maxLeaseTimeout_ms;

    /**
     * maximum clock drift allowed
     */
    private final int dmax_ms;


    /**
     * used to discard old messages
     */
    private final int messageTimeout_ms;

    /**
     *  time the proposer waits for an answer
     */
    private final int roundTimeout_ms;

    private final int cellTimeout_ms;

    private final int restartWait_ms;
    
    private final int senderId;

    private final InetSocketAddress endpoint;

    private final ASCIIString identity;

    private final int maxRetries;

    private final boolean sendLearnMessages;

    private final int toNotification_ms;

    private final boolean debugPrintMessages;

    public FleaseConfig(int leaseTimeout_ms, int dmax_ms,
                    int messageTimeout_ms, InetSocketAddress endpoint,
                    String identity, int maxRetries) {
        this(leaseTimeout_ms,dmax_ms,messageTimeout_ms,endpoint,identity,maxRetries,true,0);
    }

    public FleaseConfig(int leaseTimeout_ms, int dmax_ms,
                    int messageTimeout_ms, InetSocketAddress endpoint,
                    String identity, int maxRetries, boolean sendLearnMessages,
                    int toNotification_ms) {
        this(leaseTimeout_ms, dmax_ms, messageTimeout_ms, endpoint, identity, maxRetries, sendLearnMessages, toNotification_ms, false);
    }

    public FleaseConfig(int leaseTimeout_ms, int dmax_ms,
                    int messageTimeout_ms, InetSocketAddress endpoint,
                    String identity, int maxRetries, boolean sendLearnMessages,
                    int toNotification_ms, boolean debugPrintMessages) {

        this.maxLeaseTimeout_ms = leaseTimeout_ms;
        this.dmax_ms = dmax_ms;
        this.messageTimeout_ms = messageTimeout_ms;
        this.cellTimeout_ms = maxLeaseTimeout_ms*2;
        this.roundTimeout_ms = messageTimeout_ms*2;

        this.restartWait_ms = maxLeaseTimeout_ms+dmax_ms*2+messageTimeout_ms;

        this.endpoint = endpoint;
        this.senderId = identity.hashCode();

        checkValidConfiguration();
        this.identity = new ASCIIString(identity);
        this.maxRetries = maxRetries;

        this.sendLearnMessages = sendLearnMessages;
        this.toNotification_ms = toNotification_ms;
        this.debugPrintMessages = debugPrintMessages;

    }

    public void checkValidConfiguration() {

        if (maxLeaseTimeout_ms < dmax_ms*2) {
            throw new IllegalArgumentException("maxLeaseTimeout_ms must be at least twice as long as dmax_ms but should be much bigger");
        }

        if (maxLeaseTimeout_ms < dmax_ms*2+roundTimeout_ms*2) {
            throw new IllegalArgumentException("maxLeaseTimeout_ms must be at least as long as dmax_ms*2+4*message_timeout but should be much bigger");
        }
    }

    /**
     * @return the leaseTimeout_ms
     */
    public int getMaxLeaseTimeout() {
        return maxLeaseTimeout_ms;
    }

    /**
     * @return the dmax_ms
     */
    public int getDMax() {
        return dmax_ms;
    }

    /**
     * @return the maxPxWait_ms
     */
    public int getMessageTimeout() {
        return messageTimeout_ms;
    }

    /**
     * @return the cellTimeout_ms
     */
    public int getCellTimeout() {
        return cellTimeout_ms;
    }

    /**
     * @return the restartWait_ms
     */
    public int getRestartWait() {
        return restartWait_ms;
    }

    /**
     * @return the senderId
     */
    public int getSenderId() {
        return senderId;
    }

    /**
     * @return the endpoint
     */
    public InetSocketAddress getEndpoint() {
        return endpoint;
    }

    public ASCIIString getIdentity() {
        return identity;
    }

    public int getRoundTimeout() {
        return roundTimeout_ms;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * @return the sendLearnMessages
     */
    public boolean isSendLearnMessages() {
        return sendLearnMessages;
    }

    /**
     * @return the toNotification_ms
     */
    public int getToNotification_ms() {
        return toNotification_ms;
    }

    /**
     * @return the debugPrintMessages
     */
    public boolean isDebugPrintMessages() {
        return debugPrintMessages;
    }


}
