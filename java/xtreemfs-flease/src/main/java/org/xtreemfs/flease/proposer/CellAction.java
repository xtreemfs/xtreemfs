/*
 * Copyright (c) 2012 by Bjoern Kolbeck.
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.flease.proposer;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Stores a single action for traces.
 * @author bjko
 */
public class CellAction {
    
    private final ActionName actionName;
    private final String message;

    public static enum ActionName {
        PROPOSER_SET_VIEWID,
        PROPOSER_CELL_OPENED,
        PROPOSER_CELL_CLOSED,
        PROPOSER_ACQUIRE_LEASE,
        PROPOSER_RETURNED_LOCAL_LEASE,
        PROPOSER_START_RENEW,
        PROPOSER_RENEW_CANCELLED,
        PROPOSER_RENEW_FAILED_NO_LEASE,
        PROPOSER_RENEW_FAILED_LEASE_TO,
        PROPOSER_RENEW_FAILED_LEASE_NOT_ENOUGH_TIME,
        PROPOSER_RENEW_FAILED_NOT_OWNER,
        PROPOSER_HANDOVER_LEASE,
        PROPOSER_RESTART_EVENT,
        PROPOSER_RENEW_EVENT,
        PROPOSER_INTERNAL_ERROR_CELL_RESET,       
        PROPOSER_SET_BALLOT_NO,
        PROPOSER_REQUEST_MASTER_EPOCH,
        PROPOSER_WAIT_FOR_ACK,
        PROPOSER_PREPARE_START,
        PROPOSER_PREPARE_PROCESS_RESPONSE,       
        PROPOSER_PREPARE_TIMEOUT,
        PROPOSER_VIEW_OUTDATED,
        PROPOSER_PREPARE_OVERRULED,
        PROPOSER_PREPARE_SUCCESS,
        PROPOSER_PREPARE_EMPTY,
        PROPOSER_PREPARE_PRIOR_VALUE,
        PROPOSER_PREPARE_IMPLICIT_RENEW,
        PROPOSER_PREPARE_MAX_MASTER_EPOCH,       
        PROPOSER_ACCEPT_START,
        PROPOSER_ACCEPT_PROCESS_RESPONSE,
        PROPOSER_ACCEPT_TIMEOUT,
        PROPOSER_ACCEPT_OVERRULED,
        PROPOSER_LEARN_START,
        PROPOSER_LEARN_TIMED_OUT, 
        PROPOSER_SCHEDULED_RENEW,
        PROPOSER_LEARN_LEASE_IN_GRACE_PERIOD, 
        PROPOSER_CANCELLED,
        PROPOSER_CANCEL_LEASE_FAILED,
        PROPOSER_CANCEL_SCHEDULE_RESTART,
        PROPOSER_RECEIVED_OUT_OF_SYNC_MSG,
        PROPOSER_RENEW_LEASE,
        PROPOSER_RENEW_FAILED_NO_LOCAL_LEASE_INFO,
        PROPOSER_SCHEDULED_RESTART,
        PROPOSER_SCHEDULED_TIMEOUT,
        PROPOSER_PREPARE_FAILED,
        PROPOSER_PREPARE_LEASE_TO,
        PROPOSER_ACCEPT_FAILED,
        PROPOSER_ACCEPT_SUCCESS,
    }
    
    public static class CellActionList implements Iterable<CellAction> {
        public static final int MAX_ACTIONS_IN_LIST = 50;
        List<CellAction> actions;
        
        public CellActionList() {
            actions = new LinkedList<CellAction>();
        }
        
        public void addAction(ActionName actionName) {
            addAction(actionName, null);
        }
        
        public void addAction(ActionName actionName, String message) {
            actions.add(new CellAction(actionName, message));
            if (actions.size() > MAX_ACTIONS_IN_LIST) {
                actions.remove(0);
            }
        }
        
        @Override
        public Iterator<CellAction> iterator() {
            return actions.iterator();
        }
        
        @Override
        public String toString() {
            StringBuilder text = new StringBuilder();
            text.append("actions:[");
            Iterator<CellAction> iter = iterator();
            while (iter.hasNext()) {
                CellAction action = iter.next();
                text.append(action);
                if (iter.hasNext()) {
                    text.append(", ");
                }
            }
            text.append("]");
            return text.toString();
        }
        
    }
    
    public CellAction(ActionName actionName) {
        this.actionName = actionName;
        this.message = null;
    }
    
    public CellAction(ActionName actionName, String message) {
        this.actionName = actionName;
        this.message = message;
    }
    
    public String toString() {
        return actionName + (message != null
                ? " (" + message +")"
                : "");           
    }

}
