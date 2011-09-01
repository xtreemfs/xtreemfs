/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.olp;

/**
 * <p>Simple Monitor implementation that uses the average-metric on a fixed amount of samples for calculating 
 * performance information of this stage.</p>
 * 
 * <p>Methods of this class are not thread safe, because processing of a request is assumed to be single threaded.</p>
 * 
 * @author flangner
 * @since 08/18/2011
 * @version 1.0
 */
class SimpleMonitor implements Monitor {
    
    /**
     * <p>Number of samples to collect before calculating there average.</p>
     */
    private final static int SAMPLE_AMOUNT = 10;
    
    /**
     * <p>Contains samples of fixed time measurements in ms of different types of requests.</p>
     */
    private final double[][] fixedTimeMeasurements;
    
    /**
     * <p>Contains samples of variable time measurements in ms/byte of different types of requests.</p>
     */
    private final double[][] variableTimeMeasurements;
    
    /**
     * <p>Contains the current indices for the next sample to insert for different types of requests.</p>
     */
    private final int[]      measurmentIndex;
    
    /**
     * TODO extract interface (listener will receive information in ms || ms/byte)
     * <p>Listener to notify about new summaries of collected samples.</p>
     */
    private final Controller controller;
    
    /**
     * <p>Default constructor initializing necessary fields for collecting measurement samples.</p>
     * 
     * @param numTypes - amount of different request types expected.
     * @param controller - to send the summarized performance information to.
     */
    SimpleMonitor(int numTypes, Controller controller) {
        
        this.controller = controller;
        fixedTimeMeasurements = new double[numTypes][SAMPLE_AMOUNT];
        variableTimeMeasurements = new double[numTypes][SAMPLE_AMOUNT];
        measurmentIndex = new int[numTypes];
        
        for (int i = 0; i < numTypes; i++) {
            fixedTimeMeasurements[i] = new double[SAMPLE_AMOUNT];
            variableTimeMeasurements[i] = new double[SAMPLE_AMOUNT];
        }
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.olp.Monitor#record(int, double, double)
     */
    @Override
    public void record(int type, double fixedProcessingTime, double variableProcessingTime) {
        
        // record measurement
        fixedTimeMeasurements[type][measurmentIndex[type]] = fixedProcessingTime;
        variableTimeMeasurements[type][measurmentIndex[type]] = variableProcessingTime;
        measurmentIndex[type]++;
        
        // summarize samples if necessary
        if(measurmentIndex[type] == SAMPLE_AMOUNT) {
            
            // summarize fixed time proportion
            double avg = 0;
            for (double sample : fixedTimeMeasurements[type]) {
                avg += sample;
            }
            controller.updateFixedProcessingTimeAverage(type, avg / SAMPLE_AMOUNT);
            
            avg = 0;
            for (double sample : variableTimeMeasurements[type]) {
                avg += sample;
            }
            controller.updateVariableProcessingTimeAverage(type, avg / SAMPLE_AMOUNT);
            
            measurmentIndex[type] = 0;
        }
    }
}
