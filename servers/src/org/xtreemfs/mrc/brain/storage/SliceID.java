/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

    This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
    Grid Operating System, see <http://www.xtreemos.eu> for more details.
    The XtreemOS project has been developed with the financial support of the
    European Commission's IST program under contract #FP6-033576.

    XtreemFS is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 2 of the License, or (at your option)
    any later version.

    XtreemFS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
*/
/*
 * AUTHORS: Jan Stender (ZIB), Björn Kolbeck (ZIB)
 */

package org.xtreemfs.mrc.brain.storage;

import java.io.IOException;
import java.io.Serializable;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import org.xtreemfs.common.buffer.ReusableBuffer;

/**
 * 
 * @author bjko
 */
public class SliceID implements Serializable {

    public static final int SIZE_IN_BYTES = 16;

    private static final int VOLID_SIZE_BYTES = 12;

    private static final int SLICENO_SIZE_BYTES = Integer.SIZE / 8;

    private final String volumeID;
    
    private final byte[] volID;

    private int sliceNo;

    private String strRepres;

    /** Creates a new instance of SliceID from a String */
    public SliceID(String in) {
        strRepres = in;
        volID = new byte[VOLID_SIZE_BYTES];
        for (int i = 0; i < VOLID_SIZE_BYTES; i++) {
            volID[i] = (byte) Integer.parseInt(in.substring(i * 2, i * 2 + 2),
                    16);
        }
        sliceNo = Integer.parseInt(in.substring(24), 16);
        
        volumeID = strRepres.substring(0, VOLID_SIZE_BYTES * 2);
    }
    
    public SliceID(String volumeId, int sliceNo) {

        volID = new byte[VOLID_SIZE_BYTES];
        for (int i = 0; i < VOLID_SIZE_BYTES; i++) {
            volID[i] = (byte) Integer.parseInt(volumeId.substring(i * 2, i * 2 + 2),
                    16);
        }
        this.volumeID = volumeId;
        this.sliceNo = sliceNo;
        
        strRepres = volumeId + VolIDGen.intToHex(sliceNo);
    }

    /**
     * Generates a new SliceID
     */
    public SliceID(int sliceNo) throws SocketException {
        volID = VolIDGen.getGenerator().getNewVolIDBytes();
        this.sliceNo = sliceNo;
        strRepres = "";
        for (int i = 0; i < volID.length; i++) {
            strRepres += VolIDGen.byteToHex(volID[i] & 0xFF);
        }
        volumeID = strRepres;
        
        strRepres += VolIDGen.intToHex(sliceNo);
    }

    /**
     * Generates a new SliceID for an existing volume
     */
    public SliceID(SliceID volumeID, int sliceNo) throws SocketException {
        volID = volumeID.volID;
        this.sliceNo = sliceNo;
        strRepres = "";
        for (int i = 0; i < volID.length; i++) {
            strRepres += VolIDGen.byteToHex(volID[i] & 0xFF);
        }
        this.volumeID = strRepres;
        
        strRepres += VolIDGen.intToHex(sliceNo);
    }

    /**
     * reads a sliceID from a byteBuffer
     */
    public SliceID(ReusableBuffer buff) {
        volID = new byte[VOLID_SIZE_BYTES];
        buff.get(volID);
        sliceNo = buff.getInt();

        strRepres = "";
        for (int i = 0; i < volID.length; i++) {
            strRepres += VolIDGen.byteToHex(volID[i] & 0xFF);
        }
        this.volumeID = strRepres;
        
        strRepres += VolIDGen.intToHex(sliceNo);
    }

    /**
     * reads a sliceID from a FileChannel
     */
    public SliceID(FileChannel fc) throws IOException {
        ByteBuffer buff = ByteBuffer.allocateDirect(SIZE_IN_BYTES);
        fc.read(buff);
        buff.position(0);
        volID = new byte[VOLID_SIZE_BYTES];
        buff.get(volID);
        sliceNo = buff.getInt();

        strRepres = "";
        for (int i = 0; i < volID.length; i++) {
            strRepres += VolIDGen.byteToHex(volID[i] & 0xFF);
        }
        this.volumeID = strRepres;
        
        strRepres += VolIDGen.intToHex(sliceNo);
    }

    /**
     * Returns the volume ID part of the slice ID.
     */
    public String getVolumeId() {
        return volumeID;
    }

    public String toString() {
        return strRepres;
    }

    public boolean equals(Object obj) {
        try {
            SliceID other = (SliceID) obj;
            if (sliceNo != other.sliceNo)
                return false;
            for (int i = 0; i < volID.length; i++) {
                if (volID[i] != other.volID[i])
                    return false;
            }
            return true;
        } catch (ClassCastException ex) {
            return false;
        }
    }

    public void write(ReusableBuffer buff) {
        buff.put(volID);
        buff.putInt(sliceNo);
    }

    public void write(FileChannel fc) throws IOException {
        ByteBuffer buff = ByteBuffer.allocateDirect(SIZE_IN_BYTES);
        buff.put(volID);
        buff.putInt(sliceNo);
        buff.position(0);
        fc.write(buff);
    }

    public int hashCode() {
        if (this.strRepres == null)
            throw new RuntimeException("no string representation of sliceID");
        return this.strRepres.hashCode();
    }

}
