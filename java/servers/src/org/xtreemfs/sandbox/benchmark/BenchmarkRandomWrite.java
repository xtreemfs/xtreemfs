/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox.benchmark;

import org.xtreemfs.common.libxtreemfs.FileHandle;
import org.xtreemfs.common.libxtreemfs.Volume;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes;

import java.io.IOException;

/**
 * Class implementing a random read benchmark.
 * 
 * @author jensvfischer
 */
public class BenchmarkRandomWrite extends BenchmarkRandomOffsetbased {

    BenchmarkRandomWrite(Volume volume, Params params) throws Exception {
        super(volume, params);
    }

    /* Called within the benchmark method. Performs the actual reading of data from the volume. */
    @Override
    long performIO(byte[] data, long numberOfBlocks) throws IOException {

        numberOfBlocks = convertTo4KiBBlocks(numberOfBlocks);
        long byteCounter = 0;

		int flags = GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
				| GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber()
				| GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber();

        for (long j = 0; j < numberOfBlocks-1; j++) {
            FileHandle fileHandle = volume.openFile(params.userCredentials, BASFILE_FILENAME,
                    flags, 511);
            long nextOffset = generateNextRandomOffset();
            byteCounter += fileHandle.write(params.userCredentials, data, RANDOM_IO_BLOCKSIZE, nextOffset);
            fileHandle.close();
        }

		/* last block written needs to be the last block of the basefile to not violate the basefile filesize */
		FileHandle fileHandle = volume.openFile(params.userCredentials, BASFILE_FILENAME,
				flags, 511);
		byteCounter += fileHandle.write(params.userCredentials, data, RANDOM_IO_BLOCKSIZE, params.basefileSizeInBytes-RANDOM_IO_BLOCKSIZE);
		fileHandle.close();
        return byteCounter;
    }


}
