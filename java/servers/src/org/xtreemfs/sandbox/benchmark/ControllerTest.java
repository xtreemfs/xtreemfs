/*
 * Copyright (c) 2008-2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.sandbox.benchmark;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.xtreemfs.foundation.logging.Logging;

//import static org.xtreemfs.sandbox.benchmark.Controller.parseSizeToBytes;

public class ControllerTest {

    static final long KiB_IN_BYTES = 1024;
    static final long MiB_IN_BYTES = 1024 * 1024;
    static final long GiB_IN_BYTES = 1024 * 1024 * 1024;

    // private static DIRRequestDispatcher dir;

    @BeforeClass
    public static void setUp() throws Exception {
        Logging.start(Logging.LEVEL_INFO, Logging.Category.tool);
    }

    @AfterClass
    public static void tearDown() throws Exception {
    }

    // @Test
    // public void testParseSizeToBytes() throws Exception {
    // String[] sizes = {"1073741824", "1073741824B", "1024M" , "1G", "1073741824b", "1024m" , "1g"};
    // for (String size : sizes) {
    // long benchmarkSizeInBytes = Params.parseSizeToBytes(size);
    // assertEquals(1073741824L, Params.parseSizeToBytes(size));
    // }
    // }
    //
    // @Test (expected = IllegalArgumentException.class)
    // public void testParseSizeToBytesWithError() throws Exception {
    // String[] sizes = {"1073741824GG", "10737g41824B", "M1024" , "G", "m"};
    // for (String size : sizes)
    // Params.parseSizeToBytes(size);
    // }

    @Test
    @Ignore
    public void testMain() throws Exception {
        // Controller.main(new String[]{"-sw", "-sr"});

        // Controller.main(new String[]{"-sw", "-sr", "-rr", "-rfw", "-rfr", "-ssize", "1m", "-rsize", "1M",
        // "basefile-size", "1g"});
        Controller.main(new String[] { "-sw", "-sr", "-rr", "-rfw", "-rfr" });
        assertTrue(true);

        // Controller.main(new String[]{"rfw", "rfr", "rsize 20"});
        // Controller.main(new String[]{"rfw", "rfr", "rsize 20"});
    }

    @Test
    @Ignore
    public void testHelp() throws Exception {
        // Controller.main(new String[]{"-h"});
    }

    @Test
    @Ignore
    public void fullFastBenchmarkSuite() throws Exception {

        Params params = new ParamsBuilder().setSequentialSizeInBytes(3L * MiB_IN_BYTES)
                .setRandomSizeInBytes(3L * MiB_IN_BYTES).build();

        Controller controller = new Controller(params);
        controller.setupVolumes();
        ConcurrentLinkedQueue results = controller.startBenchmarks(AbstractBenchmark.BenchmarkType.SEQ_WRITE, 1);
        Controller.printResults(results);
        results = controller.startBenchmarks(AbstractBenchmark.BenchmarkType.SEQ_READ, 1);
        Controller.printResults(results);
        // results = controller.startBenchmarks(BenchmarkType.RAND_READ, 1, MiB_IN_BYTES);
        // controller.printResults(results);
        results = controller.startBenchmarks(AbstractBenchmark.BenchmarkType.FILES_WRITE, 1);
        Controller.printResults(results);
        results = controller.startBenchmarks(AbstractBenchmark.BenchmarkType.FILES_WRITE, 1);
        Controller.printResults(results);
        controller.teardown();
    }

    @Test
    public void testCLIParserSmall() throws Exception {
        // xtfs_benchmark.main(new String[] { "-sw", "-sr", "-rr", "-rfw", "-rfr", "-ssize", "1m", "-rsize",
        // "1M",
        // "basefile-size", "1g" });
//        xtfs_benchmark.main(new String[] { "-sw", "-sr", "-ssize", "100M", "--stripeWidth", "1", "--stripeSize", "1M"});
        xtfs_benchmark.main(new String[] {"-sw", "-sr", "-ssize", "50M", "-r", "10"});
//        xtfs_benchmark.main(new String[] { "-rfr", "-rfw", "-rsize", "5M"});
    }

    @Test
    @Ignore
    public void testCLIParserFull() throws Exception {
        /*
         * -sw -sr -rw -p -rr -rfr -rfw -p <number> -ssize -rsize -file-size -basefile-size -no-cleanup
         * volume1 volume2
         */

//                                                         -sw -sr -rr -rfr -rfr -p 4 -ssize 100M -rsize 10M --stripeSize 1M volumeA volumeB volumeC volumeD

        xtfs_benchmark.main(new String[] {"-sw", "-sr", "-rr", "-rfw", "-rfr", "-p", "2", "-ssize", "10M", "-rsize", "1M",
                "--file-size", "4K", "--basefile-size", "3G", "--no-cleanup", "--no-cleanup-basefile", "volumeA",
                "volumeB" });
        // xtfs_benchmark.main(new String[]{"-sw", "-sr", "-p", "2", "-ssize", "100M", "volumeA", "volumeB"});
    }

    @Test
    @Ignore
    public void testXtfs_BenchmarkDeletionOptions() throws Exception {
        /*
         * -sw -sr -rw -p -rr -rfr -rfw -p <number> -ssize -rsize -file-size -basefile-size -no-cleanup
         * volume1 volume2
         */

        xtfs_benchmark.main(new String[] { "-rr", "-rsize", "2M", "--no-cleanup-volumes", "--basefile-size", "3G",
                "--no-cleanup-basefile", "volumeA" });
        // xtfs_benchmark.main(new String[]{"-sw", "-sr", "-p", "2", "-ssize", "100M", "volumeA", "volumeB"});
    }

    @Test
    @Ignore
    public void testCLIFilebasedReadOnly() throws Exception {
        xtfs_benchmark.main(new String[] {"-rfw", "-rfr", "-rsize", "100M", "--no-cleanup-volumes", "volumeA" });

    }

    @Test
    @Ignore
    public void testCLISeq() throws Exception {
        xtfs_benchmark.main(new String[] { "-sw", "-sr", "-p", "2", "-ssize", "100M", "volumeA", "volumeB" });

    }

    @Test
    @Ignore
    public void testCLIParserFilebased() throws Exception {
        xtfs_benchmark.main(new String[] { "-rfw", "-rfr" });
    }

    @Test
    @Ignore
    public void doScrub() throws Exception {
        xtfs_benchmark.main(new String[] { "-p", "2", "--no-cleanup", "volumeA", "volumeB" });
    }

}
