package org.xtreemfs.sandbox.benchmark;

import static org.xtreemfs.foundation.logging.Logging.Category;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.xtreemfs.foundation.logging.Logging;

/**
 * The commandline benchmark tool.
 */
public class xtfs_benchmark {

    private static Controller controller;
    private static CLIOptions cliOptions;

    static {
        cliOptions = new CLIOptions();
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandlerBenchmark());
    }

    /**
     * Main loop of the commandline tool.
     * 
     * @param args
     *            commandline options for the tool, as specified in {@link CLIOptions}
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        Logging.start(6, Category.tool);
        Logging.redirect(System.err);

        cliOptions.parseCLIOptions(args);

        if (cliOptions.usageIsSet()) {
            cliOptions.displayUsage();
            return;
        }

        Config config = cliOptions.buildParamsFromCLIOptions();
        controller = new Controller(config);
//        controller.tryConnection();
        controller.setupVolumes(cliOptions.arguments.toArray(new String[cliOptions.arguments.size()]));
        runBenchmarks(config);
        controller.teardown();
    }

    /* run all benchmarks specified by the CLIOptions */
    private static void runBenchmarks(Config config) throws Exception {

        ConcurrentLinkedQueue<BenchmarkResult> result;
        ConcurrentLinkedQueue<BenchmarkResult> results = new ConcurrentLinkedQueue<BenchmarkResult>();

        if (cliOptions.sequentialWriteBenchmarkIsSet()) {
            result = controller.startBenchmarks(BenchmarkType.SEQ_WRITE, config.numberOfThreads);
            results.addAll(result);
        }

        if (cliOptions.sequentialReadBenchmarkIsSet()) {
            result = controller.startBenchmarks(BenchmarkType.SEQ_READ, config.numberOfThreads);
            results.addAll(result);
        }

        if (cliOptions.randomWriteBenchmarkIsSet()) {
            result = controller.startBenchmarks(BenchmarkType.RAND_WRITE, config.numberOfThreads);
            results.addAll(result);
        }

        if (cliOptions.randomReadBenchmarkIsSet()) {
            result = controller.startBenchmarks(BenchmarkType.RAND_READ, config.numberOfThreads);
            results.addAll(result);
        }

        if (cliOptions.randomFilebasedWriteBenchmarkIsSet()) {
            result = controller.startBenchmarks(BenchmarkType.FILES_WRITE, 1);
            results.addAll(result);
        }

        if (cliOptions.randomFilebasedReadBenchmarkIsSet()) {
            result = controller.startBenchmarks(BenchmarkType.FILES_READ, 1);
            results.addAll(result);
        }

        printResults(results);
        printCSV(results);
    }

	/* Print the results as csv. */
	private static void printCSV(ConcurrentLinkedQueue<BenchmarkResult> results) {
		System.out.println("Type;NumberOfParallelThreads;TimeInSec;MiB/Sec;DataWrittenInBytes;ByteCount");
		/* print the results */
		for (BenchmarkResult res : results) {
			System.out.println(res.toCSV());
		}
	}

	/* Print the results in a json like style. */
	private static void printResults(ConcurrentLinkedQueue<BenchmarkResult> results) {
		/* print the results */
		for (BenchmarkResult res : results) {
			System.err.println(res);
		}
	}
}
