/*
 * Copyright (c) 2008 by Nele Andersen, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.clients.io;

import java.io.IOException;

import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;

class ByteMapperRAID0 implements ByteMapper {

    final int stripeSize;

    ObjectStore objectStore;

    public ByteMapperRAID0(int stripeSize, ObjectStore objectStore){
        this.stripeSize = stripeSize;
        this.objectStore = objectStore;
    }

    /**
     *
     * @param resultBuffer - the buffer into which the data is read.
     * @param offset - the start offset of the data.
     * @param bytesToRead - the maximum number of bytes read.
     * @return the total number of bytes read into the buffer, or -1 if
     * there is no more data because the end of the file has been reached.
     * @throws Exception
     * @throws IOException
     */
    public int read(byte[] data, int offset, int length, long filePosition) throws Exception{

        if (data.length < offset+length)
            throw new RuntimeException("buffer is too small!");

        final int firstObject = (int) (filePosition / this.stripeSize);
        assert(firstObject >= 0);

        int lastObject = (int) ( (filePosition + ((long)length)) / this.stripeSize);
        if (( (filePosition + ((long)length)) % this.stripeSize) == 0)
            lastObject--;
        assert(lastObject >= firstObject);

        final int offsetInFirstObject = (int) (filePosition % this.stripeSize);
        assert(offsetInFirstObject < stripeSize);
        final int bytesInLastObject = (int) (((filePosition + length) % this.stripeSize) == 0 ? this.stripeSize :
                    ((filePosition + length) % this.stripeSize));
        assert(bytesInLastObject > 0);
        assert(bytesInLastObject <= stripeSize);

        int bytesRead = 0;
        for (int obj = firstObject; obj <= lastObject; obj++) {

            int bytesToRead = this.stripeSize;
            int objOffset = 0;

            if (obj == firstObject) {
                objOffset = offsetInFirstObject;
                bytesToRead = this.stripeSize - objOffset;
            }
            if (obj == lastObject) {
                if (firstObject == lastObject) {
                    bytesToRead = bytesInLastObject-objOffset;
                } else {
                    bytesToRead = bytesInLastObject;
                }
            }

            assert(bytesToRead > 0);
            assert(objOffset >= 0);
            assert(objOffset < stripeSize);
            assert(objOffset+bytesToRead <= stripeSize);

            //System.out.println("read   "+obj+"   objOffset="+objOffset+" length="+bytesToRead);

            ReusableBuffer rb = objectStore.readObject(obj, objOffset, bytesToRead);
            assert(offset+bytesRead <= data.length);
            if (rb == null) {
                //EOF!
                break;
            }
            if (rb.capacity() < bytesToRead) {
                //EOF!
                final int dataToRead = Math.min(rb.capacity(),data.length-offset-bytesRead);
                rb.get(data, offset+bytesRead,dataToRead);
                bytesRead += rb.capacity();
                BufferPool.free(rb);
                break;
            }
            //can get less data then requested!
            rb.get(data, offset+bytesRead, rb.remaining());
            
            bytesRead += rb.capacity();
            BufferPool.free(rb);
        }
        return bytesRead;

    }

    public int write(byte[] data, int offset, int length, long filePosition) throws Exception{
        
        final int firstObject = (int) (filePosition / this.stripeSize);
        int lastObject = (int) ( (filePosition + ((long)length)) / this.stripeSize);
        if (( (filePosition + ((long)length)) % this.stripeSize) == 0)
            lastObject--;

        final int offsetInFirstObject = (int) (filePosition % this.stripeSize);


        int bytesInLastObject = -1;
        if (firstObject == lastObject) {
            bytesInLastObject = length;
        } else {
            if (((filePosition + length) % this.stripeSize) == 0) {
                bytesInLastObject = this.stripeSize;
                assert(bytesInLastObject >= 0);
            } else {
                bytesInLastObject = (int)((filePosition + length) % this.stripeSize);
                assert(bytesInLastObject >= 0);
            }
        }
        

        int bytesWritten = 0;
        for (int obj = firstObject; obj <= lastObject; obj++) {

            int bytesToWrite = this.stripeSize;
            int objOffset = 0;

            if (obj == firstObject) {
                bytesToWrite = this.stripeSize-offsetInFirstObject;
                objOffset = offsetInFirstObject;
            }
            if (obj == lastObject)
                bytesToWrite = bytesInLastObject;

            
            ReusableBuffer view = ReusableBuffer.wrap(data, offset+bytesWritten, bytesToWrite);
            objectStore.writeObject(objOffset, obj, view);
            bytesWritten += bytesToWrite;
        }
        return bytesWritten;

    }

}