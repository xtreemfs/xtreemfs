/*
 * Copyright (c) 2011 by Felix Langner,
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
 * @version 1.00, 08/25/11
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
     * <p>Calculates response time for a request regarding its type, size, priority and utilization of the service 
     * obtained from the composition and average processing times of requests currently responded by the service.</p> 
     * 
     * @param type
     * @param size
     * @param hasPriority
     * 
     * @return the estimated response time for the given request.
     */
    double estimateResponseTime(int type, long size, boolean hasPriority) {
        
        double result = Double.longBitsToDouble(fixedProcessingTimeAverages.get(type));
        if (size > 0L) {
            
            result += Double.longBitsToDouble(variableProcessingTimeAverages.get(type)) * size;
        }
        if (hasPriority) {
            
            result += estimatePriorityWaitingTime();
        } else {
            
            result += estimateWaitingTime();
        }
        return result;
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
        
        if (isInternalRequest) {
            
            internalQueueComposition.decrementAndGet(type);
            internalQueueBandwidthComposition.getAndAdd(type, -size);
        } else {
            
            queueComposition.decrementAndGet(type);
            queueBandwidthComposition.addAndGet(type, -size);
            
            if (hasPriority) {
                
                priorityQueueComposition.decrementAndGet(type);
                priorityQueueBandwidthComposition.addAndGet(type, -size);
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
     * @return the estimated waiting time for the next request enqueued.
     */
    private double estimateWaitingTime() {
        
        double result = successorPerformanceInformation.getWaitingTime();
        
        final int numTypes = fixedProcessingTimeAverages.length();
        for (int i = 0; i < numTypes; i++) {
            
            result += Double.longBitsToDouble(fixedProcessingTimeAverages.get(i)) * queueComposition.get(i);
            result += Double.longBitsToDouble(variableProcessingTimeAverages.get(i)) * queueBandwidthComposition.get(i);
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
     * @return the estimated waiting time for a high-priority request approaching the stage.
     */
    private double estimatePriorityWaitingTime() {
        
        double result = successorPerformanceInformation.getPriorityWaitingTime();
        
        final int numTypes = fixedProcessingTimeAverages.length();
        for (int i = 0; i < numTypes; i++) {
            
            result += Double.longBitsToDouble(fixedProcessingTimeAverages.get(i)) * priorityQueueComposition.get(i);
            result += Double.longBitsToDouble(variableProcessingTimeAverages.get(i)) * 
                      priorityQueueBandwidthComposition.get(i);
        }
        
        return result;
    }
    
    /**
     * @param fixedProcessingTime
     * @param variableProcessingTime
     * @return 
     */
    private double estimateWaitingTime(double[] fixedProcessingTime, double[] variableProcessingTime) {
        
        double result = successorPerformanceInformation.getWaitingTime();
        
        final int numTypes = fixedProcessingTime.length;
        for (int i = 0; i < numTypes; i++) {
            
            result += fixedProcessingTime[i] * queueComposition.get(i);
            result += variableProcessingTime[i] * queueBandwidthComposition.get(i);
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
     * @return
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
        
        builder.append("Fixed processing time averages (external/internal:\n");
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
            builder.append(String.format("%.2f", Double.longBitsToDouble(internalFixedProcessingTimeAverages.get(i))) + "\t");
        }
        builder.append("\n\n");
        
        builder.append("Variable processing time averages (external/internal:\n");
        for (int i = 0; i < numTypes; i++) {
            builder.append(i + "\t");
        }
        for (int i = 0; i < numInternalTypes; i++) {
            builder.append(i + "\t");
        }
        builder.append("\n");
        for (int i = 0; i < numTypes; i++) {
            builder.append(String.format("%.2f", Double.longBitsToDouble(variableProcessingTimeAverages.get(i))) + "\t");
        }
        for (int i = 0; i < numInternalTypes; i++) {
            builder.append(String.format("%.2f", Double.longBitsToDouble(internalVariableProcessingTimeAverages.get(i))) + "\t");
        }
        builder.append("\n\n");
        
        builder.append("Queue composition (external/internal:\n");
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
        
        builder.append("Queue bandwidth composition (external/internal:\n");
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
        
        builder.append("Priority queue composition (external/internal:\n");
        for (int i = 0; i < numTypes; i++) {
            builder.append(i + "\t");
        }
        builder.append("\n");
        for (int i = 0; i < numTypes; i++) {
            builder.append(priorityQueueComposition.get(i) + "\t");
        }
        builder.append("\n\n");
        
        builder.append("Queue bandwidth composition (external/internal:\n");
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
