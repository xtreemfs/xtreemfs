/*
 * Copyright (c) 2009-2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.test.common.monitoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.common.clients.Client;
import org.xtreemfs.common.monitoring.generatedcode.XTREEMFS_MIBOidTable;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.mrc.MRCConfig;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.AccessControlPolicyType;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.test.TestHelper;

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

public class DIRMonitoringTest {
    @Rule
    public final TestRule testLog = TestHelper.testLog;

    TestEnvironment      testEnv;

    SnmpPeer             dirAgent, mrcAgent, osdAgent;

    SnmpSession          session;

    @BeforeClass
    public static void initializeTest() throws Exception {
        Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);
    }

    @Before
    public void setUp() throws Exception {

        testEnv = new TestEnvironment(new TestEnvironment.Services[] { TestEnvironment.Services.DIR_SERVICE,
                TestEnvironment.Services.DIR_CLIENT, TestEnvironment.Services.TIME_SYNC,
                TestEnvironment.Services.RPC_CLIENT, TestEnvironment.Services.MRC, TestEnvironment.Services.OSD });
        testEnv.start();

        final SnmpOidTableSupport oidTable = new XTREEMFS_MIBOidTable();
        SnmpOid.setSnmpOidTable(oidTable);

        DIRConfig dirConfig = SetupUtils.createDIRConfig();
        MRCConfig mrcConfig = SetupUtils.createMRC1Config();
        OSDConfig osdConfig = SetupUtils.createMultipleOSDConfigs(1)[0];

        dirAgent = new SnmpPeer(dirConfig.getSnmpAddress().getHostName(), dirConfig.getSnmpPort());
        mrcAgent = new SnmpPeer(mrcConfig.getSnmpAddress().getHostName(), mrcConfig.getSnmpPort());
        osdAgent = new SnmpPeer(osdConfig.getSnmpAddress().getHostName(), osdConfig.getSnmpPort());

        // Create and set Parameters, i.e. the community strings for read-only
        // and read-write.
        // Since the config don't provide an ACL file it doens't matter what
        // community strings are
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

    @Test
    public void testAddressMappingCount() throws Exception {

        SnmpVarBindList result = makeSnmpGet(dirAgent, "addressMappingCount.0");

        SnmpVarBind varBind = result.getVarBindAt(0);
        SnmpInt snmpInt = varBind.getSnmpIntValue();

        // After initialization there should be 3 address mappings registered at
        // the DIR.
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

    @Test
    public void testServiceCount() throws Exception {

        SnmpVarBindList result = makeSnmpGet(dirAgent, "serviceCount.0");

        SnmpVarBind varBind = result.getVarBindAt(0);
        SnmpInt snmpInt = varBind.getSnmpIntValue();

        // After initialization there should be 2 services registered at the
        // DIR.
        assertEquals(2, snmpInt.intValue());

        Client c = new Client(new InetSocketAddress[] { testEnv.getDIRAddress() }, 10000, 60000, null);
        c.start();
        UserCredentials uc = UserCredentials.newBuilder().setUsername("test").addGroups("test").build();

        c.createVolume("foobar", RPCAuthentication.authNone, uc, SetupUtils.getStripingPolicy(64, 1),
                AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL, 0777);
        result = makeSnmpGet(dirAgent, "serviceCount.0");
        varBind = result.getVarBindAt(0);
        snmpInt = varBind.getSnmpIntValue();

        // Now there should be 3 services.
        assertEquals(3, snmpInt.intValue());

        c.deleteVolume("foobar", RPCAuthentication.authNone, uc);
        result = makeSnmpGet(dirAgent, "serviceCount.0");
        varBind = result.getVarBindAt(0);
        snmpInt = varBind.getSnmpIntValue();

        // Now there should be 2 services again.
        assertEquals(2, snmpInt.intValue());

        c.stop();

        // start another MRC to increase number of registred services
        MRCConfig mrcConfig2 = SetupUtils.createMRC2Config();
        MRCRequestDispatcher secondMrc = new MRCRequestDispatcher(mrcConfig2,
                SetupUtils.createMRC2dbsConfig());

        secondMrc.startup();

        result = makeSnmpGet(dirAgent, "serviceCount.0");
        varBind = result.getVarBindAt(0);
        snmpInt = varBind.getSnmpIntValue();
        // Now there should be 3 services.
        assertEquals(3, snmpInt.intValue());

        secondMrc.shutdown();

        result = makeSnmpGet(dirAgent, "serviceCount.0");
        varBind = result.getVarBindAt(0);
        snmpInt = varBind.getSnmpIntValue();
        // Now there should be 2 services again.
        assertEquals(3, snmpInt.intValue());

    }

}
