/*
 * Copyright (c) 2009-2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.monitoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.babudb.BabuDBFactory;
import org.xtreemfs.common.monitoring.generatedcode.XTREEMFS_MIBOidTable;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.mrc.MRCConfig;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceConstants;
import org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceConstants;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;
import org.xtreemfs.SetupUtils;
import org.xtreemfs.TestEnvironment;
import org.xtreemfs.TestHelper;

import com.sun.management.snmp.SnmpDefinitions;
import com.sun.management.snmp.SnmpInt;
import com.sun.management.snmp.SnmpOid;
import com.sun.management.snmp.SnmpOidTableSupport;
import com.sun.management.snmp.SnmpString;
import com.sun.management.snmp.SnmpVarBind;
import com.sun.management.snmp.SnmpVarBindList;
import com.sun.management.snmp.manager.SnmpParameters;
import com.sun.management.snmp.manager.SnmpPeer;
import com.sun.management.snmp.manager.SnmpRequest;
import com.sun.management.snmp.manager.SnmpSession;

public class GeneralMonitoringTest {
    @Rule
    public final TestRule   testLog = TestHelper.testLog;

    private TestEnvironment testEnv;

    private DIRConfig       dirConfig;

    private MRCConfig       mrcConfig;

    private OSDConfig       osdConfig;

    private SnmpPeer        dirAgent, mrcAgent, osdAgent;

    private SnmpSession     session;

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

        dirConfig = SetupUtils.createDIRConfig();
        mrcConfig = SetupUtils.createMRC1Config();
        osdConfig = SetupUtils.createMultipleOSDConfigs(1)[0];

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
    public void testRpcInterface() throws Exception {

        // DIR InterfaceID
        SnmpVarBindList result = makeSnmpGet(dirAgent, "rpcInterface.0");
        SnmpVarBind varBind = result.getVarBindAt(0);
        SnmpInt snmpInt = varBind.getSnmpIntValue();

        assertEquals(DIRServiceConstants.INTERFACE_ID, snmpInt.intValue());

        // MRC InterfaceID
        result = makeSnmpGet(mrcAgent, "rpcInterface.0");
        varBind = result.getVarBindAt(0);
        snmpInt = varBind.getSnmpIntValue();

        assertEquals(MRCServiceConstants.INTERFACE_ID, snmpInt.intValue());

        // OSD InterfaceID
        result = makeSnmpGet(osdAgent, "rpcInterface.0");
        varBind = result.getVarBindAt(0);
        snmpInt = varBind.getSnmpIntValue();

        assertEquals(OSDServiceConstants.INTERFACE_ID, snmpInt.intValue());

    }

    @Test
    public void testDatabaseVersion() throws Exception {
        // DIR BaduDB Version
        SnmpVarBindList result = makeSnmpGet(dirAgent, "databaseVersion.0");
        SnmpVarBind varBind = result.getVarBindAt(0);
        SnmpString snmpString = varBind.getSnmpStringValue();

        assertEquals(BabuDBFactory.BABUDB_VERSION, snmpString.toString());

        // MRC BaduDB Version
        result = makeSnmpGet(mrcAgent, "databaseVersion.0");
        varBind = result.getVarBindAt(0);
        snmpString = varBind.getSnmpStringValue();

        assertEquals(BabuDBFactory.BABUDB_VERSION, snmpString.toString());
    }

    @Test
    public void testTcpPort() throws Exception {

        // DIR TCPPort
        SnmpVarBindList result = makeSnmpGet(dirAgent, "tcpPort.0");
        SnmpVarBind varBind = result.getVarBindAt(0);
        SnmpInt snmpInt = varBind.getSnmpIntValue();

        assertEquals(dirConfig.getPort(), snmpInt.intValue());

        // MRC TCPPort
        result = makeSnmpGet(mrcAgent, "tcpPort.0");
        varBind = result.getVarBindAt(0);
        snmpInt = varBind.getSnmpIntValue();

        assertEquals(mrcConfig.getPort(), snmpInt.intValue());

        // OSD TCPPort
        result = makeSnmpGet(osdAgent, "tcpPort.0");
        varBind = result.getVarBindAt(0);
        snmpInt = varBind.getSnmpIntValue();

        assertEquals(osdConfig.getPort(), snmpInt.intValue());

    }

    @Test
    public void testDebugLevel() throws Exception {
        // DIR DebugLevel
        SnmpVarBindList result = makeSnmpGet(dirAgent, "debugLevel.0");
        SnmpVarBind varBind = result.getVarBindAt(0);
        SnmpInt snmpInt = varBind.getSnmpIntValue();

        assertEquals(dirConfig.getDebugLevel(), snmpInt.intValue());

        // MRC DebugLevel
        result = makeSnmpGet(mrcAgent, "debugLevel.0");
        varBind = result.getVarBindAt(0);
        snmpInt = varBind.getSnmpIntValue();

        assertEquals(mrcConfig.getDebugLevel(), snmpInt.intValue());

        // OSD DebugLevel
        result = makeSnmpGet(osdAgent, "debugLevel.0");
        varBind = result.getVarBindAt(0);
        snmpInt = varBind.getSnmpIntValue();

        assertEquals(osdConfig.getDebugLevel(), snmpInt.intValue());
    }

    @Test
    public void testIsRunning() throws Exception {
        // DIR isRunning
        SnmpVarBindList result = makeSnmpGet(dirAgent, "isRunning.0");
        SnmpVarBind varBind = result.getVarBindAt(0);
        SnmpString snmpString = varBind.getSnmpStringValue();

        assertEquals("ONLINE", snmpString.toString());

        // MRC isRunning
        result = makeSnmpGet(mrcAgent, "isRunning.0");
        varBind = result.getVarBindAt(0);
        snmpString = varBind.getSnmpStringValue();

        assertEquals("ONLINE", snmpString.toString());

        // OSD isRunning
        result = makeSnmpGet(osdAgent, "isRunning.0");
        varBind = result.getVarBindAt(0);
        snmpString = varBind.getSnmpStringValue();

        assertEquals("ONLINE", snmpString.toString());

    }

    @Test
    public void testServiceType() throws Exception {
        // DIR TYPE
        SnmpVarBindList result = makeSnmpGet(dirAgent, "serviceType.0");
        SnmpVarBind varBind = result.getVarBindAt(0);
        SnmpString snmpString = varBind.getSnmpStringValue();

        assertEquals("DIR", snmpString.toString());

        // MRC TYPE
        result = makeSnmpGet(mrcAgent, "serviceType.0");
        varBind = result.getVarBindAt(0);
        snmpString = varBind.getSnmpStringValue();

        assertEquals("MRC", snmpString.toString());

        // OSD TYPE
        result = makeSnmpGet(osdAgent, "serviceType.0");
        varBind = result.getVarBindAt(0);
        snmpString = varBind.getSnmpStringValue();

        assertEquals("OSD", snmpString.toString());
    }

    @Test
    public void testServiceUUID() throws Exception {
        // DIR UUID
        SnmpVarBindList result = makeSnmpGet(dirAgent, "serviceUUID.0");
        SnmpVarBind varBind = result.getVarBindAt(0);
        SnmpString snmpString = varBind.getSnmpStringValue();

        assertEquals(dirConfig.getUUID().toString(), snmpString.toString());

        // MRC UUID
        result = makeSnmpGet(mrcAgent, "serviceUUID.0");
        varBind = result.getVarBindAt(0);
        snmpString = varBind.getSnmpStringValue();

        assertEquals(mrcConfig.getUUID().toString(), snmpString.toString());

        // OSD UUID
        result = makeSnmpGet(osdAgent, "serviceUUID.0");
        varBind = result.getVarBindAt(0);
        snmpString = varBind.getSnmpStringValue();

        assertEquals(osdConfig.getUUID().toString(), snmpString.toString());
    }

}
