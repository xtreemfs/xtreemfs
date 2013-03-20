package org.xtreemfs.sandbox.benchmarkOSDPerformance;

import static org.xtreemfs.foundation.logging.Logging.*;
import static org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import static org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import static org.xtreemfs.foundation.util.CLIParser.CliOption.OPTIONTYPE.STRING;
import static org.xtreemfs.foundation.util.CLIParser.CliOption.OPTIONTYPE.SWITCH;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.xtreemfs.common.libxtreemfs.AdminClient;
import org.xtreemfs.common.libxtreemfs.ClientImplementation;
import org.xtreemfs.common.libxtreemfs.Options;
import org.xtreemfs.common.libxtreemfs.UUIDResolver;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.util.CLIParser;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR;
import org.xtreemfs.utils.DefaultDirConfig;
import org.xtreemfs.utils.utils;

public class Params {
    static final int                 KiB_IN_BYTES           = 1024;
    static final int                 MiB_IN_BYTES           = 1024 * 1024;
    static final int                 GiB_IN_BYTES           = 1024 * 1024 * 1024;
    static int                       numberOfThreads;
    static long                      sequentialSizeInBytes;
    static long                      randomSizeInBytes;
    static long                      basefileSizeInBytes;
    static long                      fileSizeInBytes;
    static int                       numberOfThreadsDefault = 1;
    static long                      sequentialSizeInBytesDefault;
    static long                      randomSizeInBytesDefault;
    static long                      basefileSizeInBytesDefault;
    static long                      fileSizeInBytesDefault;
    static String                    userNameDefault        = "testUser";
    Map<String, CLIParser.CliOption> options;
    List<String>                     arguments;

    public Params() {
        this.sequentialSizeInBytesDefault = 10L * (long) MiB_IN_BYTES;
        this.randomSizeInBytesDefault = 10L * (long) MiB_IN_BYTES;
        this.basefileSizeInBytesDefault = 3L * (long) GiB_IN_BYTES;
        this.fileSizeInBytesDefault = 4L * (long) KiB_IN_BYTES;
    }

    public void parseCLIOptions(String[] args) {
        initOptions();
        this.arguments = new ArrayList<String>(5);
        CLIParser.parseCLI(args, options, arguments);
    }

    public ConnectionData getConnectionData() {
        return new ConnectionData(parseUsername(), parseGroup(), parseOSDPassword(), parseDirAddress(),
                parseMRCAddress(), parseUserCredentials(), parseAuth(), parseSSLOptions(), parseOptions());
    }

    public List<String> getArguments() {
        return this.arguments;
    }

    /**
     * Prints out usage informations and terminates the application.
     */
    public void usage() {

        System.out.println("\nusage: xtfs_benchmark [options] volume1 volume2 ... \n");
        System.out
                .println("The number of volumes must be in accordance with the number of benchmarks run in parallel (see -p).");
        System.out
                .println("All sizes can be modified with multiplication modifiers, where K means KiB, M means MiB and G means GiB. \nIf no modifier is given, sizes are assumed to be in bytes.");
        System.out.println();
        System.out.println("  " + "options:");
        utils.printOptions(options);
        System.out.println();
        System.out.println("example: xtfs_benchmark -sw -sr -p 3 -ssize 3G volume1 volume2 volume3");
        System.out
                .println("\t\t starts a sequential write and read benchmark of 3 GiB with 3 benchmarks in parallel on volume1, volume2 and volume3\n");
    }

    public boolean sequentialWriteBenchmarkIsSet() {
        return options.get("sw").switchValue;
    }

    public boolean sequentialReadBenchmarkIsSet() {
        return options.get("sr").switchValue;
    }

    public boolean randomReadBenchmarkIsSet() {
        return options.get("rr").switchValue;
    }

    public boolean randomFilebasedWriteBenchmarkIsSet() {
        return options.get("rfw").switchValue;
    }

    public boolean randomFilebasedReadBenchmarkIsSet() {
        return options.get("rfr").switchValue;
    }

    public boolean usageIsSet() {
        return (options.get(utils.OPTION_HELP).switchValue || options.get(utils.OPTION_HELP_LONG).switchValue);
    }

    private void initOptions() {

        /* -sw -sr -rw -p -rr -rfr -rfw -ssize -rsize -file-size -basefile-size -no-cleanup volume1 volume2 */

        Map<String, CLIParser.CliOption> options = utils.getDefaultAdminToolOptions(true);
        List<String> arguments = new ArrayList<String>(1);

        /* Connection Data */
        options.put("-dir-address", new CLIParser.CliOption(STRING,
                "directory service to use (e.g. 'localhost:32638'). If no URI is specified, URI and security settings are taken from '"
                        + DefaultDirConfig.DEFAULT_DIR_CONFIG + "'", "<uri>"));
        options.put("-username", new CLIParser.CliOption(STRING, "username to use", "<username>"));

        /* benchmark switches */
        options.put("sw", new CLIParser.CliOption(SWITCH, "sequential write benchmark", ""));
        options.put("sr", new CLIParser.CliOption(SWITCH, "sequential read benchmark", ""));
        options.put("rw", new CLIParser.CliOption(SWITCH, "random write benchmark", ""));
        options.put("rr", new CLIParser.CliOption(SWITCH, "random read benchmark", ""));
        options.put("rfw", new CLIParser.CliOption(SWITCH, "random filebased write benchmark", ""));
        options.put("rfr", new CLIParser.CliOption(SWITCH, "random filebased read benchmark", ""));

        options.put("p", new CLIParser.CliOption(STRING, "number of benchmarks to be started in parallel. default: 1",
                "<number>"));

        /* sizes */
        options.put("ssize", new CLIParser.CliOption(STRING,
                "size for sequential benchmarks in [B|K|M|G] (no modifier assumes bytes)", "<size>"));
        options.put("rsize", new CLIParser.CliOption(STRING,
                "size for random benchmarks in [B|K|M|G] (no modifier assumes bytes)", "<size>"));
        options.put("-basefile-size", new CLIParser.CliOption(STRING,
                "size of the basefile for random benchmarks in [B|K|M|G] (no modifier assumes bytes)", "<size>"));
        options.put("-file-size", new CLIParser.CliOption(STRING,
                "size of the files for random filebased benchmarks in [B|K|M|G] (no modifier assumes bytes)", "<size>"));

        /* deletion options */
        options.put("-no-cleanup", new CLIParser.CliOption(SWITCH, "do not delete created volumes and files", ""));
        options.put("-no-cleanup-volumes", new CLIParser.CliOption(SWITCH, "do not delete created volumes", ""));
        options.put("-no-cleanup-files", new CLIParser.CliOption(SWITCH, "do not delete created files", ""));

        this.options = options;
    }

    private long parseSizeToBytes(String size) {
        size = size.toUpperCase();
        long sizeInBytes;

        if (!size.matches("[0-9]+[BKMG]?"))
            throw new IllegalArgumentException("Wrong format for size.");

        if (size.matches("[0-9]+")) {
            sizeInBytes = Long.valueOf(size);
        } else {
            char suffix = size.charAt(size.length() - 1);
            long numbers = Long.valueOf(size.substring(0, size.length() - 1));
            switch (suffix) {
            case 'B':
                sizeInBytes = numbers;
                break;
            case 'K':
                sizeInBytes = numbers * (long) KiB_IN_BYTES;
                break;
            case 'M':
                sizeInBytes = numbers * (long) MiB_IN_BYTES;
                break;
            case 'G':
                sizeInBytes = numbers * (long) GiB_IN_BYTES;
                break;
            default:
                sizeInBytes = 0L;
                break;
            }
        }
        return sizeInBytes;
    }

    String parseMRCAddress() {

        AdminClient client = BenchmarkClientFactory.getNewClient(parseDirAddress(), parseUserCredentials(),
                parseSSLOptions(), parseOptions());

        UUIDResolver resolver = (ClientImplementation) client;

        String mrcUUID = null;
        String mrcAddress = null;
        try {
            mrcUUID = client.getServiceByType(DIR.ServiceType.SERVICE_TYPE_MRC).getServices(0).getUuid();
            mrcAddress = resolver.uuidToAddress(mrcUUID);
        } catch (Exception e) {
            Logging.logMessage(Logging.LEVEL_ERROR, Logging.Category.tool, this,
                    "Error while trying to get the MRC Address. Errormessage: %s", e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        return mrcAddress;
    }

    public int getNumberOfThreads() {
        try {
            /* Integer.valueOf throw NumberFormatException when called on null */
            return Integer.valueOf(options.get("p").stringValue);
        } catch (NumberFormatException e) {
            return numberOfThreadsDefault;
        }
    }

    public long getSequentialSizeInBytes() {
        return tryToGetSize("ssize");
    }

    public long getRandomSizeInBytes() {
        return tryToGetSize("rsize");
    }

    public long getBasefileSizeInBytes() {
        return tryToGetSize("-basefile-size");
    }

    public long getFileSizeInBytes() {
        return tryToGetSize("-file-size");
    }

    public long tryToGetSize(String optionName) {
        String sizeWithModifier = options.get(optionName).stringValue;
        long size = getDefaultSize(optionName);
        if (null != sizeWithModifier)
            size = parseSizeToBytes(options.get(optionName).stringValue);
        return size;
    }

    public long getDefaultSize(String option) {
        if ("ssize" == option)
            return sequentialSizeInBytesDefault;
        else if ("rsize" == option)
            return randomSizeInBytesDefault;
        else if ("-basefile-size" == option)
            return basefileSizeInBytesDefault;
        else if ("-file-size" == option)
            return fileSizeInBytesDefault;
        else
            throw new IllegalArgumentException("getDefaultSize: No known option");
    }

    private String parseDirAddress() {
        String dirAddress = options.get("-dir-address").stringValue;
        if (null == dirAddress) {
            dirAddress = getDefaultDir();
        }
        return dirAddress;
    }

    private String getDefaultDir() {
        String[] dirAddresses;
        DefaultDirConfig cfg = null;
        try {
            cfg = new DefaultDirConfig();
        } catch (IOException e) {
            logMessage(LEVEL_ERROR, Category.tool, Params.class,
                    "Could not read Default DIR Config in %s. Errormessage: %s", DefaultDirConfig.DEFAULT_DIR_CONFIG,
                    e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        dirAddresses = cfg.getDirectoryServices();
        return dirAddresses[0];
    }

    private String parseUsername() {
        String userName = options.get("-username").stringValue;
        if (null == userName) {
            userName = userNameDefault;
        }
        return userName;
    }

    private String parseGroup() {
        // Todo Replace with real parseGroup()
        return parseUsername();
    }

    private String parseOSDPassword() {
        String osdPassword = options.get(utils.OPTION_ADMIN_PASS).stringValue;
        if (null == osdPassword)
            osdPassword = "";
        return osdPassword;
    }

    private Auth parseAuth() {
        return RPCAuthentication.authNone;
    }

    private SSLOptions parseSSLOptions() {
        // Todo Implement SSL Options
        return null;
    }

    private Options parseOptions() {
        return new Options();
    }

    private UserCredentials parseUserCredentials() {
        return UserCredentials.newBuilder().setUsername(parseUsername()).addGroups(parseUsername()).build();
    }

}
