/*
 * Copyright (c) 2009-2013 by Christoph Kleineweber,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.scheduler.simulator;

import java.util.List;
import org.xtreemfs.scheduler.data.OSDDescription.OSDType;
import org.xtreemfs.scheduler.data.OSDPerformanceDescription;

/**
 *
 * @author Christoph Kleineweber <kleineweber@zib.de>
 */
public class SchedulerConfig {
    private double seqentialReservationProbability;
    private double randomReservationProbability;
    private double bestEffortProbability;
    private double coldStorageProbability;
    private List<OSDPerformanceDescription> osdDescriptions;
    private List<OSDType> osdTypes;
    private List<Integer> osdNumbers;
    private double minAngle;
    private double capacityGain; 
    private double randomIOGain;
    private double streamingGain;
    private boolean preferUsedOSDs;
    private double arrivalRate;
    private double leaveRate;
    private long simulationSteps;
    private int queueCapacity;

    public SchedulerConfig(double seqentialReservationProbability,
            double randomReservationProbability,
            double bestEffortProbability,
            double coldStorageProbability,
            List<OSDPerformanceDescription> osdTypes,
            List<Integer> osdNumbers,
            double minAngle,
            double capacityGain,
            double randomIOGain,
            double streamingGain,
            boolean preferusedOSDs,
            double arrivalRate,
            double leaveRate,
            long simulationSteps,
            int queueCapacity) {
        this.seqentialReservationProbability = seqentialReservationProbability;
        this.randomReservationProbability = randomReservationProbability;
        this.bestEffortProbability = bestEffortProbability;
        this.coldStorageProbability = coldStorageProbability;
        this.osdDescriptions = osdTypes;
        this.osdNumbers = osdNumbers;
        this.minAngle = minAngle;
        this.capacityGain = capacityGain;
        this.randomIOGain = randomIOGain;
        this.streamingGain = streamingGain;
        this.preferUsedOSDs = preferusedOSDs;
        this.arrivalRate = arrivalRate;
        this.leaveRate = leaveRate;
        this.simulationSteps = simulationSteps;
        this.setQueueCapacity(queueCapacity);
    }
    
    /**
     * @return the seqentialReservationProbability
     */
    public double getSeqentialReservationProbability() {
        return seqentialReservationProbability;
    }

    /**
     * @return the randomReservationProbability
     */
    public double getRandomReservationProbability() {
        return randomReservationProbability;
    }

    /**
     * @return the bestEffortProbability
     */
    public double getBestEffortProbability() {
        return bestEffortProbability;
    }

    /**
     * @return the coldStorageProbability
     */
    public double getColdStorageProbability() {
        return coldStorageProbability;
    }

    /**
     * @return the osdTypes
     */
    public List<OSDPerformanceDescription> getOsdDescriptions() {
        return osdDescriptions;
    }

    /**
     * @return the osdNumbers
     */
    public List<Integer> getOsdNumbers() {
        return osdNumbers;
    }

    /**
     * @return the osdTypes
     */
    public List<OSDType> getOsdTypes() {
        return osdTypes;
    }

    /**
     * @return the minAngle
     */
    public double getMinAngle() {
        return minAngle;
    }

    /**
     * @return the capacityGain
     */
    public double getCapacityGain() {
        return capacityGain;
    }

    /**
     * @return the randomIOGain
     */
    public double getRandomIOGain() {
        return randomIOGain;
    }

    /**
     * @return the streamingGain
     */
    public double getStreamingGain() {
        return streamingGain;
    }

    /**
     * @return the preferUsedOSDs
     */
    public boolean isPreferUsedOSDs() {
        return preferUsedOSDs;
    }

    /**
     * @return the arrivalRate
     */
    public double getArrivalRate() {
        return arrivalRate;
    }

    /**
     * @return the leaveRate
     */
    public double getLeaveRate() {
        return leaveRate;
    }

    public long getSimulationSteps() {
        return simulationSteps;
    }

    public void setSimulationSteps(long simulationSteps) {
        this.simulationSteps = simulationSteps;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }
}
