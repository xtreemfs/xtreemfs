package org.xtreemfs.sandbox.benchmark;

import static org.xtreemfs.foundation.logging.Logging.Category;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.xtreemfs.foundation.logging.Logging;

public class xtfs_benchmark {
    static final int          KiB_IN_BYTES = 1024;
    static final int          MiB_IN_BYTES = 1024 * 1024;
    static final int          GiB_IN_BYTES = 1024 * 1024 * 1024;

    private static Controller controller;
    private static CLIOptions cliOptions;

    static {
        cliOptions = new CLIOptions();
    }

    public static void main(String[] args) {

        Logging.start(6, Category.tool);
        Logging.redirect(System.err);

        cliOptions.parseCLIOptions(args);

        if (cliOptions.usageIsSet()) {
            cliOptions.displayUsage();
            return;
        }

        Params params = null;
        try {
            params = cliOptions.buildParamsFromCLIOptions();
            controller = new Controller(params);
            controller.tryConnection();
            setupVolumes(params);
            runBenchmarks(params);
            controller.teardown();
        } catch (Exception e) {
            // Todo jvf add logging
            e.printStackTrace();
            /* force exit of benchmark tool in case of uncaught exceptions */
            System.exit(1);
        }
    }

    private static void setupVolumes(Params params) throws Exception {
        VolumeManager.init(params);
        VolumeManager volumeManager = VolumeManager.getInstance();

        if (cliOptions.arguments.size() == 0)
            volumeManager.createDefaultVolumes(params.numberOfThreads);
        else {
            String[] volumes = cliOptions.arguments.toArray(new String[cliOptions.arguments.size()]);
            volumeManager.openVolumes(volumes);
        }
    }

    private static void runBenchmarks(Params params) throws Exception {

        ConcurrentLinkedQueue<BenchmarkResult> result;
        ConcurrentLinkedQueue<BenchmarkResult> results = new ConcurrentLinkedQueue<BenchmarkResult>();

        if (cliOptions.sequentialWriteBenchmarkIsSet()) {
            result = controller.startBenchmarks(AbstractBenchmark.BenchmarkType.SEQ_WRITE, params.numberOfThreads);
            results.addAll(result);
        }

        if (cliOptions.sequentialReadBenchmarkIsSet()) {
            result = controller.startBenchmarks(AbstractBenchmark.BenchmarkType.SEQ_READ, params.numberOfThreads);
            results.addAll(result);
        }

        if (cliOptions.randomWriteBenchmarkIsSet()) {
            result = controller.startBenchmarks(AbstractBenchmark.BenchmarkType.RAND_WRITE, params.numberOfThreads);
            results.addAll(result);
        }

        if (cliOptions.randomReadBenchmarkIsSet()) {
            result = controller.startBenchmarks(AbstractBenchmark.BenchmarkType.RAND_READ, params.numberOfThreads);
            results.addAll(result);
        }

        if (cliOptions.randomFilebasedWriteBenchmarkIsSet()) {
            result = controller.startBenchmarks(AbstractBenchmark.BenchmarkType.FILES_WRITE, 1);
            results.addAll(result);
        }

        if (cliOptions.randomFilebasedReadBenchmarkIsSet()) {
            result = controller.startBenchmarks(AbstractBenchmark.BenchmarkType.FILES_READ, 1);
            results.addAll(result);
        }

        Controller.printResults(results);
        Controller.printCSV(results);
    }

}
