/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.olp;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * <p>Thread that will continuously send {@link PerformanceInformation} to preceding stages. Not thread-safe because the
 * sender is meant to be accessed by events fired by a single threaded stage.</p>
 * 
 * @author fx.langner
 * @version 1.00, 09/09/11
 */
class PerformanceInformationSender extends Timer {

    /**
     * <p>The maximal delay between sending performance information to a preceding stage in ms.</p>
     */
    private final static long             DELAY_BETWEEN_UPDATES = 1000;
    
    /**
     * <p>Map of tasks that will provide performance informations to preceding stages.</p>
     */
    private final Map<Integer, TimerTask> tasks = new HashMap<Integer, TimerTask>();
    
    /**
     * <p>Reference to the overload protection algorithm for computing required performance information.</p>
     */
    private final ProtectionAlgorithmCore olp;
    
    
    PerformanceInformationSender(ProtectionAlgorithmCore olp) {
        super("PerformanceInformationSender[" + olp.id + "]", true);
        this.olp = olp;
    }
    
    /**
     * <p>Method that performs required rescheduling if a piggyback exchange of performance information was observed.
     * </p>
     * 
     * @param receiver - that got performance information piggyback.
     */
    void performanceInformationUpdatedPiggyback(PerformanceInformationReceiver receiver) {
        
        assert (receiver != null);
        
        if (!addReceiver(receiver)) {
            
            // task is rescheduled if information were exchanged piggyback
            TimerTask task = tasks.remove(receiver.getStageId());
            task.cancel();
            addReceiver(receiver);
        }
    }
    
    /**
     * <p>Method to add an additional {@link PerformanceInformationReceiver} to this sender. PerformanceInformation are
     * send after a delay equal period, if scheduling was successful.</p>
     * 
     * @param receiver - that requires performance information provided by this sender.
     * 
     * @return true if receiver was successfully added, false if there already exists a task for this receiver.
     */
    boolean addReceiver(PerformanceInformationReceiver receiver) {
        
        assert (receiver != null);
        
        if (!tasks.containsKey(receiver.getStageId())) {
            
            TimerTask task = new SendPerformanceInformation(receiver);
            tasks.put(receiver.getStageId(), task);
            schedule(task, DELAY_BETWEEN_UPDATES, DELAY_BETWEEN_UPDATES);
            return true;
        }
        
        return false;
    }
    
    /**
     * <p>Task for sending performance information regularly to a designated receiver.</p>
     * 
     * @author fx.langner
     * @version 1.00, 09/09/11
     */
    private final class SendPerformanceInformation extends TimerTask {

        /**
         * <p>The receiver of the processing information.</p>
         */
        private final PerformanceInformationReceiver receiver;
        
        /**
         * <p>Default constructor for instantiating a new task to send performance information regularly to the given
         * receiver.</p>
         * 
         * @param receiver
         */
        private SendPerformanceInformation(PerformanceInformationReceiver receiver) {
            
            assert (receiver != null);
            
            this.receiver = receiver;
        }
        
        /* (non-Javadoc)
         * @see java.util.TimerTask#run()
         */
        @Override
        public void run() {
            
            receiver.processPerformanceInformation(olp.composePerformanceInformation());
        }
    }
}