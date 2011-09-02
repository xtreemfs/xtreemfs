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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @see Actuator
 * 
 * @author fx.langner
 * @version 1.00, 09/02/11
 */
public class ActuatorTest {

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
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for {@link org.xtreemfs.common.olp.Actuator#hasAdmission(double, double)}.
     */
    @Test
    public void testHasAdmission() {
        Random r = new Random();
        Actuator a = new Actuator();
        
        for (int i = 0; i < 100; i++) {
            double t1 = r.nextDouble();
            double t2 = r.nextDouble();
            if (t1 > t2) {
                assertTrue(a.hasAdmission(t1, t2));
                assertFalse(a.hasAdmission(t2, t1));
            } else {
                assertTrue(a.hasAdmission(t2, t1));
                if (t1 == t2) {
                    assertTrue(a.hasAdmission(t1, t2));
                } else {
                    assertFalse(a.hasAdmission(t1, t2));
                }
            }
        }
    }
}