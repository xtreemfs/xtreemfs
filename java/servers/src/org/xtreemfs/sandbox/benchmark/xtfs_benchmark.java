package org.xtreemfs.sandbox.benchmark;

import static org.xtreemfs.foundation.logging.Logging.*;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.utils.utils;

public class xtfs_benchmark {
    static final int                      KiB_IN_BYTES = 1024;
    static final int                      MiB_IN_BYTES = 1024 * 1024;
    static final int                      GiB_IN_BYTES = 1024 * 1024 * 1024;

    private static Controller             controller;

    public static void main(String[] args) throws Exception {

        Logging.start(6, Category.tool);
        Logging.redirect(System.err);


        CLIOptions.parseCLIOptions(args);
        if (CLIOptions.usageIsSet())  {
            CLIOptions.displayUsage();
            return;
        }

        Params params = CLIOptions.buildParams();

        // Todo (jvf) delete
        //System.err.println(params);

        controller = new Controller(params);
        controller.tryConnection();
        setupVolumes(params);
        runBenchmarks(params);
        controller.teardown();

    }

    private static void setupVolumes(Params params) throws Exception {
        VolumeManager.init(params);
        VolumeManager volumeManager = VolumeManager.getInstance();

        if (CLIOptions.arguments.size() == 0)
            volumeManager.createDefaultVolumes(params.numberOfThreads);
        else {
            // ToDo add check to verify that the number of volumes is in accordance with the number of threads
            String[] volumes = CLIOptions.arguments.toArray(new String[CLIOptions.arguments.size()]);
            volumeManager.openVolumes(volumes);
        }
    }

    private static void runBenchmarks(Params params) throws Exception {

        ConcurrentLinkedQueue<BenchmarkResult> result;
        ConcurrentLinkedQueue<BenchmarkResult> results = new ConcurrentLinkedQueue<BenchmarkResult>();

        if (CLIOptions.sequentialWriteBenchmarkIsSet()) {
            result = controller.startBenchmarks(BenchmarkType.WRITE, params.numberOfThreads);
            results.addAll(result);
        }

        if (CLIOptions.sequentialReadBenchmarkIsSet()) {
            result = controller.startBenchmarks(BenchmarkType.READ, params.numberOfThreads);
            results.addAll(result);
        }

		if (CLIOptions.randomWriteBenchmarkIsSet()) {
			result = controller.startBenchmarks(BenchmarkType.RANDOM_IO_WRITE, params.numberOfThreads);
			results.addAll(result);
		}

        if (CLIOptions.randomReadBenchmarkIsSet()) {
            result = controller.startBenchmarks(BenchmarkType.RANDOM_IO_READ, params.numberOfThreads);
            results.addAll(result);
        }

        if (CLIOptions.randomFilebasedWriteBenchmarkIsSet()) {
            result = controller.startBenchmarks(BenchmarkType.RANDOM_IO_WRITE_FILEBASED, 1);
            results.addAll(result);
        }

        if (CLIOptions.randomFilebasedReadBenchmarkIsSet()) {
            result = controller.startBenchmarks(BenchmarkType.RANDOM_IO_READ_FILEBASED, 1);
            results.addAll(result);
        }

        Controller.printResults(results);
        Controller.printCSV(results);
    }

}
