package org.xtreemfs.common.libxtreemfs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.common.libxtreemfs.ClientFactory.ClientType;
import org.xtreemfs.common.libxtreemfs.exceptions.AddressToUUIDNotFoundException;
import org.xtreemfs.common.libxtreemfs.exceptions.VolumeNotFoundException;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.SetupUtils;
import org.xtreemfs.TestEnvironment;
import org.xtreemfs.TestHelper;

public class UUIDResolverTest {
    @Rule
    public final TestRule   testLog = TestHelper.testLog;

    private TestEnvironment testEnv;

    private UserCredentials userCredentials;

    private final Auth      auth = RPCAuthentication.authNone;

    @Before
    public void setUp() throws Exception {
        Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);

        testEnv = new TestEnvironment(new TestEnvironment.Services[] { TestEnvironment.Services.DIR_SERVICE,
                TestEnvironment.Services.DIR_CLIENT, TestEnvironment.Services.TIME_SYNC,
                TestEnvironment.Services.RPC_CLIENT, TestEnvironment.Services.MRC, TestEnvironment.Services.OSD });
        testEnv.start();

        userCredentials = UserCredentials.newBuilder().setUsername("test").addGroups("test").build();
    }

    @After
    public void tearDown() throws Exception {
        testEnv.shutdown();
    }

    @Test
    public void testUUIDResolver() throws Exception {
        final String VOLUME_NAME_1 = "foobar";
        final String VOLUME_NAME_2 = "barfoo";

        Options options = new Options();

        String dirAddress = testEnv.getDIRAddress().getHostName() + ":" + testEnv.getDIRAddress().getPort();

        ClientImplementation client = (ClientImplementation) ClientFactory.createClient(ClientType.JAVA, 
        		dirAddress, userCredentials, null, options);
        client.start();

        UUIDResolver resolver = client;

        String mrcAddress = testEnv.getMRCAddress().getHostName() + ":" + testEnv.getMRCAddress().getPort();

        // Create volumes
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME_1);
        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME_2);

        // get and MRC UUID for the volume. Should be that from the only MRC.
        String uuidString = client.volumeNameToMRCUUID(VOLUME_NAME_1);
        assertEquals(SetupUtils.getMRC1UUID().toString(), uuidString);

        // same for the second volume
        uuidString = resolver.volumeNameToMRCUUID(VOLUME_NAME_2);
        assertEquals(SetupUtils.getMRC1UUID().toString(), uuidString);

        // should also work with snapshots
        uuidString = resolver.volumeNameToMRCUUID(VOLUME_NAME_2 + "@snapshotname");
        assertEquals(SetupUtils.getMRC1UUID().toString(), uuidString);

        // this should work if we use UUIDIterator, too.
        UUIDIterator uuidIterator = new UUIDIterator();
        client.volumeNameToMRCUUID(VOLUME_NAME_1, uuidIterator);
        assertEquals(SetupUtils.getMRC1UUID().toString(), uuidIterator.getUUID());

        // resolve MRC UUID
        String address = resolver.uuidToAddress(SetupUtils.getMRC1UUID().toString());
        assertEquals(testEnv.getMRCAddress().getHostName() + ":" + testEnv.getMRCAddress().getPort(), address);

        // resolve OSD UUID
        address = resolver.uuidToAddress(SetupUtils.createMultipleOSDConfigs(1)[0].getUUID().toString());
        assertEquals(testEnv.getOSDAddress().getHostName() + ":" + testEnv.getOSDAddress().getPort(), address);

        // resolve non existing uuid
        try {
            address = resolver.uuidToAddress("this-is-not-a-valid-uuid");
            fail("Resolve invalid uuid. Should have thrown an exception");
        } catch (AddressToUUIDNotFoundException e) {
        }

        // resolve non exsiting volume name
        try {
            uuidString = resolver.volumeNameToMRCUUID("non-existing-volume");
            fail("Volume doensn't exist! Should have thrown an exception");
        } catch (VolumeNotFoundException e) {
        }

        // resolve non existing volume with uuidIterator.
        try {
            resolver.volumeNameToMRCUUID("non-existing-volume", new UUIDIterator());
            fail("Volume doensn't exist! Should have thrown an exception");
        } catch (VolumeNotFoundException e) {
        }

        // if there is no correct connection the resolver cant resolve and
        // should throw an VolumeNotFoundException
        // Do not retry here to avoid unnecessary lengthy executions.
        options.setMaxTries(1);
        ClientImplementation clientFail = (ClientImplementation) ClientFactory.createClient(ClientType.JAVA, 
        		"doesntexists:44444", userCredentials, null, options);
        clientFail.start();
        UUIDResolver resolverFail = clientFail;
        try {
            resolverFail.volumeNameToMRCUUID("dummy-name");
            fail("There was no correct client initialization. Shouldn't " + "be able to resovle something");
        } catch (VolumeNotFoundException e) {
        }

        // should work with uuidIterator, too
        try {
            resolverFail.volumeNameToMRCUUID("dummy-name", new UUIDIterator());
            fail("There was no correct client initialization. Shouldn't " + "be able to resovle something");
        } catch (VolumeNotFoundException e) {
        }

        // shutdown the client
        client.shutdown();
    }

}
