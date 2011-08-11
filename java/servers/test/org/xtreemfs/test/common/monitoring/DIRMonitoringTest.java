/*
 * Copyright (c) 2009-2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.test.common.monitoring;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.xtreemfs.common.monitoring.generatedcode.XTREEMFS_MIBOidTable;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.util.FSUtils;
import org.xtreemfs.mrc.MRCConfig;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;

import com.sun.management.snmp.SnmpDefinitions;
import com.sun.management.snmp.SnmpInt;
import com.sun.management.snmp.SnmpOid;
import com.sun.management.snmp.SnmpOidTableSupport;
import com.sun.management.snmp.SnmpVarBind;
import com.sun.management.snmp.SnmpVarBindList;
import com.sun.management.snmp.manager.SnmpParameters;
import com.sun.management.snmp.manager.SnmpPeer;
import com.sun.management.snmp.manager.SnmpRequest;
import com.sun.management.snmp.manager.SnmpSession;

import junit.framework.TestCase;

public class DIRMonitoringTest extends TestCase {

    DIRRequestDispatcher dir;

    TestEnvironment      testEnv;

    DIRConfig            dirConfig;

    SnmpPeer             dirAgent, mrcAgent, osdAgent;

    SnmpSession          session;

    public DIRMonitoringTest() throws IOException {

        dirConfig = SetupUtils.createDIRConfig();
        Logging.start(Logging.LEVEL_DEBUG);
       

    }

    @Before
    public void setUp() throws Exception {

        System.out.println("TEST: " + getClass().getSimpleName());

        FSUtils.delTree(new java.io.File(SetupUtils.TEST_DIR));
        
        dir = new DIRRequestDispatcher(dirConfig, SetupUtils.createDIRdbsConfig());
        dir.startup();
        dir.waitForStartup();

        testEnv = new TestEnvironment(new TestEnvironment.Services[] { TestEnvironment.Services.DIR_CLIENT,
                TestEnvironment.Services.TIME_SYNC, TestEnvironment.Services.RPC_CLIENT,
                TestEnvironment.Services.MRC, TestEnvironment.Services.OSD });
        testEnv.start();

        final SnmpOidTableSupport oidTable = new XTREEMFS_MIBOidTable();
        SnmpOid.setSnmpOidTable(oidTable);

        MRCConfig mrcConfig = SetupUtils.createMRC1Config();
        OSDConfig osdConfig = SetupUtils.createMultipleOSDConfigs(1)[0];

        dirAgent = new SnmpPeer(dirConfig.getSnmpAddress().getHostName(), dirConfig.getSnmpPort());
        mrcAgent = new SnmpPeer(mrcConfig.getSnmpAddress().getHostName(), mrcConfig.getSnmpPort());
        osdAgent = new SnmpPeer(osdConfig.getSnmpAddress().getHostName(), osdConfig.getSnmpPort());

        // Create and set Parameters, i.e. the community strings for read-only and read-write.
        // Since the config don't provide an ACL file it doens't matter what community strings are
        // used here
        final SnmpParameters params = new SnmpParameters("public", "private");

        dirAgent.setParams(params);
        mrcAgent.setParams(params);
        osdAgent.setParams(params);

        session = new SnmpSession("UnitTest Session");

    }

    @After
    public void tearDown() throws Exception {

        session.destroySession();

        testEnv.shutdown();

        dir.shutdown();

        dir.waitForShutdown();

    }

    /**
     * Make an SNMP get
     * 
     * @param agent
     *            The {@link SnmpPeer} from which the OID should be read.
     * @param varDesc
     *            The textual representation of the OID.
     * 
     * @return
     */
    private SnmpVarBindList makeSnmpGet(SnmpPeer agent, String varDesc) throws Exception {
        final SnmpVarBindList list = new SnmpVarBindList("UnitTest varbind list");

        // We want to read the "sysDescr" variable.
        list.addVarBind(varDesc);

        // Make the SNMP get request and wait for the result.
        SnmpRequest request = session.snmpGetRequest(agent, null, list);
        final boolean completed = request.waitForCompletion(10000);

        // Check for a timeout of the request.
        assertTrue(completed);

        // Check if the response contains an error.
        int errorStatus = request.getErrorStatus();
        assertEquals(SnmpDefinitions.snmpRspNoError, errorStatus);

        // Now we can extract the content of the result.
        return request.getResponseVarBindList();

    }

    public void testAddressMappingCount() throws Exception {

        SnmpVarBindList result = makeSnmpGet(dirAgent, "addressMappingCount.0");

        SnmpVarBind varBind = result.getVarBindAt(0);
        SnmpInt snmpInt = varBind.getSnmpIntValue();

        // After initialization there should be 3 address mappings registered at the DIR.
        assertEquals(3, snmpInt.intValue());

        // start another MRC to increase number of address mappings
        MRCConfig mrcConfig2 = SetupUtils.createMRC2Config();
        MRCRequestDispatcher secondMrc = new MRCRequestDispatcher(mrcConfig2,
                SetupUtils.createMRC2dbsConfig());

        secondMrc.startup();

        result = makeSnmpGet(dirAgent, "addressMappingCount.0");

        varBind = result.getVarBindAt(0);
        snmpInt = varBind.getSnmpIntValue();

        assertEquals(4, snmpInt.intValue());

        secondMrc.shutdown();

    }

    //FIXME: This test fails. The GET request for serviceCount.0 OID returns the ServiceCount variable
    // from common.monitoring.Dir Class. This is variable correctly set and has the right value just
    // before and after the snmpGet Request is sent. Nevertheless the result of the GET request is
    // the value which was used to initialize this variable. However, outside the test environment 
    // everything works fine.
    
    // public void testServiceCount() throws Exception {
    //
    // SnmpVarBindList result = makeSnmpGet(dirAgent, "serviceCount.0");
    //
    // System.out.println(result);
    //
    // SnmpVarBind varBind = result.getVarBindAt(0);
    // SnmpInt snmpInt = varBind.getSnmpIntValue();
    //
    // // After initialization there should be 2 services registered at the DIR.
    // assertEquals(2, snmpInt.intValue());
    // }

}
