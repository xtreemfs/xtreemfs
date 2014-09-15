/*
 * Copyright (c) 2008-2011 by Christian Lorenz,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.test.osd.replication;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.common.ServiceAvailability;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestHelper;

/**
 *
 * 06.04.2009
 */
public class ServiceAvailabilityTest {
    @Rule
    public final TestRule   testLog          = TestHelper.testLog;

    public final static int INITIAL_TIMEOUT = 100; // 0.1 second
    public final static int CLEANUP_INTERVAL = 1000; // 1 second
    public final static int MAX_LAST_ACCESS = 1000 * 3; // 3 seconds

    ServiceAvailability serviceAvailability;

    @BeforeClass
    public static void initializeTest() throws Exception {
        Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        serviceAvailability = new ServiceAvailability(INITIAL_TIMEOUT, MAX_LAST_ACCESS, CLEANUP_INTERVAL);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        serviceAvailability.shutdown();
    }

    /**
     * IMPORTANT: Tests timeouts. On old slow PCs this test could fail, although the class works correct.
     * @throws Exception
     */
    @Test
    public void testCorrectTimeouts() throws Exception {
        ServiceUUID service = new ServiceUUID("UUID:localhost:36840");
        
        // service available
        assertTrue(serviceAvailability.isServiceAvailable(service));
        assertTrue(serviceAvailability.isServiceAvailable(service));
        Thread.sleep(INITIAL_TIMEOUT);
        assertTrue(serviceAvailability.isServiceAvailable(service));

        // service not available
        serviceAvailability.setServiceWasNotAvailable(service);
        assertFalse(serviceAvailability.isServiceAvailable(service));
        Thread.sleep(INITIAL_TIMEOUT);
        assertFalse(serviceAvailability.isServiceAvailable(service));
        
        // service available again
        Thread.sleep(INITIAL_TIMEOUT*2);
        assertTrue(serviceAvailability.isServiceAvailable(service));

        // service not available again
        serviceAvailability.setServiceWasNotAvailable(service);
        serviceAvailability.setServiceWasNotAvailable(service);
        Thread.sleep(INITIAL_TIMEOUT*7);
        assertFalse(serviceAvailability.isServiceAvailable(service));
        Thread.sleep(INITIAL_TIMEOUT*9);
        assertTrue(serviceAvailability.isServiceAvailable(service));
    }
    
    /**
     * IMPORTANT: Tests timeouts. On old slow PCs this test could fail, although the class works correct.
     * @throws Exception
     */
    @Test
    public void testServiceRemoval() throws Exception {
        List<ServiceUUID> services = new ArrayList<ServiceUUID>();
        int port = 36840;
        for (int i = 0; i < 10; i++)
            services.add(new ServiceUUID("UUID:localhost:"+(port++)));
        
        // test correct removal of elements if last access is too long ago
        for(ServiceUUID service : services) {
            // add to "map"
            assertTrue(serviceAvailability.isServiceAvailable(service));
            // timeout > MAX_LAST_ACCESS
            for (int timeout = INITIAL_TIMEOUT; timeout > MAX_LAST_ACCESS+CLEANUP_INTERVAL; timeout = timeout * 2) {
                serviceAvailability.setServiceWasNotAvailable(service);
            }
        }
        Thread.sleep(CLEANUP_INTERVAL);
        // all OSDs must be available again, although the timeout is not over; because they have been removed
        for(ServiceUUID service : services) {
            assertTrue(serviceAvailability.isServiceAvailable(service));
        }
    }
}
