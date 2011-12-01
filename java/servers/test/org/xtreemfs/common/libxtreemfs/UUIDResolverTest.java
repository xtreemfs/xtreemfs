package org.xtreemfs.common.libxtreemfs;

import java.io.IOException;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.xtreemfs.common.libxtreemfs.Client;
import org.xtreemfs.common.libxtreemfs.ClientImplementation;
import org.xtreemfs.common.libxtreemfs.Options;
import org.xtreemfs.common.libxtreemfs.UUIDIterator;
import org.xtreemfs.common.libxtreemfs.UUIDResolver;
import org.xtreemfs.common.libxtreemfs.exceptions.AddressToUUIDNotFoundException;
import org.xtreemfs.common.libxtreemfs.exceptions.VolumeNotFoundException;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.util.FSUtils;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;

public class UUIDResolverTest extends TestCase {
	private DIRRequestDispatcher dir;

	private TestEnvironment testEnv;

	private DIRConfig dirConfig;

	private UserCredentials userCredentials;

	private Auth auth = RPCAuthentication.authNone;

	/**
     * 
     */
	public UUIDResolverTest() throws IOException {
		dirConfig = SetupUtils.createDIRConfig();
		Logging.start(Logging.LEVEL_DEBUG);
	}

	@Before
	public void setUp() throws Exception {
		System.out.println("TEST: " + getClass().getSimpleName());

		FSUtils.delTree(new java.io.File(SetupUtils.TEST_DIR));

		dir = new DIRRequestDispatcher(dirConfig,
				SetupUtils.createDIRdbsConfig());
		dir.startup();
		dir.waitForStartup();

		testEnv = new TestEnvironment(new TestEnvironment.Services[] {
				TestEnvironment.Services.DIR_CLIENT,
				TestEnvironment.Services.TIME_SYNC,
				TestEnvironment.Services.RPC_CLIENT,
				TestEnvironment.Services.MRC, TestEnvironment.Services.OSD });
		testEnv.start();

		userCredentials = UserCredentials.newBuilder().setUsername("test")
				.addGroups("test").build();
	}

	@After
	public void tearDown() throws Exception {
		testEnv.shutdown();

		dir.shutdown();

		dir.waitForShutdown();
	}

	public void testUUIDResolver() throws Exception {
		final String VOLUME_NAME_1 = "foobar";
		final String VOLUME_NAME_2 = "barfoo";

		Options options = new Options(5000, 10000, 4, 2);

		String dirAddress = testEnv.getDIRAddress().getHostName() + ":"
				+ testEnv.getDIRAddress().getPort();

		ClientImplementation client = (ClientImplementation) Client
				.createClient(dirAddress, userCredentials, null, options);
		client.start();

		UUIDResolver resolver = client;

		String mrcAddress = testEnv.getMRCAddress().getHostName() + ":"
				+ testEnv.getMRCAddress().getPort();

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
		uuidString = resolver.volumeNameToMRCUUID(VOLUME_NAME_2+"@snapshotname");
		assertEquals(SetupUtils.getMRC1UUID().toString(), uuidString);
		
		// this should work if we use UUIDIterator, too.
		UUIDIterator uuidIterator = new UUIDIterator();
		client.volumeNameToMRCUUID(VOLUME_NAME_1, uuidIterator);
		assertEquals(SetupUtils.getMRC1UUID().toString(),
				uuidIterator.getUUID());

		// resolve MRC UUID
		String address = resolver.uuidToAddress(SetupUtils.getMRC1UUID()
				.toString());
		assertEquals(testEnv.getMRCAddress().getHostName() + ":"
				+ testEnv.getMRCAddress().getPort(), address);

		// resolve OSD UUID
		address = resolver
				.uuidToAddress(SetupUtils.createMultipleOSDConfigs(1)[0]
						.getUUID().toString());
		assertEquals(testEnv.getOSDAddress().getHostName() + ":"
				+ testEnv.getOSDAddress().getPort(), address);

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
		ClientImplementation clientFail = (ClientImplementation) Client
				.createClient("doesntexists:44444", userCredentials, null,
						options);
		clientFail.start();
		UUIDResolver resolverFail = clientFail;
		try {
			resolverFail.volumeNameToMRCUUID("dummy-name");
			fail("There was no correct client initialization. Shouldn't " +
					"be able to resovle something");
		} catch (VolumeNotFoundException e) {
		}
		
		// should work with uuidIterator, too
		try {
			resolverFail.volumeNameToMRCUUID("dummy-name", new UUIDIterator());
			fail("There was no correct client initialization. Shouldn't " +
					"be able to resovle something");
		} catch (VolumeNotFoundException e) {
		}

		// shutdown the client
		client.shutdown();
	}

}
