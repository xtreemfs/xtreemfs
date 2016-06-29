package org.xtreemfs.sandbox.tests;

import java.io.File;
import java.io.IOException;

public class MRCStressTest {

    private static int          fc     = 0;

    private static int          dc     = 0;

    private static final Object fcLock = new Object();

    private static final Object dcLock = new Object();

    public static void main(String[] args) throws Exception {

        final String rootDir = "/tmp/xtreemfs";
        final int numberOfThreads = 30;
        final int depth = 4;
        final int minSpread = 2;
        final int maxSpread = 5;
        final int minFilesPerDir = 0;
        final int maxFilesPerDir = 10;
        final int minNameLength = 1;
        final int maxNameLength = 32;

        long startTime = System.currentTimeMillis();

        Thread[] threads = new Thread[numberOfThreads];
        for (int i = 0; i < numberOfThreads; i++)
            threads[i] = new Thread() {
                public void run() {
                    try {
                        createRandomTree(rootDir, depth, minSpread, maxSpread,
                            minFilesPerDir, maxFilesPerDir, minNameLength,
                            maxNameLength);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };

        for (Thread th : threads)
            th.start();

        for (Thread th : threads)
            th.join();

        long time = System.currentTimeMillis() - startTime;
        System.out.println("created " + fc + " files and " + dc
            + " directories in " + time + " ms");
    }

    public static void createRandomTree(String rootDir, int depth,
        int minSpread, int maxSpread, int minFilesPerNode, int maxFilesPerNode,
        int minNameLength, int maxNameLength) throws IOException {

        int spread = randomNumber(minSpread, maxSpread);
        for (int i = 0; i < spread; i++) {

            // create the node
            String nestedDir = rootDir + "/"
                + randomFileName(minNameLength, maxNameLength);
            if (new File(nestedDir).mkdir())
                synchronized (dcLock) {
                    dc++;
                }
            else
                System.err.println("could not create directory " + nestedDir);

            // create nested files
            int fileCount = randomNumber(minFilesPerNode, maxFilesPerNode);
            for (int j = 0; j < fileCount; j++) {
                String fileName = nestedDir + "/"
                    + randomFileName(minNameLength, maxNameLength);

                if (new File(fileName).createNewFile())
                    synchronized (fcLock) {
                        fc++;
                    }
                else
                    System.err.println("could not create file " + nestedDir);

            }

            // create subtree
            if (depth > 1)
                createRandomTree(nestedDir, depth - 1, minSpread, maxSpread,
                    minFilesPerNode, maxFilesPerNode, minNameLength,
                    maxNameLength);
        }
    }

    private static int randomNumber(int lowerBound, int upperBound) {
        return (int) (Math.random() * (upperBound - lowerBound + 1) + lowerBound);
    }

    private static String randomFileName(int minLength, int maxLength) {

        final char[] allowedChars = { '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K',
            'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
            'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k',
            'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x',
            'y', 'z' };

        int length = randomNumber(minLength, maxLength);
        char[] chars = new char[length];
        for (int i = 0; i < chars.length; i++)
            chars[i] = allowedChars[(int) (Math.random() * allowedChars.length)];

        return new String(chars);
    }
}
