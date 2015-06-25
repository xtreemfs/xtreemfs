package org.xtreemfs.sandbox;

import java.io.IOException;
import java.util.Arrays;

import org.xtreemfs.common.libxtreemfs.Client;
import org.xtreemfs.common.libxtreemfs.ClientFactory;
import org.xtreemfs.common.libxtreemfs.FileHandle;
import org.xtreemfs.common.libxtreemfs.Options;
import org.xtreemfs.common.libxtreemfs.Volume;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.Schemes;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.util.PBRPCServiceURL;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.PORTS;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;

/**
 * Minimal example which uses the libxtreemfs for Java and default SSL certificates from tests/certs/ to test
 * basic functionality of running servers.
 * 
 * The URL to an existing volume has to be provided as first parameter e.g., pbrpcs://localhost/regular
 * 
 * You can easily start a full XtreemFS installation with SSL enabled as follows:
 * 
 * - go to the "tests" directory below the root of the XtreemFS repository
 * 
 * - run "./xtestenv -f -v regular" to start an SSL-enabled test setup which creates a new volume "regular"
 * 
 * - press the Enter key to shut it down again
 * 
 * Don't forget to set the working directory to the root of the XtreemFS repository. Otherwise, the example
 * won't find the default SSL certificates which are also used by the test setup.
 * 
 * E.g., in Eclipse set the working directory to: ${workspace_loc:xtreemfs_server}/../../ when
 * "xtreemfs_server" is the name of the Eclipse project.
 * 
 * @author mberlin
 * 
 */
public class ExampleLibxtreemfsWithSSL {

    /** Directory with testing SSL certificates. Path relative to the root of the repository. */
    public static final String CERT_DIR = "tests/certs/";

    /**
     * @param args
     *            args[0] = URL to existing volume e.g., pbrpcs://localhost/regular
     * @return
     * @throws Exception
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("usage: <URL to existing volume e.g., pbrpcs://localhost/regular>");
            return;
        }

        Client client = null;
        FileHandle fileHandle = null;
        try {
            // Parse command line parameter.
            int lastSlashIndex = args[0].lastIndexOf('/');
            final PBRPCServiceURL url = new PBRPCServiceURL(args[0].substring(0, lastSlashIndex),
                    Schemes.SCHEME_PBRPC, PORTS.DIR_PBRPC_PORT_DEFAULT.getNumber());
            final String volumeName = args[0].substring(lastSlashIndex + 1);

            // Init libxtreemfs
            final Options options = new Options();
            final UserCredentials userCredentials = UserCredentials.newBuilder()
                    .setUsername(System.getProperty("user.name")).addGroups("root").build();
            final SSLOptions sslOptions = url.getProtocol().equals(Schemes.SCHEME_PBRPC) ? null : new SSLOptions(
                    CERT_DIR + "Client.p12", "passphrase", SSLOptions.PKCS12_CONTAINER, CERT_DIR + "trusted.jks",
                    "passphrase", SSLOptions.JKS_CONTAINER, false, false, null, null);

            // Alternatively, specify own certificate files for debugging:
            // final SSLOptions sslOptions = new SSLOptions(
            // new FileInputStream(
            // "/home/mberlin/ZIB/XtreemFS/tasks archive/2013-08-05 Debug SSL issues/xtfsclient/config/xtfs-vm-certs/TestUser01.p12"),
            // "test123",
            // SSLOptions.PKCS12_CONTAINER,
            // new FileInputStream(
            // "/home/mberlin/ZIB/XtreemFS/tasks archive/2013-08-05 Debug SSL issues/xtfsclient/config/xtfs-vm-certs/trusted.jks"),
            // "J9AUcbrdVkFg75kcqumz", SSLOptions.JKS_CONTAINER, false, false, null);

            Logging.start(Logging.LEVEL_WARN);
            client = ClientFactory.createClient(url.getHost() + ":" + url.getPort(), userCredentials, sslOptions,
                    options);
            client.start();

            Volume volume = client.openVolume(volumeName, sslOptions, options);

            // Open a file.
            fileHandle = volume.openFile(
                    userCredentials,
                    "/example_libxtreemfs_test_" + String.format("%03d", (int) (Math.random() * 1000)) + ".bin",
                    SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                            | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_EXCL.getNumber()
                            | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber(), 0644);

            // Init chunk to be written.
            byte[] data = new byte[1 << 17]; // 128 kB chunk.
            Arrays.fill(data, (byte) 0xAB);

            // Write 1 MB to file.
            for (int offset = 0; offset < (1 << 20); offset += data.length) {
                fileHandle.write(userCredentials, data, data.length, offset);
            }

            // Read 1 MB from file.
            byte[] readData = new byte[data.length];
            for (int offset = 0; offset < (1 << 20); offset += data.length) {
                int readCount = fileHandle.read(userCredentials, readData, data.length, offset);
                if (readCount != data.length) {
                    throw new IOException("Read less data than expected: " + readCount + " bytes instead of: "
                            + data.length);
                }
                if (!Arrays.equals(readData, data)) {
                    throw new IOException("Read data differs from written chunk at offset: " + offset);
                }
            }
        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage() + "\n Full Stacktrace:\n" + e.getStackTrace());
            return;
        } finally {
            if (fileHandle != null) {
                try {
                    fileHandle.close();
                } catch (IOException e) {
                    System.err.println("Failed to close() the file: " + e.getMessage() + "\n Full Stacktrace:\n"
                            + e.getStackTrace());
                    return;
                }
            }
            if (client != null) {
                client.shutdown();
            }
            System.out.println("If no errors are shown, the example was successfully executed.");
        }

        return;
    }

}
