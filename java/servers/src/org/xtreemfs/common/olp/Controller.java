/*
 * Copyright (c) 2012 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.olp;

import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;

import org.xtreemfs.common.olp.Monitor.PerformanceMeasurementListener;

/**
 * 
 * <p>Methods of this class are not thread safe, because processing of a request is assumed to 
 * be single threaded.</p>
 * 
 * @author fx.langner
 * @version 1.01, 08/25/11
 */
class Controller implements PerformanceMeasurementListener {
    
    /**
     * <p>Composition of external requests queued at the controlled stage (including high priority requests).</p>
     */
    private final AtomicIntegerArray              queueComposition;
    /**
     * <p>Processing volume according to the composition of external requests (including high priority requests).</p>
     */
    private final AtomicLongArray                 queueBandwidthComposition;

    /**
     * <p>Composition of external high-priority requests queued at the controlled stage only.</p>
     */
    private final AtomicIntegerArray              priorityQueueComposition;
    /**
     * <p>Processing volume according to the composition of external high-priority requests.</p>
     */
    private final AtomicLongArray                 priorityQueueBandwidthComposition;
    
    /**
     * <p>Stage-specific processing time averages for external requests at the stage.</p>
     */
    private final AtomicLongArray                 fixedProcessingTimeAverages;
    /**
     * <p>Stage-specific processing time averages for external requests at the stage depending on their data volume.
     * </p>
     */
    private final AtomicLongArray                 variableProcessingTimeAverages;
    
    /**
     * <p>Composition of internal requests queued at the controlled stage.</p>
     */
    private final AtomicIntegerArray              internalQueueComposition;
    /**
     * <p>Processing volume according to the composition of internal requests.</p>
     */
    private final AtomicLongArray                 internalQueueBandwidthComposition;
    
    /**
     * <p>Stage-specific processing time averages for internal requests at the stage.</p>
     */
    private final AtomicLongArray                 internalFixedProcessingTimeAverages;
    /**
     * <p>Stage-specific processing time averages for internal requests at the stage depending on their data volume.</p>
     */
    private final AtomicLongArray                 internalVariableProcessingTimeAverages;
    
    /**
     * <p>{@link PerformanceInformation} of external requests that will pass subsequent stages.</p>
     */
    private final SuccessorPerformanceInformation successorPerformanceInformation;
    
    /**
     * 
     * @param numTypes
     * @param numInternalTypes
     * @param numSubsequentStages
     */
    Controller(int numTypes, int numInternalTypes, int numSubsequentStages) {
        
        queueComposition = new AtomicIntegerArray(numTypes);
        queueBandwidthComposition = new AtomicLongArray(numTypes);
        priorityQueueComposition = new AtomicIntegerArray(numTypes);
        priorityQueueBandwidthComposition = new AtomicLongArray(numTypes);
        
        fixedProcessingTimeAverages = new AtomicLongArray(numTypes);
        variableProcessingTimeAverages = new AtomicLongArray(numTypes);
        
        internalQueueComposition = new AtomicIntegerArray(numInternalTypes);
        internalQueueBandwidthComposition = new AtomicLongArray(numInternalTypes);
        
        internalFixedProcessingTimeAverages = new AtomicLongArray(numInternalTypes);
        internalVariableProcessingTimeAverages = new AtomicLongArray(numInternalTypes);
        
        successorPerformanceInformation = new SuccessorPerformanceInformation(numTypes, numSubsequentStages);
    }
      
    /**
     * <p>Calculates the pure processing time of a request given by its size and type.
     * 
     * @param type
     * @param size
     * @return the estimated processing time for the given request.
     */
    double estimateProcessingTime(int type, long size) {
                
        return Double.longBitsToDouble(fixedProcessingTimeAverages.get(type)) + 
               ((size > 0L) ? Double.longBitsToDouble(variableProcessingTimeAverages.get(type)) * size : 0L);
    }
    
    /**
     * <p>Calculates response time for a request regarding its priority and utilization of the service 
     * obtained from the composition and average processing times of requests currently responded by the service.</p> 
     * 
     * @param processingTime
     * @param hasPriority
     * 
     * @return the estimated response time for the given request.
     */
    double estimateWaitingTime(final boolean hasPriority) {
                
        if (hasPriority) {
            
            return estimatePriorityWaitingTime();
        } else {
            
            return estimateWaitingTime();
        }
    }
    
    void enterRequest(int type, long size, boolean hasPriority, boolean isInternalRequest) {
        
        if (isInternalRequest) {
            
            internalQueueComposition.incrementAndGet(type);
            internalQueueBandwidthComposition.addAndGet(type, size);            
        } else {
            
            queueComposition.incrementAndGet(type);
            queueBandwidthComposition.addAndGet(type, size);
            
            if (hasPriority) {
                
                priorityQueueComposition.incrementAndGet(type);
                priorityQueueBandwidthComposition.addAndGet(type, size);
            }
        }
    }
    
    void quitRequest(int type, long size, boolean hasPriority, boolean isInternalRequest) {
        
        long check;
        if (isInternalRequest) {
            
            check = internalQueueComposition.decrementAndGet(type);
            assert(check >= 0);
            check = internalQueueBandwidthComposition.addAndGet(type, -size);
            assert(check >= 0);
        } else {
            
            check = queueComposition.decrementAndGet(type);
            assert(check >= 0);
            check = queueBandwidthComposition.addAndGet(type, -size);
            assert(check >= 0);
            
            if (hasPriority) {
                
                check = priorityQueueComposition.decrementAndGet(type);
                assert(check >= 0);
                check = priorityQueueBandwidthComposition.addAndGet(type, -size);
                assert(check >= 0);
            }
        }
    }
    
    void updateSuccessorInformation(PerformanceInformation performanceInformation) {
        
        successorPerformanceInformation.updatePerformanceInformation(performanceInformation);
    }
    
    /**
     * 
     * @param id - identifier of the stage that collected the composed {@link PerformanceInformation}.
     * @return {@link PerformanceInformation} that can be shared with preceding stages.
     */
    PerformanceInformation composePerformanceInformation(int id) {
        
        final int numTypes = fixedProcessingTimeAverages.length();
        final double[] fixedProcessingTime = new double[numTypes];
        final double[] variableProcessingTime = new double[numTypes];
        
        for (int i = 0; i < numTypes; i++) {
            
            fixedProcessingTime[i] = Double.longBitsToDouble(fixedProcessingTimeAverages.get(i));
            variableProcessingTime[i] = Double.longBitsToDouble(variableProcessingTimeAverages.get(i));
        }

        return new PerformanceInformation(id, 
                                          fixedProcessingTime, 
                                          variableProcessingTime, 
                                          estimateWaitingTime(fixedProcessingTime, variableProcessingTime),
                                          estimatePriorityWaitingTime(fixedProcessingTime, variableProcessingTime));
    }

/*
 * methods to be accessed by the Monitor
 */

    /* (non-Javadoc)
     * @see org.xtreemfs.common.olp.Monitor.PerformanceMeasurementListener#updateFixedProcessingTimeAverage(int, double, 
     *          boolean)
     */
    @Override
    public void updateFixedProcessingTimeAverage(int type, double value, boolean internalRequests) {
        
        if (internalRequests) {
            
            internalFixedProcessingTimeAverages.set(type, Double.doubleToLongBits(value));
        } else {
            
            fixedProcessingTimeAverages.set(type, 
                    Double.doubleToLongBits(value + successorPerformanceInformation.getFixedProcessingTime(type)));
        }
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.olp.Monitor.PerformanceMeasurementListener#updateVariableProcessingTimeAverage(int, 
     *          double, boolean)
     */
    @Override
    public void updateVariableProcessingTimeAverage(int type, double value, boolean internalRequests) {
        
        if (internalRequests) {
            
            internalVariableProcessingTimeAverages.set(type, Double.doubleToLongBits(value));
        } else {
            
            variableProcessingTimeAverages.set(type, 
                    Double.doubleToLongBits(value + successorPerformanceInformation.getVariableProcessingTime(type)));
        }
    }
    
/*
 * private methods
 */
    
    /**
     * @return the estimated waiting time for the next request enqueued 
     *         (includes estimations for priority and internal requests).
     */
    private double estimateWaitingTime() {
        
        double result = successorPerformanceInformation.getWaitingTime();
        
        final int numTypes = fixedProcessingTimeAverages.length();
        for (int i = 0; i < numTypes; i++) {
            
            double avg = Double.longBitsToDouble(fixedProcessingTimeAverages.get(i));
            long rqs = queueComposition.get(i);
            long pRqs = priorityQueueComposition.get(i);
            
            result += avg * (rqs + pRqs);
            
            avg = Double.longBitsToDouble(variableProcessingTimeAverages.get(i));
            rqs = queueBandwidthComposition.get(i);
            pRqs = priorityQueueBandwidthComposition.get(i);
            
            result += avg * (rqs + pRqs);
        }
        
        final int numInternalTypes = internalFixedProcessingTimeAverages.length();
        for (int i = 0; i < numInternalTypes; i++) {
            
            double avg = Double.longBitsToDouble(internalFixedProcessingTimeAverages.get(i));
            long rqs = internalQueueComposition.get(i);
            
            result += avg * rqs;
            
            avg = Double.longBitsToDouble(internalVariableProcessingTimeAverages.get(i));
            rqs = internalQueueBandwidthComposition.get(i);
            
            result += avg * rqs;
        }
        
        return result;
    }
    
    /**
     * @return the estimated waiting time for a high-priority request approaching the stage.
     */
    private double estimatePriorityWaitingTime() {
        
        double result = successorPerformanceInformation.getPriorityWaitingTime();
        
        final int numTypes = fixedProcessingTimeAverages.length();
        for (int i = 0; i < numTypes; i++) {
            
            double avg = Double.longBitsToDouble(fixedProcessingTimeAverages.get(i));
            long rqs = priorityQueueComposition.get(i);
            
            result += avg * rqs;
            
            avg = Double.longBitsToDouble(variableProcessingTimeAverages.get(i));
            rqs = priorityQueueBandwidthComposition.get(i);
            
            result += avg * rqs;
        }
        
        return result;
    }
    
    /**
     * @param fixedProcessingTime
     * @param variableProcessingTime
     * @return estimated waiting time for the given state (includes estimations for priority and internal requests).
     */
    private double estimateWaitingTime(double[] fixedProcessingTime, double[] variableProcessingTime) {
        
        double result = successorPerformanceInformation.getWaitingTime();
        
        final int numTypes = fixedProcessingTime.length;
        for (int i = 0; i < numTypes; i++) {
            
            result += fixedProcessingTime[i] * (queueComposition.get(i) + priorityQueueComposition.get(i));
            result += variableProcessingTime[i] * 
                      (queueBandwidthComposition.get(i) + priorityQueueBandwidthComposition.get(i));
        }
        
        final int numInternalTypes = internalFixedProcessingTimeAverages.length();
        for (int i = 0; i < numInternalTypes; i++) {
            
            result += Double.longBitsToDouble(internalFixedProcessingTimeAverages.get(i)) * 
                      internalQueueComposition.get(i);
            result += Double.longBitsToDouble(internalVariableProcessingTimeAverages.get(i)) * 
                      internalQueueBandwidthComposition.get(i);
        }
        
        return result;
    }
    
    /**
     * @param fixedProcessingTime
     * @param variableProcessingTime
     * @return estimated waiting time for the given state
     */
    private double estimatePriorityWaitingTime(double[] fixedProcessingTime, double[] variableProcessingTime) {
        
        double result = successorPerformanceInformation.getPriorityWaitingTime();
        
        final int numTypes = fixedProcessingTime.length;
        for (int i = 0; i < numTypes; i++) {
            
            result += fixedProcessingTime[i] * priorityQueueComposition.get(i);
            result += variableProcessingTime[i] * priorityQueueBandwidthComposition.get(i);
        }
        
        return result;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        
        final StringBuilder builder = new StringBuilder();
        
        final int numTypes = fixedProcessingTimeAverages.length();
        final int numInternalTypes = internalFixedProcessingTimeAverages.length();
        
        builder.append("Fixed processing time averages (external/internal):\n");
        for (int i = 0; i < numTypes; i++) {
            builder.append(i + "\t");
        }
        for (int i = 0; i < numInternalTypes; i++) {
            builder.append(i + "\t");
        }
        builder.append("\n");
        for (int i = 0; i < numTypes; i++) {
            builder.append(String.format("%.2f", Double.longBitsToDouble(fixedProcessingTimeAverages.get(i))) + "\t");
        }
        for (int i = 0; i < numInternalTypes; i++) {
            builder.append(String.format("%.2f", Double.longBitsToDouble(internalFixedProcessingTimeAverages.get(i))) + 
                    "\t");
        }
        builder.append("\n\n");
        
        builder.append("Variable processing time averages (external/internal):\n");
        for (int i = 0; i < numTypes; i++) {
            builder.append(i + "\t");
        }
        for (int i = 0; i < numInternalTypes; i++) {
            builder.append(i + "\t");
        }
        builder.append("\n");
        for (int i = 0; i < numTypes; i++) {
            builder.append(String.format("%.2f", Double.longBitsToDouble(variableProcessingTimeAverages.get(i))) + 
                    "\t");
        }
        for (int i = 0; i < numInternalTypes; i++) {
            builder.append(String.format("%.2f", Double.longBitsToDouble(internalVariableProcessingTimeAverages.get(i))) 
                    + "\t");
        }
        builder.append("\n\n");
        
        builder.append("Queue composition (external/internal):\n");
        for (int i = 0; i < numTypes; i++) {
            builder.append(i + "\t");
        }
        for (int i = 0; i < numInternalTypes; i++) {
            builder.append(i + "\t");
        }
        builder.append("\n");
        for (int i = 0; i < numTypes; i++) {
            builder.append(queueComposition.get(i) + "\t");
        }
        for (int i = 0; i < numInternalTypes; i++) {
            builder.append(internalQueueComposition.get(i) + "\t");
        }
        builder.append("\n\n");
        
        builder.append("Queue bandwidth composition (external/internal):\n");
        for (int i = 0; i < numTypes; i++) {
            builder.append(i + "\t");
        }
        for (int i = 0; i < numInternalTypes; i++) {
            builder.append(i + "\t");
        }
        builder.append("\n");
        for (int i = 0; i < numTypes; i++) {
            builder.append(queueBandwidthComposition.get(i) + "\t");
        }
        for (int i = 0; i < numInternalTypes; i++) {
            builder.append(internalQueueBandwidthComposition.get(i) + "\t");
        }
        builder.append("\n\n");
        
        builder.append("Priority queue composition (external/internal):\n");
        for (int i = 0; i < numTypes; i++) {
            builder.append(i + "\t");
        }
        builder.append("\n");
        for (int i = 0; i < numTypes; i++) {
            builder.append(priorityQueueComposition.get(i) + "\t");
        }
        builder.append("\n\n");
        
        builder.append("Queue bandwidth composition (external/internal):\n");
        for (int i = 0; i < numTypes; i++) {
            builder.append(i + "\t");
        }
        builder.append("\n");
        for (int i = 0; i < numTypes; i++) {
            builder.append(priorityQueueBandwidthComposition.get(i) + "\t");
        }
        builder.append("\n\n");
        
        builder.append("Successor Performance Information: \n");
        builder.append(successorPerformanceInformation.toString());
        
        return builder.toString();
    }
}
