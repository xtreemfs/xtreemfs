/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.olp;

import java.util.LinkedList;
import java.util.List;

/**
 * <p>Implementation of {@link Monitor} that will use a slow-start like algorithm to determine the best sample amount 
 * before average-calculation.</p>
 * 
 * @author fx.langner
 * @version 1.00, 09/08/11
 */
class AdvancedMonitor extends Monitor {
    
    /**
     * <p>Number of samples to collect before calculating there average.</p>
     */
    private final static int     INITIAL_SAMPLE_AMOUNT = 10;
    
    /**
     * <p>Time in ms for the difference between two measurement averages.</p>
     */
    private final static double  AVERAGE_DIFFERENCE_THRESHOLD = 10.0;
    
    /**
     * <p>Factor of the difference between two measurement averages.</p>
     */
    private final static double  AVERAGE_QUOTIEN_THRESHOLD = 2.0;
    
    /**
     * <p>Contains samples of fixed time measurements in ms of different types of requests.</p>
     */
    private final List<Double>[] fixedTimeMeasurements;
    
    /**
     * <p>Contains fixed time averages of the last summary for comparison.</p>
     */
    private final double[]       fixedTimeAverages;
    
    /**
     * <p>Contains samples of variable time measurements in ms/byte of different types of requests.</p>
     */
    private final List<Double>[] variableTimeMeasurements;
    
    /**
     * <p>Contains variable time averages of the last summary for comparison.</p>
     */
    private final double[]       variableTimeAverages;     
    
    /**
     * <p>Contains the current amount of fixed time samples to measure before aggregation for different types of 
     * requests.</p>
     */
    private final int[]          fixedSamplesToMeasure;
    
    /**
     * <p>Contains the current amount of variable time samples to measure before aggregation for different types of 
     * requests.</p>
     */
    private final int[]          variableSamplesToMeasure;
    
    /**
     * <p>Constructor initializing necessary fields for collecting measurement samples.</p>
     * 
     * @param isForInternalRequests - true if the performance averages are measured for internal requests, 
     *                            false otherwise.
     * @param numTypes - amount of different request types expected.
     * @param listener - to send the summarized performance information to.
     * @see Monitor
     */
    @SuppressWarnings("unchecked")
    AdvancedMonitor(boolean isForInternalRequests, int numTypes, PerformanceMeasurementListener listener) {
        super(listener, isForInternalRequests);
        
        fixedTimeMeasurements = new List[numTypes];
        fixedTimeAverages = new double[numTypes];
        fixedSamplesToMeasure = new int[numTypes];
        
        variableTimeMeasurements = new List[numTypes];
        variableTimeAverages = new double[numTypes];
        variableSamplesToMeasure = new int[numTypes];
        
        for (int i = 0; i < numTypes; i++) {
            fixedSamplesToMeasure[i] = INITIAL_SAMPLE_AMOUNT;
            fixedTimeMeasurements[i] = new LinkedList<Double>();
            variableSamplesToMeasure[i] = INITIAL_SAMPLE_AMOUNT;
            variableTimeMeasurements[i] = new LinkedList<Double>();
        }
    }

    /* (non-Javadoc)
     * @see org.xtreemfs.common.olp.Monitor#record(int, double, double)
     */
    @Override
    void record(int type, double fixedProcessingTime, double variableProcessingTime) {
        
        // record measurement
        fixedTimeMeasurements[type].add(fixedProcessingTime);
        variableTimeMeasurements[type].add(variableProcessingTime);
        
        // summarize samples if necessary
        if(fixedTimeMeasurements[type].size() == fixedSamplesToMeasure[type]) {
            
            double result = summarizeMeasurements(fixedTimeMeasurements[type]);
            result = slowStart(result, type, fixedTimeAverages, fixedSamplesToMeasure);
            listener.updateFixedProcessingTimeAverage(type, result, isForInternalRequests);
            fixedTimeAverages[type] = result;
            fixedTimeMeasurements[type].clear();
            
        }        
        
        // summarize samples if necessary
        if(variableTimeMeasurements[type].size() == variableSamplesToMeasure[type]) {
            
            double result = summarizeMeasurements(variableTimeMeasurements[type]);
            result = slowStart(result, type, variableTimeAverages, variableSamplesToMeasure);
            listener.updateVariableProcessingTimeAverage(type, result, isForInternalRequests);
            variableTimeAverages[type] = result;
            variableTimeMeasurements[type].clear();
        }
    }

    /* (non-Javadoc)
     * @see org.xtreemfs.common.olp.Monitor#summarizeMeasurements(double[])
     */
    @Override
    double summarizeMeasurements(double[] measurements) {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see org.xtreemfs.common.olp.Monitor#summarizeMeasurements(java.util.List)
     */
    @Override
    double summarizeMeasurements(List<Double> measurements) {

        double avg = 0;
        for (double sample : measurements) {
            avg += sample;
        }
        return avg / measurements.size();
    }
    
/*
 * private methods
 */
    
    /**
     * <p>Method to apply a slow-start alike mechanism to the amount of samples to measure before summary.</p>
     * 
     * @param current
     * @param type
     * @param historicData
     * @param tuningKnob
     * @return the average value to use after the evaluation of the slow-start criterion.
     */
    private double slowStart(double current, int type, double[] historicData, int[] tuningKnob) {
        double result = current;
        
        if (current < historicData[type]) {
            if ((historicData[type] - current) < AVERAGE_DIFFERENCE_THRESHOLD) {
                tuningKnob[type] = (tuningKnob[type] > 1) ? (tuningKnob[type] - 1) : 1;
            } else if ((historicData[type] / current) > AVERAGE_QUOTIEN_THRESHOLD) {
                tuningKnob[type] *= 2;
                result = (current + historicData[type]) / 2.0;
            }
        
        // there was nothing measured at all (reset to initial sample amount)
        } else if (current == historicData[type] && current == 0.0) {
            tuningKnob[type] = INITIAL_SAMPLE_AMOUNT;
            
        // current >= historicData[type]
        } else {
            if ((current - historicData[type]) < AVERAGE_DIFFERENCE_THRESHOLD) {
                tuningKnob[type] = (tuningKnob[type] > 1) ? (tuningKnob[type] / 2) : 1;
            } else if ((current / historicData[type]) > AVERAGE_QUOTIEN_THRESHOLD) {
                tuningKnob[type]++;
            }
        }
        
        return result;
    }
}
