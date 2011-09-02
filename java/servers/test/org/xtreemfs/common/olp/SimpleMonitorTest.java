/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.olp;

import static org.junit.Assert.*;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.xtreemfs.common.olp.Monitor.PerformanceMeasurementListener;

/**
 * @see Monitor 
 * @see SimpleMonitor
 * 
 * @author fx.langner
 * @version 1.00, 09/02/11
 */
public class SimpleMonitorTest {

    private Monitor monitor;
    
    private final AtomicInteger fixedRqId = new AtomicInteger(-1);
    private final AtomicLong fixedRqResult = new AtomicLong(-1);
    
    private final AtomicInteger variableRqId = new AtomicInteger(-1);
    private final AtomicLong variableRqResult = new AtomicLong(-1);
    
    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        monitor = new SimpleMonitor(3, new PerformanceMeasurementListener() {
            
            @Override
            public void updateVariableProcessingTimeAverage(int type, double value) {
                variableRqId.set(type);
                variableRqResult.set(Double.doubleToLongBits(value));
            }
            
            @Override
            public void updateFixedProcessingTimeAverage(int type, double value) {
                fixedRqId.set(type);
                fixedRqResult.set(Double.doubleToLongBits(value));
            }
        });
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for {@link org.xtreemfs.common.olp.SimpleMonitor#record(int, double, double)}.
     */
    @Test
    public void testRecord() {
        Random r = new Random();
        
        // type 0 has variable and fixed processing time
        // type 1 has fixed processing time only
        // type 2 has variable processing time only    
        double[] fix0 = new double[10];
        double[] var0 = new double[10];
        double[] fix1 = new double[10];
        double[] var2 = new double[10];
        double f0 = 0, f1 = 0, v0 = 0, v2 = 0;
        
        for (int i = 0; i < 10; i++) {
            
            fix0[i] = r.nextDouble();
            var0[i] = r.nextDouble();
            fix1[i] = r.nextDouble();
            var2[i] = r.nextDouble();
            
            f0 += fix0[i];
            f1 += fix1[i];
            v0 += var0[i];
            v2 += var2[i];
        }
        f0 /= 10;
        f1 /= 10;
        v0 /= 10;
        v2 /= 10;
        
        int[] indizes = {0, 1, 1, 2, 2, 0, 1, 0, 2, 0, 
                         0, 2, 2, 2, 0, 1, 1, 2, 1, 0, 
                         2, 1, 1, 2, 1, 0, 0, 1, 0, 2};
        double[][] values = { 
                {fix0[0], var0[0]}, {fix1[0], 0}, {fix1[1], 0}, {0, var2[0]}, {0, var2[1]},
                {fix0[1], var0[1]}, {fix1[2], 0}, {fix0[2], var0[2]}, {0, var2[2]}, {fix0[3], var0[3]},
                {fix0[4], var0[4]}, {0, var2[3]}, {0, var2[4]}, {0, var2[5]}, {fix0[5], var0[5]},
                {fix1[3], 0}, {fix1[4], 0}, {0, var2[6]}, {fix1[5], 0}, {fix0[6], var0[6]},
                {0, var2[7]}, {fix1[6], 1}, {fix1[7], 0}, {0, var2[8]}, {fix1[8], 0},
                {fix0[7], var0[7]}, {fix0[8], var0[8]}, {fix1[9], 0}, {fix0[9], var0[9]}, {0, var2[9]}};
        
        for (int i = 0; i < 27; i++) {
            monitor.record(indizes[i], values[i][0], values[i][1]);
        }
        
        monitor.record(indizes[27], values[27][0], values[27][1]);
        assertEquals(1, fixedRqId.get());
        assertEquals(f1, Double.longBitsToDouble(fixedRqResult.get()), 0.0);
        
        monitor.record(indizes[28], values[28][0], values[28][1]);
        assertEquals(0, fixedRqId.get());
        assertEquals(f0, Double.longBitsToDouble(fixedRqResult.get()), 0.0);
        assertEquals(0, variableRqId.get());
        assertEquals(v0, Double.longBitsToDouble(variableRqResult.get()), 0.0);
        
        monitor.record(indizes[29], values[29][0], values[29][1]);
        assertEquals(2, variableRqId.get());
        assertEquals(v2, Double.longBitsToDouble(variableRqResult.get()), 0.0);
    }
}
