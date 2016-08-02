package org.xtreemfs.common.benchmark;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Random;

import org.xtreemfs.common.libxtreemfs.AdminClient;
import org.xtreemfs.common.libxtreemfs.FileHandle;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes;

public class UnalignedSequentialWriteBenchmark extends SequentialBenchmark {

    private final LinkedList<String> filenames;

    UnalignedSequentialWriteBenchmark(long size, BenchmarkConfig config, AdminClient client, VolumeManager volumeManager)
            throws Exception {
        super(size, config, client, volumeManager);
        filenames = new LinkedList<String>();
    }

    @Override
    long performIO(byte[] data, long numberOfBlocks) throws IOException {
        int flags = GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                | GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber()
                | GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber();
        FileHandle fileHandle = volume.openFile(config.getUserCredentials(), BENCHMARK_FILENAME + 0, flags, 0777);

        try {
            return tryPerformIO(data, numberOfBlocks, fileHandle);
        } catch (IOException e) {
            /*
             * closing the filehandle manually seems to be the only way to avoid an AssertionError in
             * VolumeImplementation.internalShutdown() when shutting down client
             */
            fileHandle.close();
            throw e;
        }
    }

    private long tryPerformIO(byte[] data, long numberOfBlocks, FileHandle fileHandle) throws IOException {
        Random random = new Random();
        this.filenames.add(BENCHMARK_FILENAME + 0);
        long byteCounter = 0;

        for (long j = 0; !cancelled && j < numberOfBlocks; j++) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "Writing block %d of %d ...", j + 1, numberOfBlocks);
            }
            
            long stripesPerRequest = (long) Math.ceil((double) requestSize / (double) config.getStripeSizeInBytes());
            long nextOffset = j * stripesPerRequest * config.getStripeSizeInBytes();
            random.nextBytes(data);
            byteCounter += fileHandle.write(config.getUserCredentials(), data, requestSize, nextOffset);
        }
        fileHandle.close();
        return byteCounter;
    }

    @Override
    void finalizeBenchmark() throws Exception {
        volumeManager.setSequentialFilelistForVolume(volume, filenames);
        volumeManager.addCreatedFiles(volume, filenames);
    }

}
