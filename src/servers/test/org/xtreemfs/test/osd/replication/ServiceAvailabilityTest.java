/*  Copyright (c) 2008 Barcelona Supercomputing Center - Centro Nacional
    de Supercomputacion and Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

    This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
    Grid Operating System, see <http://www.xtreemos.eu> for more details.
    The XtreemOS project has been developed with the financial support of the
    European Commission's IST program under contract #FP6-033576.

    XtreemFS is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 2 of the License, or (at your option)
    any later version.

    XtreemFS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Christian Lorenz (ZIB)
 */
package org.xtreemfs.test.osd.replication;


import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xtreemfs.common.ServiceAvailability;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.test.SetupUtils;

/**
 *
 * 06.04.2009
 */
public class ServiceAvailabilityTest extends TestCase {
    public final static int INITIAL_TIMEOUT = 100; // 0.1 second
    public final static int CLEANUP_INTERVAL = 1000; // 1 second
    public final static int MAX_LAST_ACCESS = 1000 * 3; // 3 seconds

    ServiceAvailability serviceAvailability;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());
        Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);

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
